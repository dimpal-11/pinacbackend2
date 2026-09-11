package in.sb.pinac.dto.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppConversation;
import in.sb.pinac.entity.whatsapp.WhatsAppMessage;

import java.util.List;

public class WhatsAppConversationDetailDto {

    private WhatsAppConversation conversation;
    private List<WhatsAppMessage> messages;

    public WhatsAppConversationDetailDto() {}

    public WhatsAppConversationDetailDto(WhatsAppConversation conversation, List<WhatsAppMessage> messages) {
        this.conversation = conversation;
        this.messages = messages;
    }

    public WhatsAppConversation getConversation() {
        return conversation;
    }

    public void setConversation(WhatsAppConversation conversation) {
        this.conversation = conversation;
    }

    public List<WhatsAppMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<WhatsAppMessage> messages) {
        this.messages = messages;
    }
}
