package in.sb.pinac.dto.whatsapp;

public class WhatsAppSendReplyRequest {

    private String message;
    private Boolean markResolved = false;

    public WhatsAppSendReplyRequest() {}

    public WhatsAppSendReplyRequest(String message, Boolean markResolved) {
        this.message = message;
        this.markResolved = markResolved;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getMarkResolved() {
        return markResolved;
    }

    public void setMarkResolved(Boolean markResolved) {
        this.markResolved = markResolved;
    }
}
