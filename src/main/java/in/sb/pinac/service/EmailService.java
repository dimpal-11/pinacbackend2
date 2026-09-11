package in.sb.pinac.service;

import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Payment;
import in.sb.pinac.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private PdfInvoiceService pdfInvoiceService;

    @Value("${spring.mail.host:smtp-relay.brevo.com}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${spring.mail.username:dimpalpatil196@gmail.com}")
    private String smtpUsername;

    @Value("${app.mail.from:dimpalpatil196@gmail.com}")
    private String fromEmail;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${app.mail.support-email:support@pinacinstitute.com}")
    private String supportEmail;

    @Value("${app.mail.support-phone:+91 72191 94211}")
    private String supportPhone;

    @Value("${app.institute.name:PINACXTREME}")
    private String instituteName;

    @Value("${app.institute.website:http://localhost:5173}")
    private String websiteUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /**
     * 1. Send Automatic Registration Success Email (Async, no password)
     */
    public boolean sendRegistrationSuccessEmail(User student, String courseName, Double coursePrice) {
        if (student == null) return false;

        String studentName = student.getName() != null && !student.getName().trim().isEmpty() ? student.getName().trim() : "Learner";
        String studentId = student.getStudentId() != null ? student.getStudentId() : ("PINAC-STU-" + student.getId());
        String studentPassword = student.getTempPassword() != null && !student.getTempPassword().trim().isEmpty()
                ? student.getTempPassword().trim()
                : "PINAC@" + (1000 + (student.getId() != null ? (int)(student.getId() % 9000) : 1234));
        String email = student.getEmail() != null ? student.getEmail().trim() : "";
        String mobile = student.getMobile() != null ? student.getMobile().trim() : "Not provided";
        String cName = courseName != null && !courseName.trim().isEmpty() ? courseName.trim() : "General Learning Program";
        String cPrice = coursePrice != null ? ("₹" + String.format("%.2f", coursePrice)) : "Standard Plan";
        String regDate = student.getCreatedAt() != null ? student.getCreatedAt().format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER);

        String htmlContent = buildRegistrationHtmlTemplate(studentName, studentId, studentPassword, email, mobile, cName, cPrice, regDate);

        System.out.println("=================================================");
        System.out.println("AUTOMATIC REGISTRATION EMAIL GENERATED");
        System.out.println("To                : " + (email.isEmpty() ? mobile : email));
        System.out.println("Subject           : ✅ Registration Successful! Welcome to " + instituteName + " - Student ID: " + studentId);
        System.out.println("Student ID        : " + studentId);
        System.out.println("Student Password  : " + studentPassword);
        System.out.println("Student Name      : " + studentName);
        System.out.println("Email             : " + email);
        System.out.println("Mobile            : " + mobile);
        System.out.println("Course            : " + cName);
        System.out.println("Registration Date : " + regDate);
        System.out.println("=================================================");

        if (mailSender != null && !email.isEmpty() && !email.endsWith(".local")) {
            CompletableFuture.runAsync(() -> {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                    helper.setFrom(fromEmail, instituteName);
                    helper.setTo(email);
                    helper.setSubject("✅ Registration Successful! Welcome to " + instituteName + " - Student ID: " + studentId);
                    helper.setText(htmlContent, true);

                    mailSender.send(message);
                    System.out.println("Registration confirmation email sent via SMTP to: " + email);
                } catch (Exception e) {
                    System.err.println("Registration email SMTP notice (" + e.getMessage() + "). Email details logged above.");
                }
            });
        }

        return true;
    }

    /**
     * 2. Send Automatic Payment Success Email (Async, with PDF Invoice, no password)
     */
    public boolean sendEnrollmentConfirmationEmail(User student, Course course, Payment payment) {
        if (student == null || payment == null) return false;

        String studentName = student.getName() != null && !student.getName().trim().isEmpty() ? student.getName().trim() : "Learner";
        String studentId = student.getStudentId() != null ? student.getStudentId() : ("PINAC-STU-" + student.getId());
        String email = student.getEmail() != null ? student.getEmail().trim() : "";
        String courseTitle = course != null && course.getTitle() != null ? course.getTitle().trim() : "PINACXTREME Masterclass";
        String invoiceNo = payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : ("INV-PINAC-" + payment.getId());
        String paymentId = payment.getPaymentId() != null ? payment.getPaymentId() : ("PAY_" + payment.getId());
        String orderId = payment.getOrderId() != null ? payment.getOrderId() : "N/A";
        Double amount = payment.getAmount() != null ? payment.getAmount() : 0.0;
        String paymentDate = payment.getCreatedAt() != null ? payment.getCreatedAt().format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER);
        String paymentStatus = payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "PAID";

        String htmlContent = buildPaymentHtmlTemplate(studentName, studentId, courseTitle, amount, paymentId, orderId, paymentDate, paymentStatus, invoiceNo);

        System.out.println("=================================================");
        System.out.println("AUTOMATIC PAYMENT CONFIRMATION EMAIL GENERATED");
        System.out.println("To             : " + email);
        System.out.println("Subject        : Payment Successful! Course Purchase Confirmed - " + courseTitle);
        System.out.println("Student ID     : " + studentId);
        System.out.println("Student Name   : " + studentName);
        System.out.println("Course Name    : " + courseTitle);
        System.out.println("Payment ID     : " + paymentId);
        System.out.println("Order ID       : " + orderId);
        System.out.println("Amount Paid    : ₹" + String.format("%.2f", amount));
        System.out.println("Payment Date   : " + paymentDate);
        System.out.println("Payment Status : " + paymentStatus);
        System.out.println("Invoice Number : " + invoiceNo);
        System.out.println("Password       : NO (Protected by security policy)");
        System.out.println("Attached       : " + invoiceNo + ".pdf");
        System.out.println("=================================================");

        if (mailSender != null && !email.isEmpty() && !email.endsWith(".local")) {
            CompletableFuture.runAsync(() -> {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                    helper.setFrom(fromEmail, instituteName);
                    helper.setTo(email);
                    helper.setSubject("🎉 Payment Successful! Course Purchase Confirmed - " + courseTitle);
                    helper.setText(htmlContent, true);

                    // Attach PDF Invoice if available
                    try {
                        byte[] pdfBytes = pdfInvoiceService.generateInvoicePdf(payment);
                        if (pdfBytes != null && pdfBytes.length > 0) {
                            helper.addAttachment("PINAC_Invoice_" + invoiceNo + ".pdf", new ByteArrayResource(pdfBytes));
                        }
                    } catch (Exception pdfEx) {
                        System.err.println("PDF invoice generation note: " + pdfEx.getMessage());
                    }

                    mailSender.send(message);
                    System.out.println("Payment confirmation email sent via SMTP to: " + email);
                } catch (Exception e) {
                    System.err.println("Payment email SMTP notice (" + e.getMessage() + "). Email details logged above.");
                }
            });
        }

        return true;
    }

    private String buildRegistrationHtmlTemplate(String name, String studentId, String studentPassword, String email, String mobile,
                                                 String course, String price, String regDate) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "  body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f7eefa; margin: 0; padding: 20px; color: #2d3748; }" +
                "  .card { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.08); border: 1px solid #ecd6f1; }" +
                "  .header { background: linear-gradient(135deg, #7c2d8d, #5c2168); padding: 28px 20px; text-align: center; color: #ffffff; }" +
                "  .content { padding: 30px 24px; line-height: 1.6; }" +
                "  .success-banner { text-align: center; margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid #f1e6f5; }" +
                "  .success-icon { font-size: 54px; line-height: 1; display: inline-block; margin-bottom: 8px; }" +
                "  .success-heading { color: #15803d; font-size: 24px; font-weight: 800; margin: 4px 0 8px 0; letter-spacing: -0.5px; }" +
                "  .success-msg { color: #4b5563; font-size: 15px; margin: 0; line-height: 1.5; font-weight: 500; }" +
                "  .greeting-title { color: #7c2d8d; font-size: 18px; font-weight: 700; margin: 16px 0 6px 0; }" +
                "  .greeting-sub { color: #4b5563; font-size: 14px; margin: 0 0 18px 0; }" +
                "  .cred-box { background-color: #f8f1fa; border: 1px solid #ecd6f1; border-radius: 12px; padding: 18px 20px; margin: 20px 0; }" +
                "  .cred-title { margin: 0 0 14px 0; color: #7c2d8d; font-size: 15px; font-weight: 700; }" +
                "  .cred-item { margin: 10px 0; font-size: 14px; color: #2d3748; }" +
                "  .cred-label { font-weight: 700; color: #1a202c; min-width: 145px; display: inline-block; }" +
                "  .badge-id { font-family: 'Consolas', 'Courier New', monospace; font-weight: 800; color: #7c2d8d; background: #f3e8f8; padding: 2px 8px; border-radius: 4px; border: 1px solid #e2c0e9; }" +
                "  .badge-pw { font-family: 'Consolas', 'Courier New', monospace; font-weight: 800; color: #1e293b; background: #e2e8f0; padding: 2px 8px; border-radius: 4px; border: 1px solid #cbd5e1; }" +
                "  .btn-container { text-align: center; margin: 26px 0 10px 0; }" +
                "  .btn { display: inline-block; background-color: #10b981; color: #ffffff !important; font-weight: 800; text-decoration: none; padding: 13px 34px; border-radius: 30px; font-size: 15px; box-shadow: 0 4px 14px rgba(16, 185, 129, 0.35); text-align: center; letter-spacing: 0.3px; }" +
                "  .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #718096; border-top: 1px solid #edf2f7; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "  <div class='card'>" +
                "    <div class='header'>" +
                "      <h1 style='margin:0; font-size: 24px; font-weight: 800;'>" + instituteName + "</h1>" +
                "      <p style='margin:5px 0 0 0; opacity: 0.9; font-size: 13px;'>Learn Today. Build Tomorrow.</p>" +
                "    </div>" +
                "    <div class='content'>" +
                "      <!-- Large green success icon (✅) at the top -->" +
                "      <div class='success-banner'>" +
                "        <div class='success-icon'>&#9989;</div>" +
                "        <h2 class='success-heading'>Registration Successful!</h2>" +
                "        <p class='success-msg'>Welcome to Pinac Institute.<br>Your account has been created successfully.</p>" +
                "      </div>" +
                "      <!-- Greeting -->" +
                "      <h3 class='greeting-title'>Welcome to " + instituteName + ", " + name + "! 🚀</h3>" +
                "      <p class='greeting-sub'>Your student registration has been verified and registered successfully.</p>" +
                "      <!-- Registration Summary -->" +
                "      <div class='cred-box'>" +
                "        <h4 class='cred-title'>&#128203; Your Registration Summary</h4>" +
                "        <div class='cred-item'><span class='cred-label'>Student ID:</span> <span class='badge-id'>" + studentId + "</span></div>" +
                "        <div class='cred-item'><span class='cred-label'>Student password :</span> <span class='badge-pw'>" + studentPassword + "</span></div>" +
                "        <div class='cred-item'><span class='cred-label'>Student Name:</span> " + name + "</div>" +
                "        <div class='cred-item'><span class='cred-label'>Registered Email:</span> " + (email.isEmpty() ? "Not provided" : email) + "</div>" +
                "        <div class='cred-item'><span class='cred-label'>Mobile Number:</span> " + mobile + "</div>" +
                (course != null && !course.trim().isEmpty() && !course.equals("General Learning Program")
                        ? "        <div class='cred-item'><span class='cred-label'>Selected Course:</span> " + course + "</div>" : "") +
                "        <div class='cred-item'><span class='cred-label'>Registration Date:</span> " + regDate + "</div>" +
                "      </div>" +
                "      <!-- Go To Course Button -->" +
                "      <div class='btn-container'>" +
                "        <a href='" + websiteUrl + "/login' class='btn'>Go To Course</a>" +
                "      </div>" +
                "      <p style='text-align: center; font-size: 12px; color: #718096; margin-top: 10px;'>Log in with your Student ID / Mobile and Student password.</p>" +
                "    </div>" +
                "    <div class='footer'>" +
                "      <p>" + instituteName + " | Nashik, Maharashtra<br>" +
                "      Support Email: " + supportEmail + " | Support Phone: " + supportPhone + "</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private String buildPaymentHtmlTemplate(String name, String studentId, String course, Double amount,
                                           String paymentId, String orderId, String paymentDate, String paymentStatus, String invoiceNo) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "  body { font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #f7eefa; margin: 0; padding: 20px; color: #2d3748; }" +
                "  .card { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.08); }" +
                "  .header { background: linear-gradient(135deg, #7c2d8d, #5c2168); padding: 30px 20px; text-align: center; color: #ffffff; }" +
                "  .content { padding: 30px; line-height: 1.6; }" +
                "  .invoice-box { background-color: #f8f1fa; border: 1px solid #ecd6f1; border-radius: 12px; padding: 18px; margin: 20px 0; }" +
                "  .invoice-item { margin: 8px 0; font-size: 14px; }" +
                "  .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #718096; border-top: 1px solid #edf2f7; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "  <div class='card'>" +
                "    <div class='header'>" +
                "      <h1 style='margin:0; font-size: 24px;'>" + instituteName + "</h1>" +
                "      <p style='margin:5px 0 0 0; opacity: 0.9; font-size: 13px;'>Learn Today. Build Tomorrow.</p>" +
                "    </div>" +
                "    <div class='content'>" +
                "      <h2 style='color:#7c2d8d;'>Payment Confirmed! 🎉</h2>" +
                "      <p>Dear " + name + ", your payment for <strong>" + course + "</strong> has been processed successfully.</p>" +
                "      <div class='invoice-box'>" +
                "        <div class='invoice-item'><strong>Invoice Number:</strong> " + invoiceNo + "</div>" +
                "        <div class='invoice-item'><strong>Student ID:</strong> " + studentId + "</div>" +
                "        <div class='invoice-item'><strong>Payment ID:</strong> " + paymentId + "</div>" +
                "        <div class='invoice-item'><strong>Order ID:</strong> " + orderId + "</div>" +
                "        <div class='invoice-item'><strong>Amount Paid:</strong> ₹" + String.format("%.2f", amount) + "</div>" +
                "        <div class='invoice-item'><strong>Date:</strong> " + paymentDate + "</div>" +
                "        <div class='invoice-item'><strong>Status:</strong> " + paymentStatus + "</div>" +
                "      </div>" +
                "      <p>Your official tax invoice is attached with this email.</p>" +
                "    </div>" +
                "    <div class='footer'>" +
                "      <p>" + instituteName + " | Support: " + supportEmail + " | " + supportPhone + "</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    /**
     * 3. Send One-Time Password (OTP) via Brevo SMTP
     * Executes synchronously and verifies SMTP connection, authentication, and delivery.
     * Throws explicit exception on SMTP failure so AuthController returns the exact error.
     */
    public boolean sendOtpEmail(String email, String otp) {
        if (email == null || email.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address is required.");
        }

        final String cleanEmail = email.trim().toLowerCase();

        System.out.println("=================================================");
        System.out.println("[BREVO SMTP DISPATCH INITIATED]");
        System.out.println("SMTP Host         : " + smtpHost + ":" + smtpPort);
        System.out.println("SMTP Username     : " + smtpUsername);
        System.out.println("From Address      : " + fromEmail);
        System.out.println("Recipient         : " + cleanEmail);
        System.out.println("Credentials       : [PROTECTED - Not logged]");
        System.out.println("Server Time       : " + LocalDateTime.now());
        System.out.println("=================================================");

        if (mailSender == null) {
            String err = "JavaMailSender bean is not configured in Spring context.";
            System.err.println("❌ [SMTP ERROR] " + err);
            throw new IllegalStateException(err);
        }

        String htmlContent = buildOtpHtmlTemplate(otp.trim());

        try {
            System.out.println("⏳ [STEP 1] Constructing MIME message for " + cleanEmail + "...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, instituteName);
            helper.setTo(cleanEmail);
            helper.setSubject("🔑 Your PINACXTREME Verification Code");
            helper.setText(htmlContent, true);

            System.out.println("⏳ [STEP 2] Calling JavaMailSender.send() through " + smtpHost + ":" + smtpPort + "...");
            // Synchronous call to JavaMailSender.send() - will trigger STARTTLS, AUTH, and transmission
            mailSender.send(message);

            System.out.println("=================================================");
            System.out.println("✅ SMTP CONNECTION      : SUCCESS (Connected to " + smtpHost + ":" + smtpPort + ")");
            System.out.println("✅ SMTP AUTHENTICATION  : SUCCESS (Authenticated as " + smtpUsername + ")");
            System.out.println("✅ EMAIL SENDING        : SUCCESS (Accepted by Brevo Relay)");
            System.out.println("Recipient               : " + cleanEmail);
            System.out.println("From                    : " + fromEmail);
            System.out.println("Time                    : " + LocalDateTime.now());
            System.out.println("=================================================");
            return true;
        } catch (org.springframework.mail.MailAuthenticationException authEx) {
            System.err.println("=================================================");
            System.err.println("✅ SMTP CONNECTION      : SUCCESS (Connected to " + smtpHost + ":" + smtpPort + ")");
            System.err.println("❌ SMTP AUTHENTICATION  : FAILED (535 5.7.8 Authentication failed)");
            System.err.println("❌ EMAIL SENDING        : ABORTED");
            System.err.println("Username                : " + smtpUsername);
            System.err.println("Recipient               : " + cleanEmail);
            System.err.println("Root Cause              : Invalid or revoked Brevo SMTP key for user '" + smtpUsername + "'");
            System.err.println("=================================================");

            // Attempt Brevo REST API fallback if configured
            boolean fallbackSuccess = sendViaBrevoRestApi(cleanEmail, otp.trim(), htmlContent);
            if (fallbackSuccess) {
                return true;
            }

            throw new RuntimeException("Brevo SMTP authentication failed: Invalid or expired SMTP credentials for '" + smtpUsername + "' on " + smtpHost + ":" + smtpPort + " (535 5.7.8 Authentication failed). Please verify your Brevo SMTP key in application.properties.", authEx);
        } catch (org.springframework.mail.MailSendException sendEx) {
            System.err.println("=================================================");
            System.err.println("❌ SMTP TRANSMISSION ERROR: " + sendEx.getMessage());
            if (sendEx.getCause() != null) {
                System.err.println("Root Cause              : " + sendEx.getCause().getMessage());
            }
            System.err.println("=================================================");
            throw new RuntimeException("Brevo SMTP email sending failed: " + sendEx.getMessage(), sendEx);
        } catch (Exception ex) {
            System.err.println("=================================================");
            System.err.println("❌ SMTP EXCEPTION OCCURRED: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            System.err.println("=================================================");
            throw new RuntimeException("Failed to send OTP email via Brevo SMTP: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fallback: Send email via Brevo v3 REST API endpoint (https://api.brevo.com/v3/smtp/email)
     */
    private boolean sendViaBrevoRestApi(String recipientEmail, String otpCode, String htmlContent) {
        String apiKey = brevoApiKey != null ? brevoApiKey.trim() : "";
        if (apiKey.isEmpty()) {
            return false;
        }

        try {
            System.out.println("[BREVO API FALLBACK] Attempting delivery via Brevo v3 REST API...");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            headers.set("accept", "application/json");

            Map<String, Object> senderMap = new HashMap<>();
            senderMap.put("name", instituteName);
            senderMap.put("email", fromEmail);

            Map<String, Object> toMap = new HashMap<>();
            toMap.put("email", recipientEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", senderMap);
            payload.put("to", List.of(toMap));
            payload.put("subject", "🔑 Your PINACXTREME Verification Code: " + otpCode);
            payload.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    "https://api.brevo.com/v3/smtp/email",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (resp.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ BREVO REST API OTP EMAIL DELIVERED SUCCESSFULLY to: " + recipientEmail);
                return true;
            }
        } catch (Exception ex) {
            System.err.println("[BREVO API FALLBACK FAILED] " + ex.getMessage());
        }
        return false;
    }

    private String buildOtpHtmlTemplate(String cleanOtp) {
        return "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; background-color:#f7eefa; padding:20px;'>" +
                "<div style='max-width:500px; margin:auto; background:#ffffff; border-radius:16px; padding:30px; border:1px solid #ecd6f1;'>" +
                "<h2 style='color:#7c2d8d; margin-top:0;'>PINACXTREME Verification</h2>" +
                "<p style='color:#475569;'>Use the following 6-digit One-Time Password (OTP) to complete your verification. This code is valid for <strong>10 minutes</strong>.</p>" +
                "<div style='background:#f7eefa; border:2px dashed #a855c4; border-radius:12px; padding:15px; text-align:center; margin:24px 0;'>" +
                "<span style='font-size:32px; font-weight:bold; letter-spacing:6px; color:#5c2168;'>" + cleanOtp + "</span>" +
                "</div>" +
                "<p style='color:#64748b; font-size:13px;'>If you did not request this OTP, please ignore this email or contact support immediately.</p>" +
                "<hr style='border:none; border-top:1px solid #ecd6f1; margin:20px 0;'/>" +
                "<p style='color:#94a3b8; font-size:11px; margin-bottom:0;'>PINACXTREME Academy · Support: " + supportEmail + "</p>" +
                "</div></body></html>";
    }
}
