package in.sb.pinac.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PaymentGoogleDto {

    private String studentName;
    private String email;
    private String mobile;
    private String course;
    private Double amount;
    private String paymentId;
    private String orderId;
    private String paymentStatus = "SUCCESS";
    private String paymentDate;
    private String transactionTime;

    public PaymentGoogleDto() {
        LocalDateTime now = LocalDateTime.now();
        this.paymentDate = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        this.transactionTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public PaymentGoogleDto(String studentName, String email, String mobile, String course,
                            Double amount, String paymentId, String orderId, String paymentStatus) {
        this.studentName = studentName;
        this.email = email;
        this.mobile = mobile;
        this.course = course;
        this.amount = amount;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentStatus = paymentStatus != null ? paymentStatus : "SUCCESS";

        LocalDateTime now = LocalDateTime.now();
        this.paymentDate = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        this.transactionTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
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

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
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

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }
}
