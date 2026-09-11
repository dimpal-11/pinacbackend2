package in.sb.pinac.dto;

public class EnrollmentSheetDTO {
    private String studentId;
    private String studentName;
    private String email;
    private String mobile;
    private String courseName;
    private String courseId;
    private String enrollmentDate;
    private String status;

    public EnrollmentSheetDTO() {}

    public EnrollmentSheetDTO(String studentId, String studentName, String email, String mobile, String courseName, String courseId, String enrollmentDate, String status) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.email = email;
        this.mobile = mobile;
        this.courseName = courseName;
        this.courseId = courseId;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(String enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
