package in.sb.pinac.controller;

import in.sb.pinac.entity.User;
import in.sb.pinac.service.AuthService;
import in.sb.pinac.service.BrevoSmsService;
import in.sb.pinac.service.EmailService;
import in.sb.pinac.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private BrevoSmsService brevoSmsService;

    @Autowired
    private EmailService emailService;

    // =========================================================
    // 1. SEND OTP
    // =========================================================
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return badRequest("Request body is required.");
        }

        String target = body.get("target");
        String mobile = body.get("mobile");
        String email = body.get("email");

        // If target is a Student ID (e.g. PINAC-STU-1), resolve to student's registered mobile & email
        if (target != null && !target.trim().isEmpty() && !target.contains("@")) {
            String digits = target.replaceAll("\\D", "").trim();
            if (digits.length() != 10) {
                Optional<User> uOpt = authService.findUserByIdentifier(target);
                if (uOpt.isPresent()) {
                    User u = uOpt.get();
                    System.out.println("[AUTH SEND-OTP] Resolved student ID '" + target + "' to email: " + u.getEmail() + ", mobile: " + u.getMobile());
                    if (mobile == null || mobile.trim().isEmpty()) {
                        mobile = u.getMobile();
                    }
                    if (email == null || email.trim().isEmpty()) {
                        email = u.getEmail();
                    }
                    target = null;
                }
            }
        }

        String[] resolved = OtpService.resolveTarget(target, mobile, email);
        String cleanMobile = resolved[0];
        String cleanEmail = resolved[1];

        if (cleanMobile == null && cleanEmail == null) {
            return badRequest("Mobile number or email is required.");
        }

        try {
            String otp = otpService.generateAndSaveOtp(cleanMobile, cleanEmail);

            // 1. Dispatch OTP to mobile number via Brevo Transactional SMS
            if (cleanMobile != null && !cleanMobile.isEmpty()) {
                try {
                    brevoSmsService.sendOtpSms(cleanMobile, otp);
                } catch (Exception smsEx) {
                    System.err.println("[SMS NOTICE] " + smsEx.getMessage());
                }
            }

            // 2. Dispatch OTP to email via Brevo SMTP (synchronous delivery check)
            if (cleanEmail != null && !cleanEmail.isEmpty()) {
                emailService.sendOtpEmail(cleanEmail, otp);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP email sent successfully to " + cleanEmail + ". Please check your inbox or spam folder.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[AUTH SEND-OTP ERROR] " + e.getMessage());
            return badRequest(e.getMessage() != null ? e.getMessage() : "Failed to send OTP.");
        }
    }

    // =========================================================
    // 2. VERIFY OTP
    // =========================================================
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return badRequest("Request body is required.");
        }

        String target = body.get("target");
        String mobile = body.get("mobile");
        String email = body.get("email");
        String otp = body.get("otp");

        if (otp == null || otp.trim().isEmpty()) {
            return badRequest("OTP is required.");
        }

        String[] resolved = OtpService.resolveTarget(target, mobile, email);
        String cleanMobile = resolved[0];
        String cleanEmail = resolved[1];

        if (cleanMobile == null && cleanEmail == null) {
            return badRequest("Email or mobile number is required.");
        }

        boolean verified = otpService.verifyOtp(cleanMobile, cleanEmail, otp.trim());
        if (!verified) {
            return badRequest("Invalid or expired OTP. Please try again.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OTP verified successfully");
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 3. SIGNUP (With City & Multi-Channel Support)
    // =========================================================
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return badRequest("Request body is required.");
        }

        String name = body.get("name");
        String email = body.get("email");
        String mobile = body.get("mobile");
        String city = body.get("city");
        String password = body.get("password");
        String otp = body.get("otp");

        if (name == null || name.trim().isEmpty()) {
            return badRequest("Full name is required.");
        }

        if (password == null || password.trim().length() < 6) {
            return badRequest("Password must be at least 6 characters.");
        }

        if (otp == null || otp.trim().isEmpty()) {
            return badRequest("OTP is required.");
        }

        String cleanEmail = OtpService.normalizeEmail(email);
        String cleanMobile = OtpService.normalizeMobile(mobile);

        if (cleanEmail == null && cleanMobile == null) {
            return badRequest("Email or mobile number is required.");
        }

        System.out.println("========================================");
        System.out.println("[AUTH SIGNUP] Request received for: " + name.trim());
        System.out.println("Email  : " + cleanEmail);
        System.out.println("Mobile : " + cleanMobile);
        System.out.println("OTP    : " + otp.trim());
        System.out.println("========================================");

        // Verify that OTP was verified or is valid for this target
        boolean isVerified = otpService.isTargetVerifiedRecently(cleanMobile, cleanEmail, otp.trim());
        if (!isVerified) {
            System.out.println("[AUTH SIGNUP REJECTED] Invalid or expired OTP for " + cleanEmail + " / " + cleanMobile);
            return badRequest("Invalid or expired OTP. Please request a new OTP.");
        }

        String courseName = body.get("courseName");
        String courseId = body.get("courseId");
        Double coursePrice = null;
        if (body.get("coursePrice") != null) {
            try {
                coursePrice = Double.valueOf(body.get("coursePrice"));
            } catch (Exception ignored) {
            }
        }

        String rawRole = body.get("role");
        String role = "STUDENT";
        if (rawRole != null && "ADMIN".equalsIgnoreCase(rawRole.trim())) {
            role = "ADMIN";
        }

        try {
            Map<String, Object> regResult = authService.registerUser(
                    name.trim(),
                    cleanEmail,
                    cleanMobile,
                    city != null ? city.trim() : null,
                    password.trim(),
                    role,
                    courseName,
                    courseId,
                    coursePrice
            );
            System.out.println("[AUTH SIGNUP SUCCESS] User registered: " + cleanEmail + " (" + regResult.get("studentId") + ")");
            return ResponseEntity.ok(regResult);
        } catch (Exception e) {
            System.err.println("[AUTH SIGNUP ERROR] " + e.getMessage());
            return badRequest(e.getMessage() != null ? e.getMessage() : "Registration failed.");
        }
    }

    // =========================================================
    // 4. LOGIN
    // =========================================================
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return badRequest("Request body is required.");
        }

        String identifier = body.get("identifier");
        String email = body.get("email");
        String mobile = body.get("mobile");
        String password = body.get("password");
        String otp = body.get("otp");

        String target = identifier != null && !identifier.trim().isEmpty()
                ? identifier
                : (email != null && !email.trim().isEmpty() ? email : mobile);

        if (target == null || target.trim().isEmpty()) {
            return badRequest("Email, mobile, or Student ID is required.");
        }

        System.out.println("========================================");
        System.out.println("[AUTH LOGIN] Request received for target: " + target.trim());
        System.out.println("Auth Mode: " + (otp != null && !otp.trim().isEmpty() ? "OTP" : "PASSWORD"));
        System.out.println("========================================");

        try {
            Map<String, Object> result = authService.login(target.trim(), password, otp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("[AUTH LOGIN REJECTED] " + e.getMessage());
            return badRequest(e.getMessage() != null ? e.getMessage() : "Authentication failed.");
        }
    }

    // =========================================================
    // 5. RESET PASSWORD
    // =========================================================
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return badRequest("Request body is required.");
        }

        String identifier = body.get("identifier");
        String newPassword = body.get("newPassword");
        String otp = body.get("otp");

        try {
            Map<String, Object> result = authService.resetPassword(identifier, newPassword, otp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return badRequest(e.getMessage() != null ? e.getMessage() : "Password reset failed.");
        }
    }

    // =========================================================
    // 6. CURRENT USER
    // =========================================================
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(err);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("user", authService.sanitizeUser(user));
        return ResponseEntity.ok(res);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", message);
        return ResponseEntity.badRequest().body(err);
    }
}