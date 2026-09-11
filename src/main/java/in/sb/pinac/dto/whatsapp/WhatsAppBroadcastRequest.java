package in.sb.pinac.dto.whatsapp;

public class WhatsAppBroadcastRequest {

    private String title;
    private String messageTemplate;
    private String targetAudience = "ALL";

    public WhatsAppBroadcastRequest() {}

    public WhatsAppBroadcastRequest(String title, String messageTemplate, String targetAudience) {
        this.title = title;
        this.messageTemplate = messageTemplate;
        this.targetAudience = targetAudience;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }
}
