package in.sb.pinac.service.admin;

import in.sb.pinac.entity.Payment;
import in.sb.pinac.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminPaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public List<Map<String, Object>> getAllPayments(String search, String status, String startDate, String endDate) {
        List<Payment> payments = paymentRepository.findByOrderByCreatedAtDesc();

        LocalDateTime start = (startDate != null && !startDate.trim().isEmpty())
                ? LocalDate.parse(startDate).atStartOfDay()
                : null;

        LocalDateTime end = (endDate != null && !endDate.trim().isEmpty())
                ? LocalDate.parse(endDate).atTime(23, 59, 59)
                : null;

        return payments.stream()
                .filter(p -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String q = search.trim().toLowerCase();
                    boolean matchPaymentId = p.getPaymentId() != null && p.getPaymentId().toLowerCase().contains(q);
                    boolean matchOrderId = p.getOrderId() != null && p.getOrderId().toLowerCase().contains(q);
                    boolean matchInvoice = p.getInvoiceNumber() != null && p.getInvoiceNumber().toLowerCase().contains(q);
                    boolean matchUser = p.getUser() != null && (
                            (p.getUser().getName() != null && p.getUser().getName().toLowerCase().contains(q)) ||
                            (p.getUser().getEmail() != null && p.getUser().getEmail().toLowerCase().contains(q)) ||
                            (p.getUser().getMobile() != null && p.getUser().getMobile().contains(q))
                    );
                    boolean matchCourse = p.getCourse() != null && p.getCourse().getTitle() != null && p.getCourse().getTitle().toLowerCase().contains(q);
                    return matchPaymentId || matchOrderId || matchInvoice || matchUser || matchCourse;
                })
                .filter(p -> {
                    if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) return true;
                    return p.getPaymentStatus() != null && p.getPaymentStatus().equalsIgnoreCase(status.trim());
                })
                .filter(p -> {
                    if (start != null && p.getCreatedAt() != null && p.getCreatedAt().isBefore(start)) return false;
                    if (end != null && p.getCreatedAt() != null && p.getCreatedAt().isAfter(end)) return false;
                    return true;
                })
                .map(this::mapPaymentDetails)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Payment not found with ID: " + id));
        return mapPaymentDetails(payment);
    }

    private Map<String, Object> mapPaymentDetails(Payment p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("paymentId", p.getPaymentId() != null ? p.getPaymentId() : "pay_" + (100000 + p.getId()));
        map.put("orderId", p.getOrderId() != null ? p.getOrderId() : "order_" + (200000 + p.getId()));
        map.put("invoiceNumber", p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "INV-PINAC-" + p.getId());
        map.put("studentId", p.getUser() != null ? p.getUser().getStudentId() : "");
        map.put("studentName", p.getUser() != null ? p.getUser().getName() : "Learner");
        map.put("studentEmail", p.getUser() != null ? p.getUser().getEmail() : "");
        map.put("studentMobile", p.getUser() != null ? p.getUser().getMobile() : "");
        map.put("studentCity", p.getUser() != null ? p.getUser().getCity() : "Nashik");
        map.put("courseId", p.getCourse() != null ? p.getCourse().getId() : null);
        map.put("courseName", p.getCourse() != null ? p.getCourse().getTitle() : "Course");
        map.put("courseCategory", p.getCourse() != null ? p.getCourse().getCategory() : "");
        map.put("amount", p.getAmount() != null ? p.getAmount() : 0.0);
        map.put("currency", p.getCurrency() != null ? p.getCurrency() : "INR");
        map.put("paymentStatus", p.getPaymentStatus() != null ? p.getPaymentStatus() : "SUCCESS");
        map.put("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod() : "RAZORPAY");
        map.put("date", p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FMT) : "");
        return map;
    }
}
