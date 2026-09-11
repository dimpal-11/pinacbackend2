package in.sb.pinac.repository;

import in.sb.pinac.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    List<Review> findByOrderByCreatedAtDesc();
    List<Review> findByStatusOrderByCreatedAtDesc(String status);
}

