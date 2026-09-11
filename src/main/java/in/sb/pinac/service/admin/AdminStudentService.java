package in.sb.pinac.service.admin;

import in.sb.pinac.dto.admin.AdminStudentUpdateRequest;
import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.Payment;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.EnrollmentRepository;
import in.sb.pinac.repository.PaymentRepository;
import in.sb.pinac.repository.UserRepository;
import in.sb.pinac.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminStudentService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    /**
     * Get all students with search and status filter.
     */
    public List<Map<String, Object>> getAllStudents(String search, String status) {
        List<User> students = userRepository.findByRoleOrderByCreatedAtDesc("STUDENT");

        return students.stream()
                .filter(s -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String q = search.trim().toLowerCase();
                    boolean matchName = s.getName() != null && s.getName().toLowerCase().contains(q);
                    boolean matchEmail = s.getEmail() != null && s.getEmail().toLowerCase().contains(q);
                    boolean matchMobile = s.getMobile() != null && s.getMobile().contains(q);
                    boolean matchCity = s.getCity() != null && s.getCity().toLowerCase().contains(q);
                    boolean matchStudentId = s.getStudentId() != null && s.getStudentId().toLowerCase().contains(q);
                    return matchName || matchEmail || matchMobile || matchCity || matchStudentId;
                })
                .filter(s -> {
                    if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) return true;
                    if ("ACTIVE".equalsIgnoreCase(status)) return Boolean.TRUE.equals(s.getActive());
                    if ("INACTIVE".equalsIgnoreCase(status)) return !Boolean.TRUE.equals(s.getActive());
                    return true;
                })
                .map(this::mapStudentOverview)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed student profile including purchased courses and payments.
     */
    public Map<String, Object> getStudentDetails(Long id) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));

        List<Enrollment> enrollments = enrollmentRepository.findByUserId(id);
        List<Payment> payments = paymentRepository.findByUserId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("profile", mapStudentOverview(student));

        List<Map<String, Object>> coursesList = enrollments.stream().map(e -> {
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("enrollmentId", e.getId());
            cMap.put("courseId", e.getCourse() != null ? e.getCourse().getId() : null);
            cMap.put("courseTitle", e.getCourse() != null ? e.getCourse().getTitle() : "Course");
            cMap.put("category", e.getCourse() != null ? e.getCourse().getCategory() : "");
            cMap.put("thumbnail", e.getCourse() != null ? e.getCourse().getThumbnail() : "");
            cMap.put("progressPercentage", e.getProgressPercentage() != null ? e.getProgressPercentage() : 0);
            cMap.put("completedLessons", e.getCompletedLessonsCount() != null ? e.getCompletedLessonsCount() : 0);
            cMap.put("enrolledAt", e.getEnrolledAt() != null ? e.getEnrolledAt().format(DATE_FMT) : "");
            cMap.put("active", e.getActive());
            return cMap;
        }).collect(Collectors.toList());

        List<Map<String, Object>> paymentList = payments.stream().map(p -> {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("paymentId", p.getId());
            pMap.put("razorpayPaymentId", p.getPaymentId() != null ? p.getPaymentId() : "");
            pMap.put("orderId", p.getOrderId() != null ? p.getOrderId() : "");
            pMap.put("invoiceNumber", p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "");
            pMap.put("amount", p.getAmount() != null ? p.getAmount() : 0.0);
            pMap.put("status", p.getPaymentStatus() != null ? p.getPaymentStatus() : "SUCCESS");
            pMap.put("method", p.getPaymentMethod() != null ? p.getPaymentMethod() : "RAZORPAY");
            pMap.put("courseName", p.getCourse() != null ? p.getCourse().getTitle() : "");
            pMap.put("date", p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FMT) : "");
            return pMap;
        }).collect(Collectors.toList());

        response.put("enrolledCourses", coursesList);
        response.put("paymentHistory", paymentList);
        return response;
    }

    /**
     * Update Student profile.
     */
    @Transactional
    public Map<String, Object> updateStudent(Long id, AdminStudentUpdateRequest req) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            student.setName(req.getName().trim());
        }

        if (req.getEmail() != null) {
            String cleanEmail = OtpService.normalizeEmail(req.getEmail());
            if (cleanEmail != null && !cleanEmail.equalsIgnoreCase(student.getEmail()) && userRepository.existsByEmail(cleanEmail)) {
                throw new IllegalArgumentException("Email already taken by another account.");
            }
            student.setEmail(cleanEmail);
        }

        if (req.getMobile() != null) {
            String cleanMobile = OtpService.normalizeMobile(req.getMobile());
            if (cleanMobile != null && !cleanMobile.equals(student.getMobile()) && userRepository.existsByMobile(cleanMobile)) {
                throw new IllegalArgumentException("Mobile number already taken by another account.");
            }
            student.setMobile(cleanMobile);
        }

        if (req.getCity() != null) {
            student.setCity(req.getCity().trim());
        }

        if (req.getActive() != null) {
            student.setActive(req.getActive());
        }

        if (req.getAvatar() != null) {
            student.setAvatar(req.getAvatar().trim());
        }

        User saved = userRepository.save(student);
        return mapStudentOverview(saved);
    }

    /**
     * Toggle active / inactive status.
     */
    @Transactional
    public Map<String, Object> toggleStatus(Long id) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));

        student.setActive(!Boolean.TRUE.equals(student.getActive()));
        User saved = userRepository.save(student);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id", saved.getId());
        res.put("active", saved.getActive());
        res.put("message", "Student status updated to " + (saved.getActive() ? "Active" : "Inactive"));
        return res;
    }

    /**
     * Delete student account and clear associations.
     */
    @Transactional
    public void deleteStudent(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    private Map<String, Object> mapStudentOverview(User s) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(s.getId());
        List<Payment> payments = paymentRepository.findByUserId(s.getId());

        double totalSpent = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        List<String> courseTitles = enrollments.stream()
                .filter(e -> e.getCourse() != null)
                .map(e -> e.getCourse().getTitle())
                .collect(Collectors.toList());

        String latestPaymentStatus = payments.isEmpty()
                ? "NONE"
                : (payments.get(0).getPaymentStatus() != null ? payments.get(0).getPaymentStatus() : "SUCCESS");

        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("studentId", s.getStudentId() != null ? s.getStudentId() : "PINAC-STU-" + s.getId());
        map.put("name", s.getName());
        map.put("email", s.getEmail() != null ? s.getEmail() : "");
        map.put("mobile", s.getMobile() != null ? s.getMobile() : "");
        map.put("city", s.getCity() != null ? s.getCity() : "Nashik");
        map.put("avatar", s.getAvatar() != null ? s.getAvatar() : "");
        map.put("active", s.getActive());
        map.put("accountStatus", Boolean.TRUE.equals(s.getActive()) ? "Active" : "Inactive");
        map.put("registrationDate", s.getCreatedAt() != null ? s.getCreatedAt().format(DATE_FMT) : "");
        map.put("purchasedCourses", courseTitles);
        map.put("purchasedCount", courseTitles.size());
        map.put("totalSpent", totalSpent);
        map.put("paymentStatus", latestPaymentStatus);
        return map;
    }
}
