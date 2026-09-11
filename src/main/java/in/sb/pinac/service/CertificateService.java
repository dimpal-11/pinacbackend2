package in.sb.pinac.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import in.sb.pinac.entity.Certificate;
import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.CertificateRepository;
import in.sb.pinac.repository.EnrollmentRepository;
import in.sb.pinac.repository.LessonProgressRepository;
import in.sb.pinac.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    /**
     * Award a verified certificate to student for a completed course.
     * Idempotent: If certificate already exists, returns existing certificate.
     */
    @Transactional
    public Certificate awardCertificate(User user, Course course) {
        if (user == null || course == null) {
            throw new IllegalArgumentException("User and Course are required to award a certificate.");
        }

        // 1. Check if certificate already exists (prevent duplicate certificates)
        Optional<Certificate> existing = certificateRepository.findByUserIdAndCourseId(user.getId(), course.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Generate unique Certificate Code: PINAC-2026-XXXXXX
        String certCode = "PINAC-2026-" + String.format("%06d", (int)(Math.random() * 900000) + 100000);
        while (certificateRepository.findByCertificateCode(certCode).isPresent()) {
            certCode = "PINAC-2026-" + String.format("%06d", (int)(Math.random() * 900000) + 100000);
        }

        LocalDateTime now = LocalDateTime.now();

        Certificate cert = new Certificate();
        cert.setUser(user);
        cert.setCourse(course);
        cert.setStudentName(user.getName() != null && !user.getName().trim().isEmpty() ? user.getName().trim() : "PINAC Graduate");
        cert.setCourseTitle(course.getTitle() != null ? course.getTitle() : "Professional Masterclass");
        cert.setCertificateCode(certCode);
        cert.setCompletionDate(now);
        cert.setIssueDate(now);
        cert.setVerificationUrl("http://localhost:5173/verify-certificate?code=" + certCode);

        Certificate saved = certificateRepository.save(cert);

        // Update enrollment status to 100% completed
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId()).orElse(null);
        if (enrollment != null) {
            enrollment.setProgressPercentage(100);
            long totalLessons = lessonRepository.countByCourseId(course.getId());
            enrollment.setCompletedLessonsCount((int) totalLessons);
            enrollmentRepository.save(enrollment);
        }

        System.out.println("=================================================");
        System.out.println("🎓 PINAC VERIFIED CERTIFICATE GENERATED");
        System.out.println("Student Name   : " + saved.getStudentName());
        System.out.println("Course Title   : " + saved.getCourseTitle());
        System.out.println("Certificate ID : " + saved.getCertificateCode());
        System.out.println("Completion Date: " + now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        System.out.println("=================================================");

        return saved;
    }

    /**
     * Verify if student has achieved 100% course completion.
     */
    public boolean isCourseFullyCompleted(Long userId, Long courseId) {
        long totalLessons = lessonRepository.countByCourseId(courseId);
        if (totalLessons == 0) {
            // Check fallback enrollment percentage
            Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
            return enrollment != null && enrollment.getProgressPercentage() != null && enrollment.getProgressPercentage() >= 100;
        }

        long completedLessons = lessonProgressRepository.countCompletedLessonsByUserIdAndCourseId(userId, courseId);
        return completedLessons >= totalLessons;
    }

    public List<Certificate> getUserCertificates(Long userId) {
        return certificateRepository.findByUserIdOrderByIssueDateDesc(userId);
    }

    public Optional<Certificate> getCertificateById(Long certificateId) {
        return certificateRepository.findById(certificateId);
    }

    public Optional<Certificate> getCertificateByCode(String code) {
        return certificateRepository.findByCertificateCode(code);
    }

    public Optional<Certificate> getCertificateByUserAndCourse(Long userId, Long courseId) {
        return certificateRepository.findByUserIdAndCourseId(userId, courseId);
    }

    /**
     * Generate high-quality landscape PDF certificate.
     */
    public byte[] generateCertificatePdf(Certificate cert) {
        // Landscape A4 (842 x 595 points)
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Color Palette
            Color brandPurple = new Color(124, 45, 141);     // #7C2D8D
            Color darkPurple = new Color(88, 28, 102);       // #581C66
            Color goldAccent = new Color(197, 148, 48);      // #C59430
            Color lightGold = new Color(245, 238, 220);      // #F5EEDC
            Color darkText = new Color(30, 30, 35);          // Charcoal
            Color slateMuted = new Color(100, 105, 115);     // Muted Gray
            Color bgCream = new Color(254, 253, 255);        // Clean Background

            // Outer Container Table
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);

            PdfPCell mainCell = new PdfPCell();
            mainCell.setBorder(Rectangle.BOX);
            mainCell.setBorderWidth(3.5f);
            mainCell.setBorderColor(brandPurple);
            mainCell.setBackgroundColor(bgCream);
            mainCell.setPadding(26);

            // Inner Decorative Border
            PdfPTable innerTable = new PdfPTable(1);
            innerTable.setWidthPercentage(100);

            PdfPCell innerCell = new PdfPCell();
            innerCell.setBorder(Rectangle.BOX);
            innerCell.setBorderWidth(1f);
            innerCell.setBorderColor(goldAccent);
            innerCell.setPadding(20);
            innerCell.setBackgroundColor(Color.WHITE);

            // 1. Organization Header
            Paragraph orgName = new Paragraph("PINAC INSTITUTE OF CREATIVE MEDIA & TECHNOLOGY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, goldAccent));
            orgName.setAlignment(Element.ALIGN_CENTER);
            innerCell.addElement(orgName);

            Paragraph orgSub = new Paragraph("ISO 9001:2015 CERTIFIED INSTITUTION • RECOGNIZED CENTER OF EXCELLENCE",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, slateMuted));
            orgSub.setAlignment(Element.ALIGN_CENTER);
            orgSub.setSpacingBefore(3f);
            innerCell.addElement(orgSub);

            // 2. Certificate Title
            Paragraph title = new Paragraph("CERTIFICATE OF COMPLETION",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, brandPurple));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(12f);
            innerCell.addElement(title);

            // 3. Subtitle / Presentation line
            Paragraph presentedTo = new Paragraph("THIS IS PROUDLY PRESENTED TO",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, slateMuted));
            presentedTo.setAlignment(Element.ALIGN_CENTER);
            presentedTo.setSpacingBefore(8f);
            innerCell.addElement(presentedTo);

            // 4. Student Name (Large & Prominent)
            Paragraph studentName = new Paragraph(cert.getStudentName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, darkPurple));
            studentName.setAlignment(Element.ALIGN_CENTER);
            studentName.setSpacingBefore(8f);
            innerCell.addElement(studentName);

            // 5. Completion Description
            Paragraph description = new Paragraph(
                    "for outstanding performance and successful 100% completion of all comprehensive video masterclasses,\n" +
                    "practical assessments, industry projects, and portfolio requirements for",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, darkText)
            );
            description.setAlignment(Element.ALIGN_CENTER);
            description.setSpacingBefore(8f);
            innerCell.addElement(description);

            // 6. Course Title (Bold Purple)
            Paragraph courseTitle = new Paragraph(cert.getCourseTitle(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, brandPurple));
            courseTitle.setAlignment(Element.ALIGN_CENTER);
            courseTitle.setSpacingBefore(6f);
            innerCell.addElement(courseTitle);

            // 7. Bottom Metadata Table (Date | Certificate ID | Signature)
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(20f);
            footerTable.setWidths(new float[]{2.5f, 3.5f, 2.5f});

            // Date
            String issueDateStr = cert.getIssueDate() != null
                    ? cert.getIssueDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.addElement(new Paragraph("Issue Date:", FontFactory.getFont(FontFactory.HELVETICA, 8, slateMuted)));
            dateCell.addElement(new Paragraph(issueDateStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkText)));
            dateCell.addElement(new Paragraph("Status: Verified & Active", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(16, 149, 83))));
            footerTable.addCell(dateCell);

            // Certificate Code & Seal
            PdfPCell idCell = new PdfPCell();
            idCell.setBorder(Rectangle.NO_BORDER);
            idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph idHeading = new Paragraph("CERTIFICATE IDENTIFIER", FontFactory.getFont(FontFactory.HELVETICA, 8, slateMuted));
            idHeading.setAlignment(Element.ALIGN_CENTER);
            idCell.addElement(idHeading);

            Paragraph codePara = new Paragraph(cert.getCertificateCode(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, brandPurple));
            codePara.setAlignment(Element.ALIGN_CENTER);
            idCell.addElement(codePara);

            Paragraph verifyNote = new Paragraph("Verify: pinacxtreme.com/verify", FontFactory.getFont(FontFactory.HELVETICA, 7, slateMuted));
            verifyNote.setAlignment(Element.ALIGN_CENTER);
            idCell.addElement(verifyNote);
            footerTable.addCell(idCell);

            // Signature Block
            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(Rectangle.NO_BORDER);
            signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph signLine = new Paragraph("_______________________", FontFactory.getFont(FontFactory.HELVETICA, 9, slateMuted));
            signLine.setAlignment(Element.ALIGN_RIGHT);
            signCell.addElement(signLine);

            Paragraph authHead = new Paragraph("Academic Director", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, darkText));
            authHead.setAlignment(Element.ALIGN_RIGHT);
            signCell.addElement(authHead);

            Paragraph orgFoot = new Paragraph("PINAC Institute", FontFactory.getFont(FontFactory.HELVETICA, 8, brandPurple));
            orgFoot.setAlignment(Element.ALIGN_RIGHT);
            signCell.addElement(orgFoot);
            footerTable.addCell(signCell);

            innerCell.addElement(footerTable);
            innerTable.addCell(innerCell);
            mainCell.addElement(innerTable);
            outerTable.addCell(mainCell);

            document.add(outerTable);
            document.close();
        } catch (Exception e) {
            System.err.println("Error rendering Certificate PDF: " + e.getMessage());
        }

        return out.toByteArray();
    }
}
