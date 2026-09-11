package in.sb.pinac.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StudentGoogleDto {

    private String name;
    private String email;
    private String mobile;
    private String city;
    private String courseName;
    private String signupDate;

    public StudentGoogleDto() {
        this.signupDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public StudentGoogleDto(String name, String email, String mobile, String city, String courseName) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.city = city;
        this.courseName = courseName;
        this.signupDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getSignupDate() {
        return signupDate;
    }

    public void setSignupDate(String signupDate) {
        this.signupDate = signupDate;
    }
}
