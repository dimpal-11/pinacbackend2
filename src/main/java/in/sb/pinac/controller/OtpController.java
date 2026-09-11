package in.sb.pinac.controller;

import in.sb.pinac.service.BrevoSmsService;
import in.sb.pinac.service.EmailService;
import in.sb.pinac.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/otp", "/api/otp"})
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private BrevoSmsService brevoSmsService;

    @Autowired
    private EmailService emailService;

    // SEND OTP (accepts mobile or email via query params or request body)
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email,
            @RequestBody(required = false) Map<String, String> body) {

        String targetMobile = mobile != null ? mobile : (body != null ? body.get("mobile") : null);
        String targetEmail = email != null ? email : (body != null ? body.get("email") : null);

        if ((targetMobile == null || targetMobile.trim().isEmpty()) &&
            (targetEmail == null || targetEmail.trim().isEmpty())) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Mobile number or email is required");
            return ResponseEntity.badRequest().body(error);
        }

        String otp = otpService.generateAndSaveOtp(targetMobile, targetEmail);

        if (targetMobile != null && !targetMobile.trim().isEmpty()) {
            brevoSmsService.sendOtpSms(targetMobile.trim(), otp);
        }
        if (targetEmail != null && !targetEmail.trim().isEmpty()) {
            emailService.sendOtpEmail(targetEmail.trim(), otp);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OTP sent successfully");
        return ResponseEntity.ok(response);
    }

    // VERIFY OTP
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String otp,
            @RequestBody(required = false) Map<String, String> body) {

        String targetMobile = mobile != null ? mobile : (body != null ? body.get("mobile") : null);
        String targetEmail = email != null ? email : (body != null ? body.get("email") : null);
        String targetOtp = otp != null ? otp : (body != null ? body.get("otp") : null);

        Map<String, Object> response = new HashMap<>();

        if (targetOtp == null || targetOtp.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "OTP is required");
            return ResponseEntity.badRequest().body(response);
        }

        boolean verified = otpService.verifyOtp(targetMobile, targetEmail, targetOtp);
        if (!verified) {
            response.put("success", false);
            response.put("message", "Invalid or expired OTP. Please check and try again.");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("message", "OTP verified successfully");
        return ResponseEntity.ok(response);
    }
}
