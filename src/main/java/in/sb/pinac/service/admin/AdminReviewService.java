package in.sb.pinac.service.admin;

import in.sb.pinac.entity.Review;
import in.sb.pinac.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public List<Map<String, Object>> getAllReviews(String status, String search) {
        List<Review> reviews = reviewRepository.findByOrderByCreatedAtDesc();

        return reviews.stream()
                .filter(r -> {
                    if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) return true;
                    return r.getStatus() != null && r.getStatus().equalsIgnoreCase(status.trim());
                })
                .filter(r -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String q = search.trim().toLowerCase();
                    boolean matchStudent = r.getUser() != null && r.getUser().getName() != null && r.getUser().getName().toLowerCase().contains(q);
                    boolean matchCourse = r.getCourse() != null && r.getCourse().getTitle() != null && r.getCourse().getTitle().toLowerCase().contains(q);
                    boolean matchComment = r.getComment() != null && r.getComment().toLowerCase().contains(q);
                    return matchStudent || matchCourse || matchComment;
                })
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("studentId", r.getUser() != null ? r.getUser().getId() : null);
                    map.put("studentName", r.getUser() != null ? r.getUser().getName() : "Student");
                    map.put("studentAvatar", r.getUser() != null ? r.getUser().getAvatar() : "");
                    map.put("courseId", r.getCourse() != null ? r.getCourse().getId() : null);
                    map.put("courseTitle", r.getCourse() != null ? r.getCourse().getTitle() : "Course");
                    map.put("rating", r.getRating() != null ? r.getRating() : 5.0);
                    map.put("comment", r.getComment());
                    map.put("status", r.getStatus() != null ? r.getStatus() : "APPROVED");
                    map.put("date", r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> updateReviewStatus(Long id, String status) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Review not found with ID: " + id));

        review.setStatus(status != null ? status.toUpperCase() : "APPROVED");
        Review saved = reviewRepository.save(review);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id", saved.getId());
        res.put("status", saved.getStatus());
        res.put("message", "Review marked as " + saved.getStatus());
        return res;
    }

    @Transactional
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new NoSuchElementException("Review not found with ID: " + id);
        }
        reviewRepository.deleteById(id);
    }
}
