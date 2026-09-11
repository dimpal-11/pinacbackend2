package in.sb.pinac.repository;

import in.sb.pinac.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUserId(Long userId);
    List<Enrollment> findByUserIdAndActiveTrue(Long userId);
    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    List<Enrollment> findByOrderByEnrolledAtDesc();
    long countByActiveTrue();
    long countByCourseIdAndActiveTrue(Long courseId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT e.user.id) FROM Enrollment e WHERE e.course.id = :courseId AND e.active = true")
    long countDistinctActiveUsersByCourseId(@org.springframework.data.repository.query.Param("courseId") Long courseId);
}
