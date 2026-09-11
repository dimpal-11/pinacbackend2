package in.sb.pinac.dto.admin;

import java.time.LocalDate;
import java.util.List;

public class AttendanceBulkMarkRequest {
    private LocalDate attendanceDate;
    private Long courseId;
    private String courseName;
    private String batchName;
    private List<AttendanceRecordDto> records;

    public AttendanceBulkMarkRequest() {}

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public List<AttendanceRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceRecordDto> records) {
        this.records = records;
    }
}
