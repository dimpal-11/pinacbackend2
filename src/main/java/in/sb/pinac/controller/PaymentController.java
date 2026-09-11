package in.sb.pinac.controller;

import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.Payment;
import in.sb.pinac.repository.EnrollmentRepository;
import in.sb.pinac.repository.PaymentRepository;
import in.sb.pinac.service.ExcelExportService;
import in.sb.pinac.service.PaymentService;
import in.sb.pinac.service.PdfInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api/payments", "/api/payment", "/payments", "/payment"})
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PdfInvoiceService pdfInvoiceService;

    @Autowired
    private ExcelExportService excelExportService;

    // CREATE PAYMENT ORDER
    @PostMapping({"", "/create-order"})
    public ResponseEntity<?> createOrder(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String couponCode,
            @RequestBody(required = false) Map<String, Object> body) {

        Long parsedUserId = userId;
        Long parsedCourseId = courseId;
        Double parsedAmount = amount;
        String parsedCoupon = couponCode;

        if (body != null) {
            if (body.get("userId") != null) parsedUserId = Long.valueOf(body.get("userId").toString());
            if (body.get("courseId") != null) parsedCourseId = Long.valueOf(body.get("courseId").toString());
            if (body.get("amount") != null) parsedAmount = Double.valueOf(body.get("amount").toString());
            if (body.get("couponCode") != null) parsedCoupon = body.get("couponCode").toString();
        }

        if (parsedCourseId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "courseId is required to create a payment order.");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> order = paymentService.createOrder(parsedUserId, parsedCourseId, parsedCoupon, parsedAmount);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // VERIFY PAYMENT & ACTIVATE ENROLLMENT
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body) {
        Long paymentId = body.get("paymentId") != null ? Long.valueOf(body.get("paymentId").toString()) : null;
        String razorpayPaymentId = (String) body.get("razorpay_payment_id");
        String razorpayOrderId = (String) body.get("razorpay_order_id");
        String razorpaySignature = (String) body.get("razorpay_signature");

        String studentName = (String) body.get("name");
        String studentEmail = (String) body.get("email");
        String studentMobile = (String) body.get("mobile");

        try {
            Map<String, Object> result = paymentService.verifyAndCompletePayment(
                    paymentId, razorpayPaymentId, razorpayOrderId, razorpaySignature,
                    studentName, studentEmail, studentMobile
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // RAZORPAY WEBHOOK ENDPOINT
    @PostMapping("/webhook")
    public ResponseEntity<?> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        try {
            Map<String, Object> result = paymentService.processWebhook(rawPayload, signature);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "error_logged", "message", e.getMessage()));
        }
    }

    // DOWNLOAD PDF INVOICE
    @GetMapping("/invoice/{invoiceNumber}/pdf")
    public ResponseEntity<byte[]> downloadPdfInvoice(@PathVariable String invoiceNumber) {
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));

        byte[] pdfBytes = pdfInvoiceService.generateInvoicePdf(payment);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PINAC_Invoice_" + invoiceNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // DOWNLOAD EXCEL SPREADSHEET
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportAllPaymentsExcel() {
        List<Payment> payments = paymentRepository.findByOrderByCreatedAtDesc();
        byte[] excelBytes = excelExportService.generatePaymentsExcel(payments);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PINAC_Payments_Report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    // GET USER PAYMENTS
    @GetMapping("/user/{userId}")
    public List<Payment> getUserPayments(@PathVariable Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    // GET USER ENROLLMENTS
    @GetMapping("/enrollments/user/{userId}")
    public List<Enrollment> getUserEnrollments(@PathVariable Long userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    // GET ALL PAYMENTS
    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findByOrderByCreatedAtDesc();
    }
}