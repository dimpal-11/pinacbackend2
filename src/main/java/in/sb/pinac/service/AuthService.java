package in.sb.pinac.service;

import in.sb.pinac.config.JwtUtils;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @Autowired
    private EmailService emailService;

    /**
     * Register a new Student account with City and optional Course details.
     * Prevents duplicates and securely hashes passwords.
     * Automatically logs user registration row to Google Sheets immediately.
     */
    @Transactional
    public Map<String, Object> registerUser(String name, String email, String mobile, String city, String rawPassword, String role,
                                            String courseName, String courseId, Double coursePrice) {
        String cleanEmail = OtpService.normalizeEmail(email);
        String cleanMobile = OtpService.normalizeMobile(mobile);

        if (cleanEmail == null && cleanMobile == null) {
            throw new IllegalArgumentException("Email or mobile number is required.");
        }

        // Duplicate Account Checks
        if (cleanEmail != null && userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email already registered. Please log in.");
        }

        if (cleanMobile != null && userRepository.existsByMobile(cleanMobile)) {
            throw new IllegalArgumentException("Mobile number already registered. Please log in.");
        }

        // Password setup
        String initialPassword = (rawPassword != null && !rawPassword.trim().isEmpty())
                ? rawPassword.trim()
                : "Pinac@" + (1000 + new Random().nextInt(9000));

        // Generate official Account ID (e.g. PINAC-STU-1 for Students, PINAC-ADM-1 for Admins)
        String userRole = (role != null && !role.trim().isEmpty()) ? role.trim().toUpperCase() : "STUDENT";
        String prefix = "ADMIN".equals(userRole) ? "PINAC-ADM-" : "PINAC-STU-";
        long studentCount = "ADMIN".equals(userRole)
                ? userRepository.countByRole("ADMIN") + 1
                : userRepository.countByRole("STUDENT") + 1;
        String studentId = prefix + studentCount;
        while (userRepository.findByStudentId(studentId).isPresent()) {
            studentCount++;
            studentId = prefix + studentCount;
        }

        User newUser = new User();
        newUser.setName(name != null && !name.trim().isEmpty() ? name.trim() : "Student");
        newUser.setEmail(cleanEmail);
        newUser.setMobile(cleanMobile);
        newUser.setCity(city != null && !city.trim().isEmpty() ? city.trim() : null);
        newUser.setStudentId(studentId);
        newUser.setPasswordHash(passwordEncoder.encode(initialPassword));
        newUser.setTempPassword(initialPassword);
        newUser.setRole(role != null && !role.trim().isEmpty() ? role.toUpperCase() : "STUDENT");
        newUser.setActive(true);

        User saved = userRepository.save(newUser);
        String token = jwtUtils.generateToken(saved);

        // STEP 1: Immediately sync registration row to Google Sheets (12-column format)
        googleSheetsService.syncRegistrationToSheet(saved, courseName, courseId, coursePrice);

        // STEP 2: Send Automatic Registration Confirmation Email (with Student ID, NO password)
        emailService.sendRegistrationSuccessEmail(saved, courseName, coursePrice);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Account registered successfully");
        res.put("token", token);
        res.put("user", sanitizeUser(saved));
        res.put("generatedStudentId", saved.getStudentId());
        res.put("studentId", saved.getStudentId());
        res.put("invoiceNumber", "INV-PINAC-" + (89420 + saved.getId()));
        return res;
    }

    public Map<String, Object> registerUser(String name, String email, String mobile, String city, String rawPassword, String role) {
        return registerUser(name, email, mobile, city, rawPassword, role, null, null, null);
    }

    public Map<String, Object> registerUser(String name, String email, String mobile, String rawPassword, String role) {
        return registerUser(name, email, mobile, null, rawPassword, role, null, null, null);
    }

    /**
     * Find a user by email, 10-digit mobile, or Student ID (case-insensitive).
     */
    public Optional<User> findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        String target = identifier.trim();
        if (target.contains("@")) {
            return userRepository.findByEmail(OtpService.normalizeEmail(target));
        }
        String cleanMobile = OtpService.normalizeMobile(target);
        if (cleanMobile != null && cleanMobile.length() == 10) {
            Optional<User> byMobile = userRepository.findByMobile(cleanMobile);
            if (byMobile.isPresent()) {
                return byMobile;
            }
        }
        Optional<User> byStudentId = userRepository.findByStudentId(target);
        if (byStudentId.isPresent()) {
            return byStudentId;
        }
        return userRepository.findByStudentIdIgnoreCase(target);
    }

    /**
     * Login via Email + Password, Mobile + Password, or Identifier + OTP.
     */
    public Map<String, Object> login(String identifier, String rawPassword, String otp) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Email, mobile, or Student ID is required.");
        }

        Optional<User> userOpt = findUserByIdentifier(identifier);

        if (userOpt.isEmpty()) {
            System.out.println("[AUTH LOGIN] User not found for identifier: " + identifier.trim());
            throw new IllegalArgumentException("Account not found with identifier: " + identifier.trim());
        }

        User user = userOpt.get();

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("Account is deactivated. Please contact support.");
        }

        // 1. OTP-based Login
        if (otp != null && !otp.trim().isEmpty()) {
            boolean validOtp = otpService.verifyOtp(user.getMobile(), user.getEmail(), otp.trim());
            if (!validOtp) {
                System.out.println("[AUTH LOGIN] OTP login failed for user: " + user.getEmail() + " / " + user.getMobile());
                throw new IllegalArgumentException("Invalid or expired OTP. Please try again.");
            }
            // Consume OTP after successful login
            otpService.findLatestOtpRecord(user.getMobile(), user.getEmail()).ifPresent(e -> {
                e.setConsumed(true);
            });
            System.out.println("[AUTH LOGIN] OTP login successful for user: " + user.getEmail() + " (" + user.getStudentId() + ")");
        }
        // 2. Password-based Login
        else if (rawPassword != null && !rawPassword.trim().isEmpty()) {
            boolean matches = user.getPasswordHash() != null && passwordEncoder.matches(rawPassword.trim(), user.getPasswordHash());
            if (!matches && (user.getTempPassword() == null || !rawPassword.trim().equals(user.getTempPassword()))) {
                System.out.println("[AUTH LOGIN] Password mismatch for user: " + user.getEmail() + " (" + user.getStudentId() + ")");
                throw new IllegalArgumentException("Incorrect password. Please check and try again.");
            }
            System.out.println("[AUTH LOGIN] Password login successful for user: " + user.getEmail() + " (" + user.getStudentId() + ")");
        } else {
            throw new IllegalArgumentException("Password or OTP is required to log in.");
        }

        String token = jwtUtils.generateToken(user);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Login successful");
        res.put("token", token);
        res.put("user", sanitizeUser(user));
        return res;
    }

    /**
     * Reset Password after OTP verification.
     */
    @Transactional
    public Map<String, Object> resetPassword(String identifier, String newPassword, String otp) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Email or mobile number is required.");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP is required.");
        }

        String target = identifier.trim();
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
            throw new IllegalArgumentException("No user found with identifier: " + identifier);
        }

        User user = userOpt.get();

        boolean validOtp = otpService.verifyAndConsumeOtpForReset(user.getMobile(), user.getEmail(), otp.trim());
        if (!validOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP. Please request a new OTP.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        user.setTempPassword(null);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        String token = jwtUtils.generateToken(saved);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Password reset successfully. You can now log in.");
        res.put("token", token);
        res.put("user", sanitizeUser(saved));
        return res;
    }

    public Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("studentId", user.getStudentId() != null && !user.getStudentId().isEmpty() ? user.getStudentId() : ("PINAC-STU-" + user.getId()));
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("mobile", user.getMobile());
        map.put("city", user.getCity());
        map.put("role", user.getRole());
        map.put("avatar", user.getAvatar());
        map.put("active", user.getActive());
        map.put("createdAt", user.getCreatedAt());
        map.put("invoiceNumber", "INV-PINAC-" + (user.getId() != null ? (89420 + user.getId()) : "REG"));
        return map;
    }
}
