package in.sb.pinac.entity.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages", indexes = {
    @Index(name = "idx_wa_msg_conv_id", columnList = "conversation_id"),
    @Index(name = "idx_wa_msg_message_id", columnList = "message_id")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore
    private WhatsAppConversation conversation;

    @Column(name = "sender", nullable = false, length = 50)
    private String sender; // USER, AI, HUMAN_ADMIN

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "status", length = 50)
    private String status = "SENT"; // RECEIVED, SENT, DELIVERED, READ, FAILED

    @Column(name = "intent", length = 100)
    private String intent;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public WhatsAppMessage() {}

    public WhatsAppMessage(WhatsAppConversation conversation, String sender, String messageText, String messageId, String intent) {
        this.conversation = conversation;
        this.sender = sender;
        this.messageText = messageText;
        this.messageId = messageId;
        this.intent = intent;
        this.status = "USER".equalsIgnoreCase(sender) ? "RECEIVED" : "SENT";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WhatsAppConversation getConversation() {
        return conversation;
    }

    public void setConversation(WhatsAppConversation conversation) {
        this.conversation = conversation;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
