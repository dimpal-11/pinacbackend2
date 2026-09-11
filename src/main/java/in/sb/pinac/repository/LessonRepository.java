package in.sb.pinac.repository;

import in.sb.pinac.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByChapterIdOrderByOrderIndexAsc(Long chapterId);

    @Query("SELECT l FROM Lesson l WHERE l.chapter.course.id = :courseId ORDER BY l.chapter.orderIndex ASC, l.orderIndex ASC")
    List<Lesson> findAllByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.chapter.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
