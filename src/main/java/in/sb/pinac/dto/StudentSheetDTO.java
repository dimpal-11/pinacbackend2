package in.sb.pinac.dto;

import java.time.LocalDateTime;

public class StudentSheetDTO {
    private String name;
    private String email;
    private String mobile;
    private String city;
    private String courseName;
    private String courseId;
    private Double coursePrice;
    private String signupDate;

    public StudentSheetDTO() {}

    public StudentSheetDTO(String name, String email, String mobile, String city, String courseName, String courseId, Double coursePrice, String signupDate) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.city = city;
        this.courseName = courseName;
        this.courseId = courseId;
        this.coursePrice = coursePrice;
        this.signupDate = signupDate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public Double getCoursePrice() { return coursePrice; }
    public void setCoursePrice(Double coursePrice) { this.coursePrice = coursePrice; }

    public String getSignupDate() { return signupDate; }
    public void setSignupDate(String signupDate) { this.signupDate = signupDate; }
}
