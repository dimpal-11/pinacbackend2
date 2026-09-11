package in.sb.pinac.service.admin;

import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public List<Map<String, Object>> getAllOrders(String search, String status) {
        List<Enrollment> enrollments = enrollmentRepository.findAll();

        return enrollments.stream()
                .filter(e -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String q = search.trim().toLowerCase();
                    boolean matchStudent = e.getUser() != null && (
                            (e.getUser().getName() != null && e.getUser().getName().toLowerCase().contains(q)) ||
                            (e.getUser().getEmail() != null && e.getUser().getEmail().toLowerCase().contains(q)) ||
                            (e.getUser().getMobile() != null && e.getUser().getMobile().contains(q))
                    );
                    boolean matchCourse = e.getCourse() != null && e.getCourse().getTitle() != null && e.getCourse().getTitle().toLowerCase().contains(q);
                    boolean matchPayment = e.getPayment() != null && (
                            (e.getPayment().getPaymentId() != null && e.getPayment().getPaymentId().toLowerCase().contains(q)) ||
                            (e.getPayment().getOrderId() != null && e.getPayment().getOrderId().toLowerCase().contains(q)) ||
                            (e.getPayment().getInvoiceNumber() != null && e.getPayment().getInvoiceNumber().toLowerCase().contains(q))
                    );
                    return matchStudent || matchCourse || matchPayment;
                })
                .filter(e -> {
                    if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) return true;
                    if ("ACTIVE".equalsIgnoreCase(status)) return Boolean.TRUE.equals(e.getActive());
                    if ("INACTIVE".equalsIgnoreCase(status)) return !Boolean.TRUE.equals(e.getActive());
                    return true;
                })
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orderId", e.getId());
                    map.put("studentId", e.getUser() != null ? e.getUser().getStudentId() : "");
                    map.put("studentName", e.getUser() != null ? e.getUser().getName() : "Learner");
                    map.put("studentEmail", e.getUser() != null ? e.getUser().getEmail() : "");
                    map.put("studentMobile", e.getUser() != null ? e.getUser().getMobile() : "");
                    map.put("courseId", e.getCourse() != null ? e.getCourse().getId() : null);
                    map.put("courseName", e.getCourse() != null ? e.getCourse().getTitle() : "Course");
                    map.put("coursePrice", e.getCourse() != null ? e.getCourse().getPrice() : 0.0);
                    map.put("paymentAmount", e.getPayment() != null ? e.getPayment().getAmount() : (e.getCourse() != null ? e.getCourse().getPrice() : 0.0));
                    map.put("paymentStatus", e.getPayment() != null ? e.getPayment().getPaymentStatus() : "SUCCESS");
                    map.put("invoiceNumber", e.getPayment() != null ? e.getPayment().getInvoiceNumber() : "INV-PINAC-" + e.getId());
                    map.put("razorpayPaymentId", e.getPayment() != null ? e.getPayment().getPaymentId() : "");
                    map.put("progressPercentage", e.getProgressPercentage() != null ? e.getProgressPercentage() : 0);
                    map.put("active", e.getActive());
                    map.put("orderDate", e.getEnrolledAt() != null ? e.getEnrolledAt().format(DATE_FMT) : "");
                    return map;
                })
                .collect(Collectors.toList());
    }
}
