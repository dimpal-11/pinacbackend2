package in.sb.pinac.controller.admin;

import in.sb.pinac.dto.admin.AttendanceBulkMarkRequest;
import in.sb.pinac.dto.admin.AttendanceRecordDto;
import in.sb.pinac.service.admin.AdminAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/attendance")
public class AdminAttendanceController {

    @Autowired
    private AdminAttendanceService adminAttendanceService;

    // GET ATTENDANCE RECORDS WITH FILTERS
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminAttendanceService.getAttendanceRecords(date, courseId, status, batchName, search));
    }

    // GET STATS
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAttendanceStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminAttendanceService.getAttendanceStats(date));
    }

    // BULK SAVE / MARK ATTENDANCE
    @PostMapping("/mark-bulk")
    public ResponseEntity<?> markBulkAttendance(@RequestBody AttendanceBulkMarkRequest request) {
        try {
            return ResponseEntity.ok(adminAttendanceService.markBulkAttendance(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // CREATE MANUAL RECORD
    @PostMapping
    public ResponseEntity<?> createAttendance(@RequestBody AttendanceRecordDto dto) {
        try {
            return ResponseEntity.ok(adminAttendanceService.createAttendance(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // UPDATE RECORD
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAttendance(
            @PathVariable Long id,
            @RequestBody AttendanceRecordDto dto) {
        try {
            return ResponseEntity.ok(adminAttendanceService.updateAttendance(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // DELETE RECORD
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Long id) {
        try {
            adminAttendanceService.deleteAttendance(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Attendance record deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // GET STUDENT ATTENDANCE OVERVIEW / SUMMARY
    @GetMapping("/summary")
    public ResponseEntity<List<Map<String, Object>>> getStudentSummary() {
        return ResponseEntity.ok(adminAttendanceService.getStudentAttendanceSummary());
    }
}
