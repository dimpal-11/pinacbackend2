package in.sb.pinac.controller.admin;

import in.sb.pinac.dto.admin.AdminCourseRequest;
import in.sb.pinac.entity.Course;
import in.sb.pinac.service.admin.AdminCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    @Autowired
    private AdminCourseService adminCourseService;

    // GET ALL COURSES
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminCourseService.getAllCourses(search, category, status));
    }

    // GET COURSE BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminCourseService.getCourseById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // CREATE COURSE
    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody AdminCourseRequest request) {
        try {
            Course saved = adminCourseService.createCourse(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // UPDATE COURSE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @RequestBody AdminCourseRequest request) {
        try {
            Course updated = adminCourseService.updateCourse(id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // UPDATE COURSE STATUS (PUBLISHED, DRAFT, HIDDEN)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateCourseStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            Course updated = adminCourseService.updateCourseStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // DELETE COURSE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            adminCourseService.deleteCourse(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Course deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
