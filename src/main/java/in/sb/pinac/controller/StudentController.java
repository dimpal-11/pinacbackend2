package in.sb.pinac.controller;

import in.sb.pinac.entity.*;
import in.sb.pinac.repository.*;
import in.sb.pinac.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // STUDENT DASHBOARD AGGREGATE ENDPOINT
    @GetMapping("/dashboard")
    public ResponseEntity<?> getStudentDashboard(@AuthenticationPrincipal User authUser,
                                                @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        if (targetId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        User user = userRepository.findById(targetId).orElseThrow(() -> new RuntimeException("User not found: " + targetId));
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(targetId);
        List<Payment> payments = paymentRepository.findByUserId(targetId);
        List<Certificate> certificates = certificateRepository.findByUserId(targetId);

        int totalCoursesEnrolled = enrollments.size();
        long completedCourses = enrollments.stream().filter(e -> e.getProgressPercentage() != null && e.getProgressPercentage() >= 100).count();

        Map<String, Object> dashboard = new HashMap<>();
        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("id", user.getId());
        studentMap.put("studentId", user.getStudentId() != null ? user.getStudentId() : "PINAC-STU-" + user.getId());
        studentMap.put("name", user.getName() != null ? user.getName() : "");
        studentMap.put("email", user.getEmail() != null ? user.getEmail() : "");
        studentMap.put("mobile", user.getMobile() != null ? user.getMobile() : "");
        studentMap.put("city", user.getCity() != null ? user.getCity() : "");
        studentMap.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        studentMap.put("role", user.getRole() != null ? user.getRole() : "STUDENT");
        studentMap.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        dashboard.put("student", studentMap);

        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("totalEnrolled", totalCoursesEnrolled);
        statsMap.put("completedCourses", completedCourses);
        statsMap.put("inProgressCourses", totalCoursesEnrolled - completedCourses);
        statsMap.put("certificatesEarned", certificates.size());
        dashboard.put("stats", statsMap);

        dashboard.put("enrollments", enrollments);
        dashboard.put("payments", payments);
        dashboard.put("certificates", certificates);

        return ResponseEntity.ok(dashboard);
    }

    // GET ENROLLED COURSES
    @GetMapping("/courses")
    public ResponseEntity<?> getEnrolledCourses(@AuthenticationPrincipal User authUser,
                                                @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        if (targetId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        return ResponseEntity.ok(enrollmentRepository.findByUserId(targetId));
    }

    // GET COURSE PROGRESS FOR LMS PLAYER
    @GetMapping("/progress/{courseId}")
    public ResponseEntity<?> getCourseProgress(@PathVariable Long courseId,
                                               @AuthenticationPrincipal User authUser,
                                               @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        if (targetId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        return ResponseEntity.ok(progressService.getCourseProgress(targetId, courseId));
    }

    // UPDATE LESSON PROGRESS (MARK COMPLETE / SYNC PLAYHEAD)
    @PostMapping("/progress/update")
    public ResponseEntity<?> updateLessonProgress(@RequestBody Map<String, Object> body,
                                                  @AuthenticationPrincipal User authUser) {
        Long userId = authUser != null ? authUser.getId() : (body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null);
        Long courseId = Long.valueOf(body.get("courseId").toString());
        Long lessonId = Long.valueOf(body.get("lessonId").toString());
        Boolean completed = body.get("completed") != null ? Boolean.valueOf(body.get("completed").toString()) : null;
        Integer watchedSeconds = body.get("watchedSeconds") != null ? Integer.valueOf(body.get("watchedSeconds").toString()) : null;

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        Map<String, Object> progress = progressService.updateLessonProgress(userId, courseId, lessonId, completed, watchedSeconds);
        return ResponseEntity.ok(progress);
    }

    // GET STUDENT CERTIFICATES
    @GetMapping("/certificates")
    public ResponseEntity<?> getCertificates(@AuthenticationPrincipal User authUser,
                                             @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        if (targetId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        return ResponseEntity.ok(certificateRepository.findByUserId(targetId));
    }

    // DOWNLOAD CERTIFICATE PDF
    @GetMapping("/certificates/{id}/pdf")
    public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable Long id) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + id));

        byte[] pdfBytes = certificateService.generateCertificatePdf(cert);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PINAC_Certificate_" + cert.getCertificateCode() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // UPDATE PROFILE & PASSWORD
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal User authUser) {
        Long targetId = authUser != null ? authUser.getId() : (body.get("userId") != null ? Long.valueOf(body.get("userId")) : null);
        if (targetId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        User user = userRepository.findById(targetId).orElseThrow();

        if (body.get("name") != null && !body.get("name").trim().isEmpty()) {
            user.setName(body.get("name").trim());
        }
        if (body.get("mobile") != null && !body.get("mobile").trim().isEmpty()) {
            user.setMobile(body.get("mobile").trim());
        }
        if (body.get("city") != null) {
            user.setCity(body.get("city").trim());
        }
        if (body.get("avatar") != null) {
            user.setAvatar(body.get("avatar").trim());
        }
        if (body.get("newPassword") != null && !body.get("newPassword").trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(body.get("newPassword").trim()));
            user.setTempPassword(null);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile updated successfully",
                "user", Map.of(
                        "id", saved.getId(),
                        "studentId", saved.getStudentId(),
                        "name", saved.getName(),
                        "email", saved.getEmail(),
                        "mobile", saved.getMobile() != null ? saved.getMobile() : "",
                        "city", saved.getCity() != null ? saved.getCity() : "",
                        "avatar", saved.getAvatar() != null ? saved.getAvatar() : ""
                )
        ));
    }
}
