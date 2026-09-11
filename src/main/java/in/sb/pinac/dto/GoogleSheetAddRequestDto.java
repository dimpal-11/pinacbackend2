package in.sb.pinac.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GoogleSheetAddRequestDto {

    @JsonProperty("userName")
    @JsonAlias({"name", "studentName"})
    private String userName;

    @JsonProperty("userEmail")
    @JsonAlias({"email", "studentEmail"})
    private String userEmail;

    @JsonProperty("mobileNumber")
    @JsonAlias({"mobile", "phone", "contactNumber"})
    private String mobileNumber;

    @JsonProperty("courseName")
    @JsonAlias({"course", "courseTitle"})
    private String courseName;

    @JsonProperty("courseId")
    private String courseId;

    @JsonProperty("coursePrice")
    @JsonAlias({"price", "amount"})
    private Object coursePrice;

    @JsonProperty("paymentId")
    @JsonAlias({"razorpayPaymentId", "transactionId"})
    private String paymentId;

    @JsonProperty("orderId")
    @JsonAlias({"razorpayOrderId"})
    private String orderId;

    @JsonProperty("paymentStatus")
    @JsonAlias({"status"})
    private String paymentStatus;

    @JsonProperty("city")
    private String city;

    @JsonProperty("courseActive")
    @JsonAlias({"active", "isActive"})
    private Object courseActive;

    public GoogleSheetAddRequestDto() {}

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
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

    public Object getCoursePrice() {
        return coursePrice;
    }

    public void setCoursePrice(Object coursePrice) {
        this.coursePrice = coursePrice;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Object getCourseActive() {
        return courseActive;
    }

    public void setCourseActive(Object courseActive) {
        this.courseActive = courseActive;
    }
}
