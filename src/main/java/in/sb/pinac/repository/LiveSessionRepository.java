package in.sb.pinac.repository;

import in.sb.pinac.entity.LiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {

    List<LiveSession> findByCourseId(Long courseId);

    List<LiveSession> findByCourseIdIn(List<Long> courseIds);

    List<LiveSession> findAllByOrderBySessionDateDescStartTimeDesc();

    List<LiveSession> findBySessionDateOrderByStartTimeAsc(LocalDate sessionDate);

    @Query("SELECT ls FROM LiveSession ls WHERE ls.course.id IN :courseIds ORDER BY ls.sessionDate ASC, ls.startTime ASC")
    List<LiveSession> findByCourseIdInOrderByDateAsc(@Param("courseIds") List<Long> courseIds);

    long countByStatus(String status);
}
