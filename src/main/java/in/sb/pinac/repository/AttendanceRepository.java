package in.sb.pinac.repository;

import in.sb.pinac.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByAttendanceDateOrderByStudentNameAsc(LocalDate attendanceDate);

    List<Attendance> findByAttendanceDateAndCourseId(LocalDate attendanceDate, Long courseId);

    List<Attendance> findByAttendanceDateAndBatchName(LocalDate attendanceDate, String batchName);

    List<Attendance> findByStudentIdOrderByAttendanceDateDesc(String studentId);

    List<Attendance> findByUserIdOrderByAttendanceDateDesc(Long userId);

    Optional<Attendance> findByStudentIdAndAttendanceDateAndCourseId(String studentId, LocalDate attendanceDate, Long courseId);

    Optional<Attendance> findByStudentIdAndAttendanceDate(String studentId, LocalDate attendanceDate);

    long countByAttendanceDateAndStatus(LocalDate attendanceDate, String status);

    long countByAttendanceDate(LocalDate attendanceDate);

    @Query("SELECT a FROM Attendance a WHERE (:date IS NULL OR a.attendanceDate = :date) " +
           "AND (:courseId IS NULL OR a.courseId = :courseId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:batchName IS NULL OR a.batchName = :batchName) " +
           "ORDER BY a.attendanceDate DESC, a.studentName ASC")
    List<Attendance> findWithFilters(@Param("date") LocalDate date,
                                     @Param("courseId") Long courseId,
                                     @Param("status") String status,
                                     @Param("batchName") String batchName);

    List<Attendance> findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate startDate, LocalDate endDate);
}
