package in.sb.pinac.dto.whatsapp;

public class WhatsAppStatusUpdateRequest {

    private String status; // AI_ACTIVE, HUMAN_REQUIRED, RESOLVED, CLOSED

    public WhatsAppStatusUpdateRequest() {}

    public WhatsAppStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
