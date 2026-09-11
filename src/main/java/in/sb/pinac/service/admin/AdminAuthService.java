package in.sb.pinac.service.admin;

import in.sb.pinac.config.JwtUtils;
import in.sb.pinac.dto.admin.AdminLoginRequest;
import in.sb.pinac.dto.admin.AdminResetPasswordRequest;
import in.sb.pinac.dto.admin.AdminSignupRequest;
import in.sb.pinac.dto.admin.ChangePasswordRequest;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.UserRepository;
import in.sb.pinac.service.AuthService;
import in.sb.pinac.service.BrevoSmsService;
import in.sb.pinac.service.EmailService;
import in.sb.pinac.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private BrevoSmsService brevoSmsService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthService authService;

    /**
     * Admin Login supporting Email + Password OR Mobile + Password.
     * Enforces strict role check: role must be 'ADMIN'.
     */
    public Map<String, Object> login(AdminLoginRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = request.getEmail() != null && !request.getEmail().trim().isEmpty()
                    ? request.getEmail()
                    : request.getMobile();
        }

        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Email or mobile number is required.");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        String target = identifier.trim();
        Optional<User> userOpt = Optional.empty();

        if (target.contains("@")) {
            userOpt = userRepository.findByEmail(OtpService.normalizeEmail(target));
        } else {
            String cleanMobile = OtpService.normalizeMobile(target);
            if (cleanMobile != null && cleanMobile.length() == 10) {
                userOpt = userRepository.findByMobile(cleanMobile);
            }
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByStudentId(target);
            }
        }

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email/mobile or password.");
        }

        User user = userOpt.get();

        // Strict Role Check: Student cannot login to Admin Panel
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Access Denied. You are not authorized to access the Admin Panel.");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Your admin account has been deactivated. Please contact the super administrator.");
        }

        boolean passwordMatches = (user.getPasswordHash() != null && passwordEncoder.matches(request.getPassword().trim(), user.getPasswordHash()))
                || (user.getTempPassword() != null && request.getPassword().trim().equals(user.getTempPassword()));

        if (!passwordMatches) {
            throw new IllegalArgumentException("Invalid email/mobile or password.");
        }

        String token = jwtUtils.generateToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Admin login successful");
        response.put("token", token);
        response.put("user", sanitizeAdmin(user));
        return response;
    }

    /**
     * Admin Signup with OTP verification and BCrypt password encryption.
     */
    @Transactional
    public Map<String, Object> signup(AdminSignupRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        String cleanEmail = OtpService.normalizeEmail(request.getEmail());
        String cleanMobile = OtpService.normalizeMobile(request.getMobile());

        if (cleanEmail == null && cleanMobile == null) {
            throw new IllegalArgumentException("Email or mobile number is required.");
        }

        // Duplicate Check
        if (cleanEmail != null && userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email is already registered. Please login.");
        }

        if (cleanMobile != null && userRepository.existsByMobile(cleanMobile)) {
            throw new IllegalArgumentException("Mobile number is already registered. Please login.");
        }

        // Verify OTP if provided
        if (request.getOtp() != null && !request.getOtp().trim().isEmpty()) {
            boolean isOtpValid = otpService.isTargetVerifiedRecently(cleanMobile, cleanEmail, request.getOtp().trim())
                    || otpService.verifyOtp(cleanMobile, cleanEmail, request.getOtp().trim());
            if (!isOtpValid) {
                throw new IllegalArgumentException("Invalid or expired OTP. Please verify OTP first.");
            }
        }

        // Generate Admin ID
        long adminCount = userRepository.countByRole("ADMIN") + 1;
        String adminId = "PINAC-ADM-" + String.format("%04d", adminCount);
        while (userRepository.findByStudentId(adminId).isPresent()) {
            adminCount++;
            adminId = "PINAC-ADM-" + String.format("%04d", adminCount);
        }

        User newAdmin = new User();
        newAdmin.setName(request.getName().trim());
        newAdmin.setEmail(cleanEmail);
        newAdmin.setMobile(cleanMobile);
        newAdmin.setCity(request.getCity() != null ? request.getCity().trim() : "Nashik");
        newAdmin.setStudentId(adminId);
        newAdmin.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        newAdmin.setTempPassword(null);
        newAdmin.setRole("ADMIN");
        newAdmin.setActive(true);

        User saved = userRepository.save(newAdmin);
        String token = jwtUtils.generateToken(saved);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Admin account created successfully");
        response.put("token", token);
        response.put("user", sanitizeAdmin(saved));
        return response;
    }

    /**
     * Send OTP for Admin Forgot Password or Signup.
     */
    public Map<String, Object> sendOtp(String target, String mobile, String email) {
        String[] resolved = OtpService.resolveTarget(target, mobile, email);
        String cleanMobile = resolved[0];
        String cleanEmail = resolved[1];

        if (cleanMobile == null && cleanEmail == null) {
            throw new IllegalArgumentException("Mobile number or email is required.");
        }

        String otp = otpService.generateAndSaveOtp(cleanMobile, cleanEmail);

        if (cleanMobile != null && !cleanMobile.isEmpty()) {
            brevoSmsService.sendOtpSms(cleanMobile, otp);
        }
        if (cleanEmail != null && !cleanEmail.isEmpty()) {
            emailService.sendOtpEmail(cleanEmail, otp);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OTP sent successfully to " + (cleanMobile != null ? "+91 " + cleanMobile : cleanEmail));
        return response;
    }

    /**
     * Verify OTP.
     */
    public boolean verifyOtp(String target, String mobile, String email, String otp) {
        String[] resolved = OtpService.resolveTarget(target, mobile, email);
        return otpService.verifyOtp(resolved[0], resolved[1], otp != null ? otp.trim() : "");
    }

    /**
     * Reset Password for Admin with OTP.
     */
    @Transactional
    public Map<String, Object> resetPassword(AdminResetPasswordRequest request) {
        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new IllegalArgumentException("Email or mobile number is required.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            throw new IllegalArgumentException("OTP is required.");
        }

        String target = request.getIdentifier().trim();
        Optional<User> userOpt = Optional.empty();

        if (target.contains("@")) {
            userOpt = userRepository.findByEmail(OtpService.normalizeEmail(target));
        } else {
            String cleanMobile = OtpService.normalizeMobile(target);
            if (cleanMobile != null) {
                userOpt = userRepository.findByMobile(cleanMobile);
            }
        }

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("No admin account found with identifier: " + target);
        }

        User user = userOpt.get();
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Access Denied. You are not authorized to access the Admin Panel.");
        }

        boolean validOtp = otpService.verifyAndConsumeOtpForReset(user.getMobile(), user.getEmail(), request.getOtp().trim());
        if (!validOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP. Please request a new OTP.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setTempPassword(null);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        String token = jwtUtils.generateToken(saved);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset successfully. You can now login.");
        response.put("token", token);
        response.put("user", sanitizeAdmin(saved));
        return response;
    }

    /**
     * Authenticated Admin Change Password.
     */
    @Transactional
    public Map<String, Object> changePassword(User currentAdmin, ChangePasswordRequest request) {
        if (currentAdmin == null) {
            throw new SecurityException("Unauthorized");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Current password is required.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }

        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match.");
        }

        boolean currentMatches = (currentAdmin.getPasswordHash() != null && passwordEncoder.matches(request.getCurrentPassword().trim(), currentAdmin.getPasswordHash()))
                || (currentAdmin.getTempPassword() != null && request.getCurrentPassword().trim().equals(currentAdmin.getTempPassword()));

        if (!currentMatches) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        currentAdmin.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        currentAdmin.setTempPassword(null);
        currentAdmin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(currentAdmin);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password changed successfully.");
        return response;
    }

    /**
     * Authenticated Admin Update Profile.
     */
    @Transactional
    public Map<String, Object> updateProfile(User currentAdmin, Map<String, Object> body) {
        if (currentAdmin == null) {
            throw new SecurityException("Unauthorized");
        }
        if (body.containsKey("name") && body.get("name") != null) {
            currentAdmin.setName(String.valueOf(body.get("name")).trim());
        }
        if (body.containsKey("mobile") && body.get("mobile") != null) {
            currentAdmin.setMobile(String.valueOf(body.get("mobile")).trim());
        }
        if (body.containsKey("city") && body.get("city") != null) {
            currentAdmin.setCity(String.valueOf(body.get("city")).trim());
        }
        if (body.containsKey("avatar")) {
            currentAdmin.setAvatar(body.get("avatar") != null ? String.valueOf(body.get("avatar")).trim() : null);
        }
        currentAdmin.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(currentAdmin);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Admin profile updated successfully");
        res.put("user", sanitizeAdmin(saved));
        return res;
    }

    public Map<String, Object> sanitizeAdmin(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("studentId", user.getStudentId() != null ? user.getStudentId() : "PINAC-ADM-" + user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("mobile", user.getMobile());
        map.put("city", user.getCity());
        map.put("role", user.getRole());
        map.put("avatar", user.getAvatar());
        map.put("active", user.getActive());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }
}
