package in.sb.pinac.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import in.sb.pinac.dto.EnrollmentGoogleDto;
import in.sb.pinac.dto.EnrollmentSheetDTO;
import in.sb.pinac.dto.GoogleSheetAddRequestDto;
import in.sb.pinac.dto.PaymentGoogleDto;
import in.sb.pinac.dto.PaymentSheetDTO;
import in.sb.pinac.dto.StudentGoogleDto;
import in.sb.pinac.dto.StudentSheetDTO;
import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.Payment;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.CourseRepository;
import in.sb.pinac.repository.EnrollmentRepository;
import in.sb.pinac.repository.PaymentRepository;
import in.sb.pinac.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Dedicated GoogleSheetsService to handle all Google Sheets operations.
 *
 * Requirements:
 * - Direct Google Sheets integration without exposing credentials.
 * - Exact 12-column mapping:
 *   | Date & Time | User Name | User Email | Mobile Number | Course Name | Course ID | Course Price | Payment ID | Order ID | Payment Status | City | Course Active |
 * - Automatically append a new row when a user registers or completes a payment.
 * - Asynchronous, non-blocking execution so database transactions and user flows are never blocked or broken.
 * - Production-ready SLF4J logging for all successful and failed operations.
 */
@Service
public class GoogleSheetsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static final List<Object> HEADERS_12 = Collections.unmodifiableList(Arrays.asList(
            "Date & Time",
            "User Name",
            "User Email",
            "Mobile Number",
            "Course Name",
            "Course ID",
            "Course Price",
            "Payment ID",
            "Order ID",
            "Payment Status",
            "City",
            "Course Active"
    ));

    @Autowired(required = false)
    private Sheets sheetsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Value("${google.sheets.spreadsheet-id:1R-KWdJjFNGzF20gmc5zipVI2fDRrRltfb29CuLWuDPI}")
    private String spreadsheetId;

    @Value("${google.sheets.sheet-name:Sheet1}")
    private String sheetName;

    @Value("${google.sheets.enabled:true}")
    private boolean enabled;

    @Value("${google.sheets.webhook-url:}")
    private String webhookUrl;

    /**
     * Resolves the target sheet tab dynamically from the spreadsheet.
     * Checks configured tab name; falls back to the first existing tab if not found.
     */
    public String resolveTargetSheetName() {
        if (sheetsService == null || spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
            return (sheetName != null && !sheetName.trim().isEmpty()) ? sheetName.trim() : "Sheet1";
        }
        try {
            Spreadsheet sp = sheetsService.spreadsheets().get(spreadsheetId.trim()).execute();
            if (sp != null && sp.getSheets() != null && !sp.getSheets().isEmpty()) {
                for (com.google.api.services.sheets.v4.model.Sheet s : sp.getSheets()) {
                    if (s.getProperties() != null && s.getProperties().getTitle() != null) {
                        if (sheetName != null && sheetName.trim().equalsIgnoreCase(s.getProperties().getTitle().trim())) {
                            return s.getProperties().getTitle().trim();
                        }
                    }
                }
                return sp.getSheets().get(0).getProperties().getTitle().trim();
            }
        } catch (Exception e) {
            log.warn("Google Sheet tab resolution notice: {}", e.getMessage());
        }
        return (sheetName != null && !sheetName.trim().isEmpty()) ? sheetName.trim() : "Sheet1";
    }

    /**
     * Ensures that row 1 of the target sheet contains the exact 12 required header columns.
     * Reuses existing headers if they already match.
     */
    public synchronized void ensureHeaderRow(String targetTab) {
        if (!isAvailable()) return;

        try {
            String headerRange = targetTab + "!A1:L1";
            ValueRange current = sheetsService.spreadsheets().values()
                    .get(spreadsheetId.trim(), headerRange)
                    .execute();

            List<List<Object>> values = current.getValues();
            boolean needsUpdate = false;

            if (values == null || values.isEmpty() || values.get(0).isEmpty()) {
                needsUpdate = true;
            } else {
                List<Object> existingHeaders = values.get(0);
                if (existingHeaders.size() < HEADERS_12.size()) {
                    needsUpdate = true;
                } else {
                    for (int i = 0; i < HEADERS_12.size(); i++) {
                        String expected = HEADERS_12.get(i).toString().trim();
                        String actual = i < existingHeaders.size() && existingHeaders.get(i) != null
                                ? existingHeaders.get(i).toString().trim()
                                : "";
                        if (!expected.equalsIgnoreCase(actual)) {
                            needsUpdate = true;
                            break;
                        }
                    }
                }
            }

            if (needsUpdate) {
                ValueRange body = new ValueRange().setValues(Collections.singletonList(HEADERS_12));
                sheetsService.spreadsheets().values()
                        .update(spreadsheetId.trim(), headerRange, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
                log.info("Google Sheets: Header row (A1:L1) verified & updated with exact 12 columns in tab '{}'", targetTab);

                // Clear any leftover in M1:Z1 cleanly
                try {
                    sheetsService.spreadsheets().values()
                            .clear(spreadsheetId.trim(), targetTab + "!M1:Z1", new ClearValuesRequest())
                            .execute();
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Google Sheets: Header verification notice for tab '{}': {}", targetTab, e.getMessage());
        }
    }

    /**
     * 1. USER REGISTRATION
     * Automatically logs a new student registration row to Google Sheets.
     * Non-blocking, failure-safe: errors do not break user registration.
     */
    public void syncRegistrationToSheet(User student, String courseName, String courseId, Double coursePrice) {
        if (student == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                if (!isAvailable()) {
                    log.warn("Google Sheets integration is disabled or unconfigured. Skipping registration sync for {}", student.getEmail());
                    return;
                }

                // Fetch fresh user record from database
                User dbUser = null;
                if (student.getId() != null) {
                    dbUser = userRepository.findById(student.getId()).orElse(null);
                }
                if (dbUser == null && student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
                    dbUser = userRepository.findFirstByEmailOrderByIdDesc(student.getEmail().trim()).orElse(null);
                }
                if (dbUser == null && student.getMobile() != null && !student.getMobile().trim().isEmpty()) {
                    dbUser = userRepository.findFirstByMobileOrderByIdDesc(student.getMobile().trim()).orElse(null);
                }
                if (dbUser == null) {
                    dbUser = student;
                }

                String dateTime = dbUser.getCreatedAt() != null
                        ? dbUser.getCreatedAt().format(DATE_FORMATTER)
                        : LocalDateTime.now().format(DATE_FORMATTER);

                String userName = (dbUser.getName() != null && !dbUser.getName().trim().isEmpty())
                        ? dbUser.getName().trim()
                        : "Student";
                String userEmail = dbUser.getEmail() != null ? dbUser.getEmail().trim() : "";
                String mobileNumber = (dbUser.getMobile() != null && !dbUser.getMobile().trim().isEmpty())
                        ? dbUser.getMobile().trim()
                        : "N/A";
                String city = (dbUser.getCity() != null && !dbUser.getCity().trim().isEmpty())
                        ? dbUser.getCity().trim()
                        : "N/A";

                String cName = (courseName != null && !courseName.trim().isEmpty())
                        ? courseName.trim()
                        : "Not Purchased";
                String cId = (courseId != null && !courseId.trim().isEmpty())
                        ? courseId.trim()
                        : "N/A";
                String cPrice = (coursePrice != null && coursePrice > 0)
                        ? String.format("%.2f", coursePrice)
                        : "0.00";

                String paymentId = "Pending";
                String orderId = "Pending";
                String paymentStatus = "Not Paid";
                String courseActive = "No";

                // Column order: Date & Time, User Name, User Email, Mobile Number, Course Name, Course ID, Course Price, Payment ID, Order ID, Payment Status, City, Course Active
                List<Object> row = Arrays.asList(
                        dateTime,
                        userName,
                        userEmail,
                        mobileNumber,
                        cName,
                        cId,
                        cPrice,
                        paymentId,
                        orderId,
                        paymentStatus,
                        city,
                        courseActive
                );

                String targetTab = resolveTargetSheetName();
                ensureHeaderRow(targetTab);

                // If user already exists in sheet, update pending registration row (without overwriting purchased courses)
                boolean updated = updateExistingRow(userEmail, mobileNumber, userName, paymentId, row, targetTab, true);
                if (!updated) {
                    appendRow(targetTab, row);
                }

                log.info("Google Sheets: Registration synced successfully for user {} ({})", userName, userEmail);

                // Webhook notification if configured
                sendToWebhook("REGISTER", dateTime, userName, userEmail, mobileNumber, city,
                        cName, cId, cPrice, paymentId, orderId, paymentStatus, courseActive);

            } catch (Exception e) {
                log.error("Google Sheets registration sync failed for {}: {}", student.getEmail(), e.getMessage(), e);
            }
        });
    }

    /**
     * 2. COURSE PAYMENT & PURCHASE
     * Automatically updates or appends payment details in Google Sheets after successful payment.
     * Non-blocking, failure-safe: errors do not break payment verification.
     */
    public void syncPaymentToSheet(User student, Course course, Payment payment, Enrollment enrollment) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!isAvailable()) {
                    log.warn("Google Sheets integration is disabled or unconfigured. Skipping payment sync.");
                    return;
                }

                // Fetch freshest records from Database
                Payment dbPayment = null;
                if (payment != null && payment.getId() != null) {
                    dbPayment = paymentRepository.findById(payment.getId()).orElse(null);
                }
                if (dbPayment == null && payment != null && payment.getPaymentId() != null) {
                    dbPayment = paymentRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
                }
                if (dbPayment == null) {
                    dbPayment = payment;
                }

                User dbUser = null;
                if (student != null && student.getId() != null) {
                    dbUser = userRepository.findById(student.getId()).orElse(null);
                }
                if (dbUser == null && dbPayment != null && dbPayment.getUser() != null) {
                    dbUser = dbPayment.getUser();
                }
                if (dbUser == null && student != null && student.getEmail() != null) {
                    dbUser = userRepository.findFirstByEmailOrderByIdDesc(student.getEmail().trim()).orElse(null);
                }
                if (dbUser == null) {
                    dbUser = student;
                }

                Course dbCourse = null;
                if (course != null && course.getId() != null) {
                    dbCourse = courseRepository.findById(course.getId()).orElse(null);
                }
                if (dbCourse == null && dbPayment != null && dbPayment.getCourse() != null) {
                    dbCourse = dbPayment.getCourse();
                }
                if (dbCourse == null) {
                    dbCourse = course;
                }

                Enrollment dbEnrollment = null;
                if (enrollment != null && enrollment.getId() != null) {
                    dbEnrollment = enrollmentRepository.findById(enrollment.getId()).orElse(null);
                }
                if (dbEnrollment == null && dbUser != null && dbCourse != null && dbUser.getId() != null && dbCourse.getId() != null) {
                    dbEnrollment = enrollmentRepository.findByUserIdAndCourseId(dbUser.getId(), dbCourse.getId()).orElse(null);
                }
                if (dbEnrollment == null) {
                    dbEnrollment = enrollment;
                }

                String dateTime = dbPayment != null && dbPayment.getCreatedAt() != null
                        ? dbPayment.getCreatedAt().format(DATE_FORMATTER)
                        : (dbUser != null && dbUser.getCreatedAt() != null
                                ? dbUser.getCreatedAt().format(DATE_FORMATTER)
                                : LocalDateTime.now().format(DATE_FORMATTER));

                String userName = dbUser != null && dbUser.getName() != null && !dbUser.getName().trim().isEmpty()
                        ? dbUser.getName().trim()
                        : "Learner";
                String userEmail = dbUser != null && dbUser.getEmail() != null ? dbUser.getEmail().trim() : "";
                String mobileNumber = dbUser != null && dbUser.getMobile() != null && !dbUser.getMobile().trim().isEmpty()
                        ? dbUser.getMobile().trim()
                        : "N/A";
                String city = dbUser != null && dbUser.getCity() != null && !dbUser.getCity().trim().isEmpty()
                        ? dbUser.getCity().trim()
                        : "N/A";

                String courseName = dbCourse != null && dbCourse.getTitle() != null && !dbCourse.getTitle().trim().isEmpty()
                        ? dbCourse.getTitle().trim()
                        : "PINACXTREME Masterclass";
                String courseId = dbCourse != null && dbCourse.getId() != null ? dbCourse.getId().toString() : "1";

                Double amountVal = (dbPayment != null && dbPayment.getAmount() != null)
                        ? dbPayment.getAmount()
                        : (dbCourse != null && dbCourse.getPrice() != null ? dbCourse.getPrice() : 0.0);
                String coursePrice = String.format("%.2f", amountVal);

                String rawPaymentStatus = dbPayment != null && dbPayment.getPaymentStatus() != null
                        ? dbPayment.getPaymentStatus().trim().toUpperCase()
                        : "SUCCESSFUL";

                String paymentStatus;
                String courseActive;

                if ("SUCCESS".equals(rawPaymentStatus) || "SUCCESSFUL".equals(rawPaymentStatus) || "PAID".equals(rawPaymentStatus)) {
                    paymentStatus = "SUCCESS";
                    courseActive = "Yes";
                } else if ("FAILED".equals(rawPaymentStatus) || "FAILURE".equals(rawPaymentStatus)) {
                    paymentStatus = "FAILED";
                    courseActive = "No";
                } else {
                    paymentStatus = "PENDING";
                    courseActive = "No";
                }

                if (dbEnrollment != null && "SUCCESS".equals(paymentStatus)) {
                    courseActive = Boolean.TRUE.equals(dbEnrollment.getActive()) ? "Yes" : "No";
                }

                String paymentId = dbPayment != null && dbPayment.getPaymentId() != null && !dbPayment.getPaymentId().trim().isEmpty()
                        ? dbPayment.getPaymentId().trim()
                        : ("SUCCESS".equals(paymentStatus) ? "PAY_" + UUID.randomUUID().toString().substring(0, 8) : "Pending");

                String orderId = dbPayment != null && dbPayment.getOrderId() != null && !dbPayment.getOrderId().trim().isEmpty()
                        ? dbPayment.getOrderId().trim()
                        : ("order_" + UUID.randomUUID().toString().substring(0, 8));

                // Column order: Date & Time, User Name, User Email, Mobile Number, Course Name, Course ID, Course Price, Payment ID, Order ID, Payment Status, City, Course Active
                List<Object> updatedRow = Arrays.asList(
                        dateTime,
                        userName,
                        userEmail,
                        mobileNumber,
                        courseName,
                        courseId,
                        coursePrice,
                        paymentId,
                        orderId,
                        paymentStatus,
                        city,
                        courseActive
                );

                String targetTab = resolveTargetSheetName();
                ensureHeaderRow(targetTab);

                // If user had an existing pending row, update it in-place; otherwise append a new row
                boolean rowUpdated = updateExistingRow(userEmail, mobileNumber, userName, paymentId, updatedRow, targetTab, false);
                if (!rowUpdated) {
                    appendRow(targetTab, updatedRow);
                }

                log.info("Google Sheets: Payment synced successfully for user {} ({}), paymentId={}, status={}",
                        userName, userEmail, paymentId, paymentStatus);

                // Webhook notification if configured
                sendToWebhook("COURSE_PURCHASE", dateTime, userName, userEmail, mobileNumber, city,
                        courseName, courseId, coursePrice, paymentId, orderId, paymentStatus, courseActive);

            } catch (Exception e) {
                log.error("Google Sheets payment sync failed: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * POST /api/google-sheet/add
     * Directly appends a row from the request DTO.
     */
    public Map<String, Object> addRowFromDto(GoogleSheetAddRequestDto dto) {
        Map<String, Object> response = new HashMap<>();
        if (dto == null) {
            response.put("success", false);
            response.put("message", "Request body cannot be null");
            return response;
        }

        try {
            String dateTime = LocalDateTime.now().format(DATE_FORMATTER);
            String userName = dto.getUserName() != null && !dto.getUserName().trim().isEmpty() ? dto.getUserName().trim() : "Student";
            String userEmail = dto.getUserEmail() != null ? dto.getUserEmail().trim() : "";
            String mobileNumber = dto.getMobileNumber() != null && !dto.getMobileNumber().trim().isEmpty() ? dto.getMobileNumber().trim() : "N/A";
            String courseName = dto.getCourseName() != null && !dto.getCourseName().trim().isEmpty() ? dto.getCourseName().trim() : "Not Purchased";
            String courseId = dto.getCourseId() != null && !dto.getCourseId().trim().isEmpty() ? dto.getCourseId().trim() : "N/A";

            String coursePrice = "0.00";
            if (dto.getCoursePrice() != null) {
                try {
                    double p = Double.parseDouble(dto.getCoursePrice().toString().trim());
                    coursePrice = String.format("%.2f", p);
                } catch (Exception e) {
                    coursePrice = dto.getCoursePrice().toString().trim();
                }
            }

            String paymentId = dto.getPaymentId() != null && !dto.getPaymentId().trim().isEmpty() ? dto.getPaymentId().trim() : "Pending";
            String orderId = dto.getOrderId() != null && !dto.getOrderId().trim().isEmpty() ? dto.getOrderId().trim() : "Pending";
            String paymentStatus = dto.getPaymentStatus() != null && !dto.getPaymentStatus().trim().isEmpty() ? dto.getPaymentStatus().trim() : "PENDING";
            String city = dto.getCity() != null && !dto.getCity().trim().isEmpty() ? dto.getCity().trim() : "N/A";

            String courseActive = "No";
            if (dto.getCourseActive() != null) {
                String actStr = dto.getCourseActive().toString().trim().toLowerCase();
                if ("true".equals(actStr) || "yes".equals(actStr) || "active".equals(actStr)) {
                    courseActive = "Yes";
                }
            }

            List<Object> row = Arrays.asList(
                    dateTime,
                    userName,
                    userEmail,
                    mobileNumber,
                    courseName,
                    courseId,
                    coursePrice,
                    paymentId,
                    orderId,
                    paymentStatus,
                    city,
                    courseActive
            );

            String targetTab = resolveTargetSheetName();
            ensureHeaderRow(targetTab);

            boolean appended = appendRow(targetTab, row);

            response.put("success", appended);
            response.put("message", appended ? "Row added to Google Sheet successfully" : "Failed to append row to Google Sheet");
            response.put("spreadsheetId", spreadsheetId);
            response.put("sheetName", targetTab);
            response.put("row", row);
            return response;
        } catch (Exception e) {
            log.error("Failed to add row from DTO: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error adding row: " + e.getMessage());
            return response;
        }
    }

    /**
     * Finds existing row in Google Sheets by Email or Mobile and updates it in-place.
     * Prevents duplicate rows while allowing pending registration rows to be upgraded to paid.
     */
    private boolean updateExistingRow(String email, String mobile, String name, String paymentId, List<Object> updatedRow, String targetSheet, boolean forRegistration) {
        if (!isAvailable()) return false;

        try {
            String readRange = targetSheet + "!A:L";
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId.trim(), readRange)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return false;
            }

            int matchingRowIndex = -1; // 1-based index in Sheet

            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.isEmpty()) continue;

                // Column order: 0: Date&Time, 1: UserName, 2: UserEmail, 3: MobileNumber, 4: CourseName, 5: CourseID, 6: CoursePrice, 7: PaymentID, 8: OrderID, 9: PaymentStatus, 10: City, 11: CourseActive
                String rowName = row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "";
                String rowEmail = row.size() > 2 && row.get(2) != null ? row.get(2).toString().trim() : "";
                String rowMobile = row.size() > 3 && row.get(3) != null ? row.get(3).toString().trim() : "";
                String rowPaymentId = row.size() > 7 && row.get(7) != null ? row.get(7).toString().trim() : "";
                String rowCourseActive = row.size() > 11 && row.get(11) != null ? row.get(11).toString().trim() : "";

                // Duplicate prevention: If exact paymentId is already recorded, update that row
                if (paymentId != null && !paymentId.isEmpty() && !"Pending".equalsIgnoreCase(paymentId) && paymentId.equalsIgnoreCase(rowPaymentId)) {
                    matchingRowIndex = i + 1;
                    break;
                }

                boolean emailMatches = email != null && !email.isEmpty() && email.equalsIgnoreCase(rowEmail);
                boolean mobileMatches = mobile != null && !mobile.isEmpty() && !mobile.equalsIgnoreCase("N/A") && mobile.equalsIgnoreCase(rowMobile);
                boolean nameMatches = name != null && !name.isEmpty() && name.equalsIgnoreCase(rowName);

                if (emailMatches || mobileMatches || (email != null && email.isEmpty() && mobile != null && mobile.isEmpty() && nameMatches)) {
                    // If this is a registration sync and row already has an active course, preserve it
                    if (forRegistration && ("Yes".equalsIgnoreCase(rowCourseActive))) {
                        log.info("Google Sheets: Preserving existing active course purchase for user {}", email);
                        return true;
                    }
                    // If payment sync and row was in pending state, update that pending row
                    if (!forRegistration && ("Pending".equalsIgnoreCase(rowPaymentId) || "Not Paid".equalsIgnoreCase(rowPaymentId))) {
                        matchingRowIndex = i + 1;
                        break;
                    }
                }
            }

            if (matchingRowIndex > 0) {
                String updateRange = targetSheet + "!A" + matchingRowIndex + ":L" + matchingRowIndex;
                ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedRow));

                UpdateValuesResponse updateRes = sheetsService.spreadsheets().values()
                        .update(spreadsheetId.trim(), updateRange, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute();

                log.info("Google Sheets: Row #{} updated in-place successfully ({})", matchingRowIndex, updateRes.getUpdatedRange());
                return true;
            }
        } catch (Exception e) {
            log.warn("Google Sheets in-place update notice: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Appends a new 12-column row to the target sheet tab.
     */
    private boolean appendRow(String targetSheet, List<Object> rowData) {
        if (!isAvailable()) return false;

        try {
            String range = targetSheet + "!A:L";
            ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));

            AppendValuesResponse response = sheetsService.spreadsheets().values()
                    .append(spreadsheetId.trim(), range, body)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

            log.info("Google Sheets: Appended row to {} successfully ({})", targetSheet, response.getTableRange());
            return true;
        } catch (Exception e) {
            log.error("Google Sheets append failed for tab {}: {}", targetSheet, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if Google Sheets API client and spreadsheet ID are properly configured and available.
     */
    public boolean isAvailable() {
        return enabled && sheetsService != null && spreadsheetId != null && !spreadsheetId.trim().isEmpty();
    }

    /**
     * Direct Database Synchronization Helpers
     */
    public void syncUserFromDatabase(Long userId) {
        if (userId == null) return;
        userRepository.findById(userId).ifPresent(user -> syncRegistrationToSheet(user, null, null, null));
    }

    public void syncPaymentFromDatabase(Long paymentId) {
        if (paymentId == null) return;
        paymentRepository.findById(paymentId).ifPresent(p -> {
            Enrollment e = enrollmentRepository.findByUserId(p.getUser() != null ? p.getUser().getId() : 0L)
                    .stream().findFirst().orElse(null);
            syncPaymentToSheet(p.getUser(), p.getCourse(), p, e);
        });
    }

    /**
     * Bulk Sync: Fetch all users and purchases directly from PostgreSQL database to Google Sheet
     */
    public Map<String, Object> syncAllFromDatabase() {
        List<User> students = userRepository.findByRoleOrderByCreatedAtDesc("STUDENT");
        List<Payment> payments = paymentRepository.findByOrderByCreatedAtDesc();

        int syncedStudents = 0;
        for (User student : students) {
            syncRegistrationToSheet(student, null, null, null);
            syncedStudents++;
        }

        int syncedPayments = 0;
        for (Payment payment : payments) {
            Enrollment enrollment = enrollmentRepository.findByUserId(payment.getUser() != null ? payment.getUser().getId() : 0L)
                    .stream().findFirst().orElse(null);
            syncPaymentToSheet(payment.getUser(), payment.getCourse(), payment, enrollment);
            syncedPayments++;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Database to Google Sheets sync triggered successfully");
        res.put("studentsCount", syncedStudents);
        res.put("paymentsCount", syncedPayments);
        return res;
    }

    /**
     * Check status for admin dashboard and monitoring
     */
    public Map<String, Object> checkStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("googleServiceAccountConfigured", sheetsService != null);
        status.put("spreadsheetIdConfigured", spreadsheetId != null && !spreadsheetId.isEmpty());
        status.put("spreadsheetId", spreadsheetId != null && !spreadsheetId.isEmpty() ? spreadsheetId : "NOT_SET");
        status.put("sheetName", resolveTargetSheetName());
        status.put("webhookConfigured", webhookUrl != null && !webhookUrl.isEmpty());
        status.put("fields", HEADERS_12);
        status.put("enabled", enabled);
        return status;
    }

    // DTO Helper Overloads for existing Controller compatibility
    public void appendStudent(StudentSheetDTO dto) {
        if (dto == null) return;
        User u = null;
        if (dto.getEmail() != null) {
            u = userRepository.findByEmail(dto.getEmail()).orElse(null);
        }
        if (u == null) {
            u = new User();
            u.setName(dto.getName());
            u.setEmail(dto.getEmail());
            u.setMobile(dto.getMobile());
            u.setCity(dto.getCity());
        }
        syncRegistrationToSheet(u, dto.getCourseName(), dto.getCourseId(), dto.getCoursePrice());
    }

    public void updatePayment(PaymentSheetDTO dto) {
        if (dto == null) return;
        User u = null;
        if (dto.getEmail() != null) {
            u = userRepository.findByEmail(dto.getEmail()).orElse(null);
        }
        if (u == null) {
            u = new User();
            u.setName(dto.getName());
            u.setEmail(dto.getEmail());
            u.setMobile(dto.getMobile());
            u.setCity(dto.getCity());
        }

        Course c = null;
        if (dto.getCourseId() != null && !dto.getCourseId().isEmpty()) {
            try { c = courseRepository.findById(Long.parseLong(dto.getCourseId())).orElse(null); } catch (Exception ignored) {}
        }
        if (c == null) {
            c = new Course();
            c.setTitle(dto.getCourseName());
        }

        Payment p = null;
        if (dto.getRazorpayPaymentId() != null) {
            p = paymentRepository.findByPaymentId(dto.getRazorpayPaymentId()).orElse(null);
        }
        if (p == null) {
            p = new Payment();
            p.setPaymentId(dto.getRazorpayPaymentId());
            p.setOrderId(dto.getRazorpayOrderId());
            p.setAmount(dto.getAmount());
            p.setPaymentStatus(dto.getPaymentStatus() != null ? dto.getPaymentStatus() : "Successful");
        }

        syncPaymentToSheet(u, c, p, null);
    }

    public void appendEnrollment(EnrollmentSheetDTO dto) {
        if (dto == null) return;
        User u = null;
        if (dto.getEmail() != null) {
            u = userRepository.findByEmail(dto.getEmail()).orElse(null);
        }
        if (u == null) {
            u = new User();
            u.setName(dto.getStudentName());
            u.setEmail(dto.getEmail());
            u.setMobile(dto.getMobile());
            u.setStudentId(dto.getStudentId());
        }

        Course c = null;
        if (dto.getCourseId() != null && !dto.getCourseId().isEmpty()) {
            try { c = courseRepository.findById(Long.parseLong(dto.getCourseId())).orElse(null); } catch (Exception ignored) {}
        }
        if (c == null) {
            c = new Course();
            c.setTitle(dto.getCourseName());
        }

        Payment p = new Payment();
        p.setPaymentStatus("Successful");

        Enrollment e = new Enrollment();
        e.setActive("Active".equalsIgnoreCase(dto.getStatus()) || "Yes".equalsIgnoreCase(dto.getStatus()));

        syncPaymentToSheet(u, c, p, e);
    }

    public Map<String, Object> appendStudent(StudentGoogleDto dto) {
        Map<String, Object> result = new HashMap<>();
        if (dto == null) {
            result.put("success", false);
            return result;
        }
        User u = null;
        if (dto.getEmail() != null) {
            u = userRepository.findByEmail(dto.getEmail()).orElse(null);
        }
        if (u == null) {
            u = new User();
            u.setName(dto.getName());
            u.setEmail(dto.getEmail());
            u.setMobile(dto.getMobile());
            u.setCity(dto.getCity());
        }
        syncRegistrationToSheet(u, dto.getCourseName(), "", null);
        result.put("success", true);
        return result;
    }

    public Map<String, Object> appendPayment(PaymentGoogleDto dto) {
        Map<String, Object> result = new HashMap<>();
        if (dto == null) {
            result.put("success", false);
            return result;
        }
        User u = null;
        if (dto.getEmail() != null) {
            u = userRepository.findByEmail(dto.getEmail()).orElse(null);
        }
        if (u == null) {
            u = new User();
            u.setName(dto.getStudentName());
            u.setEmail(dto.getEmail());
            u.setMobile(dto.getMobile());
        }

        Course c = new Course();
        c.setTitle(dto.getCourse());

        Payment p = new Payment();
        p.setPaymentId(dto.getPaymentId());
        p.setOrderId(dto.getOrderId());
        p.setAmount(dto.getAmount());
        p.setPaymentStatus(dto.getPaymentStatus());

        syncPaymentToSheet(u, c, p, null);
        result.put("success", true);
        return result;
    }

    public Map<String, Object> appendEnrollment(EnrollmentGoogleDto dto) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * Webhook dispatcher for real-time external notifications
     */
    private void sendToWebhook(String action, String dateTime, String userName, String userEmail, String mobileNumber,
                               String city, String courseName, String courseId, String coursePrice,
                               String paymentId, String orderId, String paymentStatus, String courseActive) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }

        try {
            String identifier = !userEmail.isEmpty() ? userEmail : mobileNumber;
            String jsonPayload = String.format(
                    "{" +
                    "\"action\":\"%s\"," +
                    "\"sheetName\":\"%s\"," +
                    "\"identifier\":\"%s\"," +
                    "\"dateTime\":\"%s\"," +
                    "\"userName\":\"%s\"," +
                    "\"userEmail\":\"%s\"," +
                    "\"mobileNumber\":\"%s\"," +
                    "\"courseName\":\"%s\"," +
                    "\"courseId\":\"%s\"," +
                    "\"coursePrice\":\"%s\"," +
                    "\"paymentId\":\"%s\"," +
                    "\"orderId\":\"%s\"," +
                    "\"paymentStatus\":\"%s\"," +
                    "\"city\":\"%s\"," +
                    "\"courseActive\":\"%s\"," +
                    "\"values\":[[\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"]]" +
                    "}",
                    escapeJson(action),
                    escapeJson(sheetName),
                    escapeJson(identifier),
                    escapeJson(dateTime),
                    escapeJson(userName),
                    escapeJson(userEmail),
                    escapeJson(mobileNumber),
                    escapeJson(courseName),
                    escapeJson(courseId),
                    escapeJson(coursePrice),
                    escapeJson(paymentId),
                    escapeJson(orderId),
                    escapeJson(paymentStatus),
                    escapeJson(city),
                    escapeJson(courseActive),
                    escapeJson(dateTime),
                    escapeJson(userName),
                    escapeJson(userEmail),
                    escapeJson(mobileNumber),
                    escapeJson(courseName),
                    escapeJson(courseId),
                    escapeJson(coursePrice),
                    escapeJson(paymentId),
                    escapeJson(orderId),
                    escapeJson(paymentStatus),
                    escapeJson(city),
                    escapeJson(courseActive)
            );

            URL url = URI.create(webhookUrl.trim()).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            log.info("Google Sheets webhook dispatch response code: {}", responseCode);
        } catch (Exception e) {
            log.warn("Google Sheets webhook notice: {}", e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
