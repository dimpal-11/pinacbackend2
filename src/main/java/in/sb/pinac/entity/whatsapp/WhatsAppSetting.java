package in.sb.pinac.entity.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_settings")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number_id")
    private String phoneNumberId;

    @Column(name = "business_account_id")
    private String businessAccountId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "verify_token")
    private String verifyToken = "pinac_wa_webhook_verify_token_2026";

    @Column(name = "ai_enabled")
    private Boolean aiEnabled = true;

    @Column(name = "gemini_api_key")
    private String geminiApiKey;

    @Column(name = "ai_model", length = 100)
    private String aiModel = "gemini-1.5-flash";

    @Column(name = "auto_reply_enabled")
    private Boolean autoReplyEnabled = true;

    @Column(name = "human_handover_keywords", length = 500)
    private String humanHandoverKeywords = "human, sir, support, call, talk to human, agent, counselor";

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage = "Hello 👋 Welcome to PINAC Institute. How can I help you today?";

    @Column(name = "fallback_message", columnDefinition = "TEXT")
    private String fallbackMessage = "I couldn't find the exact information. Your message has been forwarded to our support team. They will contact you shortly.";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public WhatsAppSetting() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getBusinessAccountId() {
        return businessAccountId;
    }

    public void setBusinessAccountId(String businessAccountId) {
        this.businessAccountId = businessAccountId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public Boolean getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public Boolean getAutoReplyEnabled() {
        return autoReplyEnabled;
    }

    public void setAutoReplyEnabled(Boolean autoReplyEnabled) {
        this.autoReplyEnabled = autoReplyEnabled;
    }

    public String getHumanHandoverKeywords() {
        return humanHandoverKeywords;
    }

    public void setHumanHandoverKeywords(String humanHandoverKeywords) {
        this.humanHandoverKeywords = humanHandoverKeywords;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
