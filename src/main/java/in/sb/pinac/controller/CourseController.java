package in.sb.pinac.controller;

import in.sb.pinac.entity.Category;
import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Review;
import in.sb.pinac.repository.ReviewRepository;
import in.sb.pinac.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/courses", "/api/courses"})
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private ReviewRepository reviewRepository;

    // GET ALL COURSES (with optional category filter or keyword search)
    @GetMapping
    public List<Course> getCourses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return courseService.searchCourses(search.trim());
        }
        if (category != null && !category.trim().isEmpty()) {
            return courseService.getCoursesByCategory(category.trim());
        }
        return courseService.getAllActiveCourses();
    }

    // GET COURSE BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET COURSE BY SLUG
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Course> getCourseBySlug(@PathVariable String slug) {
        return courseService.getCourseBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET ALL CATEGORIES
    @GetMapping("/categories")
    public List<Category> getCategories() {
        return courseService.getAllCategories();
    }

    // GET COURSE SYLLABUS (Chapters & Lessons)
    @GetMapping("/{id}/syllabus")
    public ResponseEntity<?> getCourseSyllabus(@PathVariable Long id) {
        try {
            Map<String, Object> syllabus = courseService.getCourseSyllabus(id);
            return ResponseEntity.ok(syllabus);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Autowired
    private in.sb.pinac.service.ProgressService progressService;

    // COMPLETE COURSE (Verify 100% and generate certificate)
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeCourse(@PathVariable Long id,
                                           @org.springframework.security.core.annotation.AuthenticationPrincipal in.sb.pinac.entity.User authUser,
                                           @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        if (targetId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required."));
        }

        try {
            Map<String, Object> res = progressService.completeCourseDirect(targetId, id);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // GET COURSE REVIEWS
    @GetMapping("/{id}/reviews")
    public List<Review> getCourseReviews(@PathVariable Long id) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(id);
    }
}
