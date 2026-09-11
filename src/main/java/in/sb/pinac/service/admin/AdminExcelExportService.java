package in.sb.pinac.service.admin;

import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.Payment;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.CourseRepository;
import in.sb.pinac.repository.EnrollmentRepository;
import in.sb.pinac.repository.PaymentRepository;
import in.sb.pinac.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminExcelExportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public byte[] exportExcel(String type, String timeframe, String startDate, String endDate) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("PINAC Admin Report");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Data Style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Amount Style
            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setAlignment(HorizontalAlignment.RIGHT);
            amountStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            DataFormat format = workbook.createDataFormat();
            amountStyle.setDataFormat(format.getFormat("₹#,##0.00"));

            LocalDateTime[] range = calculateDateRange(timeframe, startDate, endDate);
            LocalDateTime start = range[0];
            LocalDateTime end = range[1];

            if ("students".equalsIgnoreCase(type)) {
                buildStudentsSheet(workbook, sheet, headerStyle, dataStyle, start, end);
            } else if ("course_sales".equalsIgnoreCase(type)) {
                buildCourseSalesSheet(workbook, sheet, headerStyle, dataStyle, amountStyle, start, end);
            } else if ("revenue".equalsIgnoreCase(type)) {
                buildRevenueSheet(workbook, sheet, headerStyle, dataStyle, amountStyle, start, end);
            } else {
                // Default payments export
                buildPaymentsSheet(workbook, sheet, headerStyle, dataStyle, amountStyle, start, end);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public byte[] exportCsv(String type, String timeframe, String startDate, String endDate) {
        StringBuilder csv = new StringBuilder();
        LocalDateTime[] range = calculateDateRange(timeframe, startDate, endDate);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        if ("students".equalsIgnoreCase(type)) {
            csv.append("Student ID,Full Name,Email,Mobile,City,Registration Date,Status,Purchased Courses Count\n");
            List<User> students = userRepository.findByRoleOrderByCreatedAtDesc("STUDENT");
            for (User s : filterByDate(students, User::getCreatedAt, start, end)) {
                csv.append(escapeCsv(s.getStudentId())).append(",")
                        .append(escapeCsv(s.getName())).append(",")
                        .append(escapeCsv(s.getEmail())).append(",")
                        .append(escapeCsv(s.getMobile())).append(",")
                        .append(escapeCsv(s.getCity() != null ? s.getCity() : "Nashik")).append(",")
                        .append(escapeCsv(s.getCreatedAt() != null ? s.getCreatedAt().format(DATE_FORMATTER) : "")).append(",")
                        .append(escapeCsv(Boolean.TRUE.equals(s.getActive()) ? "Active" : "Inactive")).append(",")
                        .append(enrollmentRepository.findByUserId(s.getId()).size()).append("\n");
            }
        } else {
            csv.append("Payment ID,Order ID,Invoice Number,Student Name,Email,Mobile,Course Name,Amount,Status,Date\n");
            List<Payment> payments = paymentRepository.findByOrderByCreatedAtDesc();
            for (Payment p : filterByDate(payments, Payment::getCreatedAt, start, end)) {
                csv.append(escapeCsv(p.getPaymentId())).append(",")
                        .append(escapeCsv(p.getOrderId())).append(",")
                        .append(escapeCsv(p.getInvoiceNumber())).append(",")
                        .append(escapeCsv(p.getUser() != null ? p.getUser().getName() : "")).append(",")
                        .append(escapeCsv(p.getUser() != null ? p.getUser().getEmail() : "")).append(",")
                        .append(escapeCsv(p.getUser() != null ? p.getUser().getMobile() : "")).append(",")
                        .append(escapeCsv(p.getCourse() != null ? p.getCourse().getTitle() : "")).append(",")
                        .append(p.getAmount() != null ? p.getAmount() : 0.0).append(",")
                        .append(escapeCsv(p.getPaymentStatus())).append(",")
                        .append(escapeCsv(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FORMATTER) : "")).append("\n");
            }
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void buildPaymentsSheet(Workbook wb, Sheet sheet, CellStyle headerStyle, CellStyle dataStyle, CellStyle amountStyle, LocalDateTime start, LocalDateTime end) {
        String[] columns = {
                "Date & Time", "Payment ID", "Order ID", "Invoice No", "Student Name", "Email", "Mobile", "City", "Course Name", "Amount (INR)", "Payment Status", "Method"
        };

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < columns.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(columns[i]);
            c.setCellStyle(headerStyle);
        }

        List<Payment> payments = filterByDate(paymentRepository.findByOrderByCreatedAtDesc(), Payment::getCreatedAt, start, end);
        int rIdx = 1;
        for (Payment p : payments) {
            Row row = sheet.createRow(rIdx++);
            row.setHeightInPoints(20);

            createCell(row, 0, p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FORMATTER) : "", dataStyle);
            createCell(row, 1, p.getPaymentId() != null ? p.getPaymentId() : "", dataStyle);
            createCell(row, 2, p.getOrderId() != null ? p.getOrderId() : "", dataStyle);
            createCell(row, 3, p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "", dataStyle);
            createCell(row, 4, p.getUser() != null ? p.getUser().getName() : "", dataStyle);
            createCell(row, 5, p.getUser() != null ? p.getUser().getEmail() : "", dataStyle);
            createCell(row, 6, p.getUser() != null ? p.getUser().getMobile() : "", dataStyle);
            createCell(row, 7, p.getUser() != null && p.getUser().getCity() != null ? p.getUser().getCity() : "Nashik", dataStyle);
            createCell(row, 8, p.getCourse() != null ? p.getCourse().getTitle() : "", dataStyle);

            Cell amountCell = row.createCell(9);
            amountCell.setCellValue(p.getAmount() != null ? p.getAmount() : 0.0);
            amountCell.setCellStyle(amountStyle);

            createCell(row, 10, p.getPaymentStatus() != null ? p.getPaymentStatus() : "SUCCESS", dataStyle);
            createCell(row, 11, p.getPaymentMethod() != null ? p.getPaymentMethod() : "RAZORPAY", dataStyle);
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
    }

    private void buildStudentsSheet(Workbook wb, Sheet sheet, CellStyle headerStyle, CellStyle dataStyle, LocalDateTime start, LocalDateTime end) {
        String[] columns = {
                "Registration Date", "Student ID", "Full Name", "Email", "Mobile", "City", "Account Status", "Enrolled Courses"
        };

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < columns.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(columns[i]);
            c.setCellStyle(headerStyle);
        }

        List<User> students = filterByDate(userRepository.findByRoleOrderByCreatedAtDesc("STUDENT"), User::getCreatedAt, start, end);
        int rIdx = 1;
        for (User s : students) {
            Row row = sheet.createRow(rIdx++);
            row.setHeightInPoints(20);

            List<Enrollment> enrollments = enrollmentRepository.findByUserId(s.getId());
            String courses = enrollments.stream().filter(e -> e.getCourse() != null).map(e -> e.getCourse().getTitle()).collect(Collectors.joining(", "));

            createCell(row, 0, s.getCreatedAt() != null ? s.getCreatedAt().format(DATE_FORMATTER) : "", dataStyle);
            createCell(row, 1, s.getStudentId() != null ? s.getStudentId() : "PINAC-STU-" + s.getId(), dataStyle);
            createCell(row, 2, s.getName(), dataStyle);
            createCell(row, 3, s.getEmail() != null ? s.getEmail() : "", dataStyle);
            createCell(row, 4, s.getMobile() != null ? s.getMobile() : "", dataStyle);
            createCell(row, 5, s.getCity() != null ? s.getCity() : "Nashik", dataStyle);
            createCell(row, 6, Boolean.TRUE.equals(s.getActive()) ? "Active" : "Inactive", dataStyle);
            createCell(row, 7, courses, dataStyle);
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
    }

    private void buildCourseSalesSheet(Workbook wb, Sheet sheet, CellStyle headerStyle, CellStyle dataStyle, CellStyle amountStyle, LocalDateTime start, LocalDateTime end) {
        String[] columns = {
                "Course ID", "Course Name", "Category", "Price (INR)", "Total Enrollments", "Total Revenue (INR)", "Status"
        };

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < columns.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(columns[i]);
            c.setCellStyle(headerStyle);
        }

        List<Course> courses = courseRepository.findAll();
        List<Payment> allPayments = filterByDate(paymentRepository.findByOrderByCreatedAtDesc(), Payment::getCreatedAt, start, end);
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();

        int rIdx = 1;
        for (Course c : courses) {
            Row row = sheet.createRow(rIdx++);
            row.setHeightInPoints(20);

            double revenue = allPayments.stream()
                    .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) && p.getCourse() != null && Objects.equals(p.getCourse().getId(), c.getId()))
                    .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                    .sum();

            long enrollments = allEnrollments.stream()
                    .filter(e -> e.getCourse() != null && Objects.equals(e.getCourse().getId(), c.getId()))
                    .count();

            createCell(row, 0, String.valueOf(c.getId()), dataStyle);
            createCell(row, 1, c.getTitle(), dataStyle);
            createCell(row, 2, c.getCategory() != null ? c.getCategory() : "", dataStyle);

            Cell priceCell = row.createCell(3);
            priceCell.setCellValue(c.getPrice() != null ? c.getPrice() : 0.0);
            priceCell.setCellStyle(amountStyle);

            createCell(row, 4, String.valueOf(enrollments), dataStyle);

            Cell revCell = row.createCell(5);
            revCell.setCellValue(revenue);
            revCell.setCellStyle(amountStyle);

            createCell(row, 6, Boolean.TRUE.equals(c.getActive()) ? "Active" : "Draft", dataStyle);
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
    }

    private void buildRevenueSheet(Workbook wb, Sheet sheet, CellStyle headerStyle, CellStyle dataStyle, CellStyle amountStyle, LocalDateTime start, LocalDateTime end) {
        String[] columns = {
                "Period", "Total Orders", "Successful Payments", "Failed Payments", "Total Revenue (INR)"
        };

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < columns.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(columns[i]);
            c.setCellStyle(headerStyle);
        }

        List<Payment> payments = filterByDate(paymentRepository.findByOrderByCreatedAtDesc(), Payment::getCreatedAt, start, end);
        double totalRev = payments.stream().filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus())).mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0).sum();
        long successCount = payments.stream().filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus())).count();
        long failedCount = payments.stream().filter(p -> "FAILED".equalsIgnoreCase(p.getPaymentStatus())).count();

        Row summaryRow = sheet.createRow(1);
        summaryRow.setHeightInPoints(22);
        createCell(summaryRow, 0, "Selected Period Summary", dataStyle);
        createCell(summaryRow, 1, String.valueOf(payments.size()), dataStyle);
        createCell(summaryRow, 2, String.valueOf(successCount), dataStyle);
        createCell(summaryRow, 3, String.valueOf(failedCount), dataStyle);

        Cell revCell = summaryRow.createCell(4);
        revCell.setCellValue(totalRev);
        revCell.setCellStyle(amountStyle);

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private <T> List<T> filterByDate(List<T> items, java.util.function.Function<T, LocalDateTime> dateGetter, LocalDateTime start, LocalDateTime end) {
        return items.stream().filter(item -> {
            LocalDateTime date = dateGetter.apply(item);
            if (date == null) return true;
            if (start != null && date.isBefore(start)) return false;
            if (end != null && date.isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    private LocalDateTime[] calculateDateRange(String timeframe, String startDate, String endDate) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = null;
        LocalDateTime end = null;

        if (startDate != null && !startDate.trim().isEmpty()) {
            start = LocalDate.parse(startDate.trim()).atStartOfDay();
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            end = LocalDate.parse(endDate.trim()).atTime(23, 59, 59);
        }

        if (start != null || end != null) {
            return new LocalDateTime[]{start, end};
        }

        if ("today".equalsIgnoreCase(timeframe)) {
            start = today.atStartOfDay();
            end = today.atTime(23, 59, 59);
        } else if ("weekly".equalsIgnoreCase(timeframe) || "week".equalsIgnoreCase(timeframe)) {
            start = today.minusDays(7).atStartOfDay();
            end = today.atTime(23, 59, 59);
        } else if ("monthly".equalsIgnoreCase(timeframe) || "month".equalsIgnoreCase(timeframe)) {
            start = today.withDayOfMonth(1).atStartOfDay();
            end = today.atTime(23, 59, 59);
        } else if ("yearly".equalsIgnoreCase(timeframe) || "year".equalsIgnoreCase(timeframe)) {
            start = today.withDayOfYear(1).atStartOfDay();
            end = today.atTime(23, 59, 59);
        }

        return new LocalDateTime[]{start, end};
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
