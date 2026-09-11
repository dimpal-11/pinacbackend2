package in.sb.pinac.controller;

import in.sb.pinac.entity.Certificate;
import in.sb.pinac.entity.User;
import in.sb.pinac.service.CertificateService;
import in.sb.pinac.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/certificates", "/api/certificate"})
public class CertificateController {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private ProgressService progressService;

    /**
     * 1. GET ALL CERTIFICATES FOR CURRENT USER
     * GET /api/certificates/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyCertificates(@AuthenticationPrincipal User authUser,
                                              @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        if (targetId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required to view certificates."));
        }

        List<Certificate> certs = certificateService.getUserCertificates(targetId);
        return ResponseEntity.ok(certs);
    }

    /**
     * 2. GET SINGLE CERTIFICATE BY ID
     * GET /api/certificates/{certificateId}
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<?> getCertificateById(@PathVariable Long certificateId,
                                                @AuthenticationPrincipal User authUser,
                                                @RequestParam(required = false) Long userId) {
        Certificate cert = certificateService.getCertificateById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found with ID: " + certificateId));

        return ResponseEntity.ok(cert);
    }

    /**
     * 3. DOWNLOAD CERTIFICATE PDF
     * GET /api/certificates/{certificateId}/download
     */
    @GetMapping("/{certificateId}/download")
    public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable Long certificateId) {
        Certificate cert = certificateService.getCertificateById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found with ID: " + certificateId));

        byte[] pdfBytes = certificateService.generateCertificatePdf(cert);

        String filename = "PINAC_Certificate_" + cert.getCertificateCode() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * 4. PUBLIC VERIFICATION ENDPOINT
     * GET /api/certificates/verify/{certificateCode}
     */
    @GetMapping("/verify/{certificateCode}")
    public ResponseEntity<?> verifyCertificateByCode(@PathVariable String certificateCode) {
        Certificate cert = certificateService.getCertificateByCode(certificateCode.trim())
                .orElse(null);

        if (cert == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("valid", false);
            err.put("message", "No authentic certificate found with Code: " + certificateCode);
            return ResponseEntity.status(404).body(err);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("valid", true);
        res.put("certificateCode", cert.getCertificateCode());
        res.put("studentName", cert.getStudentName());
        res.put("courseTitle", cert.getCourseTitle());
        res.put("issueDate", cert.getIssueDate());
        res.put("completionDate", cert.getCompletionDate());
        res.put("status", "VERIFIED & AUTHENTIC");
        res.put("institution", "PINAC Institute of Creative Media & Technology");
        return ResponseEntity.ok(res);
    }
}
