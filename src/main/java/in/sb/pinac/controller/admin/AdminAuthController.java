package in.sb.pinac.controller.admin;

import in.sb.pinac.dto.admin.AdminLoginRequest;
import in.sb.pinac.dto.admin.AdminResetPasswordRequest;
import in.sb.pinac.dto.admin.AdminSignupRequest;
import in.sb.pinac.dto.admin.ChangePasswordRequest;
import in.sb.pinac.entity.User;
import in.sb.pinac.service.admin.AdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private AdminAuthService adminAuthService;

    // 1. ADMIN LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest request) {
        try {
            Map<String, Object> result = adminAuthService.login(request);
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage()); // "Access Denied. You are not authorized to access the Admin Panel."
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Login failed. " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // 2. ADMIN SIGNUP
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AdminSignupRequest request) {
        try {
            Map<String, Object> result = adminAuthService.signup(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // 3. SEND OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        try {
            String target = body.get("target");
            String mobile = body.get("mobile");
            String email = body.get("email");
            Map<String, Object> result = adminAuthService.sendOtp(target, mobile, email);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // 4. VERIFY OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            String target = body.get("target");
            String mobile = body.get("mobile");
            String email = body.get("email");
            String otp = body.get("otp");
            boolean valid = adminAuthService.verifyOtp(target, mobile, email, otp);
            if (!valid) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid or expired OTP"));
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "OTP verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 5. FORGOT PASSWORD (Send OTP)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String target = body.get("identifier");
            if (target == null) target = body.get("target");
            String mobile = body.get("mobile");
            String email = body.get("email");
            Map<String, Object> result = adminAuthService.sendOtp(target, mobile, email);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 6. RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody AdminResetPasswordRequest request) {
        try {
            Map<String, Object> result = adminAuthService.resetPassword(request);
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 7. CHANGE PASSWORD (Authenticated)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User authAdmin,
                                           @RequestBody ChangePasswordRequest request) {
        try {
            Map<String, Object> result = adminAuthService.changePassword(authAdmin, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 8. GET CURRENT ADMIN
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@AuthenticationPrincipal User authAdmin) {
        if (authAdmin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Unauthorized"));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", adminAuthService.sanitizeAdmin(authAdmin)
        ));
    }

    // 9. UPDATE CURRENT ADMIN PROFILE
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal User authAdmin,
                                           @RequestBody Map<String, Object> body) {
        if (authAdmin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            Map<String, Object> result = adminAuthService.updateProfile(authAdmin, body);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
