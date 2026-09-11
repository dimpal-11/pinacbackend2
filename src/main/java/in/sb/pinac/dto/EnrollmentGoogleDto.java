package in.sb.pinac.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EnrollmentGoogleDto {

    private String studentName;
    private String email;
    private String mobile;
    private String courseName;
    private String courseId;
    private String enrolledAt;
    private String status = "ACTIVE";
    private Integer progress = 0;

    public EnrollmentGoogleDto() {
        this.enrolledAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public EnrollmentGoogleDto(String studentName, String email, String mobile, String courseName, String courseId) {
        this.studentName = studentName;
        this.email = email;
        this.mobile = mobile;
        this.courseName = courseName;
        this.courseId = courseId;
        this.enrolledAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    // Getters and Setters
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(String enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}
