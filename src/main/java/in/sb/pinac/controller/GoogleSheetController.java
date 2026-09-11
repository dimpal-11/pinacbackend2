package in.sb.pinac.controller;

import in.sb.pinac.dto.EnrollmentGoogleDto;
import in.sb.pinac.dto.GoogleSheetAddRequestDto;
import in.sb.pinac.dto.PaymentGoogleDto;
import in.sb.pinac.dto.StudentGoogleDto;
import in.sb.pinac.service.GoogleSheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/google", "/api/sheets", "/api/google-sheet"})
public class GoogleSheetController {

    @Autowired
    private GoogleSheetService googleSheetService;

    /**
     * Directly add/append a row to Google Sheets
     * POST /api/google-sheet/add
     */
    @PostMapping("/add")
    public ResponseEntity<?> addRow(@RequestBody GoogleSheetAddRequestDto dto) {
        Map<String, Object> response = googleSheetService.addRowFromDto(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * 1. Append new student signup row to Google Sheets
     * POST /api/google/student
     */
    @PostMapping("/student")
    public ResponseEntity<?> appendStudent(@RequestBody StudentGoogleDto dto) {
        Map<String, Object> response = googleSheetService.appendStudent(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Append new payment row to Google Sheets after successful payment
     * POST /api/google/payment
     */
    @PostMapping("/payment")
    public ResponseEntity<?> appendPayment(@RequestBody PaymentGoogleDto dto) {
        Map<String, Object> response = googleSheetService.appendPayment(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Append course enrollment row to Google Sheets
     * POST /api/google/enrollment
     */
    @PostMapping("/enrollment")
    public ResponseEntity<?> appendEnrollment(@RequestBody EnrollmentGoogleDto dto) {
        Map<String, Object> response = googleSheetService.appendEnrollment(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. Check Google Sheets Service Account & API connection health
     * GET /api/google/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = googleSheetService.checkStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 5. Trigger sync of all database records to Google Sheet
     * POST /api/google-sheet/sync or GET /api/google-sheet/sync
     */
    @RequestMapping(value = {"/sync", "/sync-all"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> syncAll() {
        Map<String, Object> result = googleSheetService.syncAllFromDatabase();
        return ResponseEntity.ok(result);
    }
}
