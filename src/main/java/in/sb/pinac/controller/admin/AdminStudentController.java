package in.sb.pinac.controller.admin;

import in.sb.pinac.dto.admin.AdminStudentUpdateRequest;
import in.sb.pinac.service.admin.AdminStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentController {

    @Autowired
    private AdminStudentService adminStudentService;

    // GET ALL STUDENTS
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminStudentService.getAllStudents(search, status));
    }

    // GET SINGLE STUDENT DETAILS (With purchased courses & payments)
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminStudentService.getStudentDetails(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // UPDATE STUDENT PROFILE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(
            @PathVariable Long id,
            @RequestBody AdminStudentUpdateRequest request) {
        try {
            return ResponseEntity.ok(adminStudentService.updateStudent(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // TOGGLE ACTIVE STATUS
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatusPatch(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminStudentService.toggleStatus(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatusPut(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminStudentService.toggleStatus(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // DELETE STUDENT
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            adminStudentService.deleteStudent(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Student deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
