package in.sb.pinac.dto;

public class PaymentSheetDTO {
    private String name;
    private String email;
    private String mobile;
    private String city;
    private String courseName;
    private String courseId;
    private Double amount;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String paymentStatus;
    private String paymentDate;
    private String transactionTime;
    private String enrollmentStatus;

    public PaymentSheetDTO() {}

    public PaymentSheetDTO(String name, String email, String mobile, String city, String courseName, String courseId, Double amount, String razorpayPaymentId, String razorpayOrderId, String paymentStatus, String paymentDate, String transactionTime, String enrollmentStatus) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.city = city;
        this.courseName = courseName;
        this.courseId = courseId;
        this.amount = amount;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
        this.transactionTime = transactionTime;
        this.enrollmentStatus = enrollmentStatus;
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

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getTransactionTime() { return transactionTime; }
    public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }

    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }
}
