package in.sb.pinac.dto;

public class PaymentVerificationDTO {
    private Long paymentId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;
    private String name;
    private String email;
    private String mobile;

    public PaymentVerificationDTO() {}

    public PaymentVerificationDTO(Long paymentId, String razorpayPaymentId, String razorpayOrderId, String razorpaySignature, String name, String email, String mobile) {
        this.paymentId = paymentId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpaySignature = razorpaySignature;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}
