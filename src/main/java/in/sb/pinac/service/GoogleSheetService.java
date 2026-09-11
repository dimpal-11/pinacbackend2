package in.sb.pinac.service;

import in.sb.pinac.dto.EnrollmentGoogleDto;
import in.sb.pinac.dto.EnrollmentSheetDTO;
import in.sb.pinac.dto.PaymentGoogleDto;
import in.sb.pinac.dto.PaymentSheetDTO;
import in.sb.pinac.dto.StudentGoogleDto;
import in.sb.pinac.dto.StudentSheetDTO;
import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.Payment;
import in.sb.pinac.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Backward-compatible service delegating to the dedicated GoogleSheetsService.
 * Ensures zero regressions for existing controllers and admin services.
 */
@Service
public class GoogleSheetService {

    public static final List<Object> HEADERS_12 = GoogleSheetsService.HEADERS_12;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    public void syncRegistrationToSheet(User student, String courseName, String courseId, Double coursePrice) {
        googleSheetsService.syncRegistrationToSheet(student, courseName, courseId, coursePrice);
    }

    public void syncPaymentToSheet(User student, Course course, Payment payment, Enrollment enrollment) {
        googleSheetsService.syncPaymentToSheet(student, course, payment, enrollment);
    }

    public void syncUserFromDatabase(Long userId) {
        googleSheetsService.syncUserFromDatabase(userId);
    }

    public void syncPaymentFromDatabase(Long paymentId) {
        googleSheetsService.syncPaymentFromDatabase(paymentId);
    }

    public Map<String, Object> syncAllFromDatabase() {
        return googleSheetsService.syncAllFromDatabase();
    }

    public Map<String, Object> checkStatus() {
        return googleSheetsService.checkStatus();
    }

    public void appendStudent(StudentSheetDTO dto) {
        googleSheetsService.appendStudent(dto);
    }

    public void updatePayment(PaymentSheetDTO dto) {
        googleSheetsService.updatePayment(dto);
    }

    public void appendEnrollment(EnrollmentSheetDTO dto) {
        googleSheetsService.appendEnrollment(dto);
    }

    public Map<String, Object> appendStudent(StudentGoogleDto dto) {
        return googleSheetsService.appendStudent(dto);
    }

    public Map<String, Object> appendPayment(PaymentGoogleDto dto) {
        return googleSheetsService.appendPayment(dto);
    }

    public Map<String, Object> appendEnrollment(EnrollmentGoogleDto dto) {
        return googleSheetsService.appendEnrollment(dto);
    }

    public Map<String, Object> addRowFromDto(in.sb.pinac.dto.GoogleSheetAddRequestDto dto) {
        return googleSheetsService.addRowFromDto(dto);
    }
}
