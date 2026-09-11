package in.sb.pinac.controller;

import in.sb.pinac.dto.whatsapp.WhatsAppSimulateRequest;
import in.sb.pinac.dto.whatsapp.WhatsAppWebhookPayload;
import in.sb.pinac.entity.whatsapp.WhatsAppSetting;
import in.sb.pinac.repository.whatsapp.WhatsAppSettingRepository;
import in.sb.pinac.service.whatsapp.WhatsAppAIService;
import in.sb.pinac.service.whatsapp.WhatsAppQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/whatsapp")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Autowired
    private WhatsAppQueueService queueService;

    @Autowired
    private WhatsAppAIService aiService;

    @Autowired
    private WhatsAppSettingRepository settingRepository;

    @Value("${whatsapp.api.verify-token:pinac_wa_webhook_verify_token_2026}")
    private String defaultVerifyToken;

    /**
     * 1. Meta WhatsApp Webhook Challenge Verification (GET)
     * Meta sends: ?hub.mode=subscribe&hub.verify_token=...&hub.challenge=...
     */
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        log.info("Received Meta WhatsApp Webhook verification request. mode={}, token={}", mode, verifyToken);

        WhatsAppSetting settings = settingRepository.findFirstByOrderByIdAsc().orElse(new WhatsAppSetting());
        String expectedToken = settings.getVerifyToken() != null && !settings.getVerifyToken().isBlank()
                ? settings.getVerifyToken()
                : defaultVerifyToken;

        if ("subscribe".equalsIgnoreCase(mode) && expectedToken.equals(verifyToken)) {
            log.info("WhatsApp Webhook verified successfully! Returning challenge.");
            return ResponseEntity.ok(challenge != null ? challenge : "VERIFIED");
        } else {
            log.warn("WhatsApp Webhook verification failed. Token mismatch or invalid mode.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch");
        }
    }

    /**
     * 2. Meta WhatsApp Incoming Messages Webhook (POST)
     * Receives event payloads from Meta Cloud API
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleIncomingWebhook(@RequestBody(required = false) WhatsAppWebhookPayload payload) {
        log.info("Received WhatsApp Webhook POST event");

        if (payload == null || payload.getEntry() == null || payload.getEntry().isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "reason", "Empty payload"));
        }

        try {
            for (WhatsAppWebhookPayload.Entry entry : payload.getEntry()) {
                if (entry.getChanges() == null) continue;

                for (WhatsAppWebhookPayload.Change change : entry.getChanges()) {
                    WhatsAppWebhookPayload.Value val = change.getValue();
                    if (val == null) continue;

                    // Handle Incoming Messages
                    if (val.getMessages() != null && !val.getMessages().isEmpty()) {
                        String senderName = "Student";
                        if (val.getContacts() != null && !val.getContacts().isEmpty()) {
                            WhatsAppWebhookPayload.Contact c = val.getContacts().get(0);
                            if (c.getProfile() != null && c.getProfile().getName() != null) {
                                senderName = c.getProfile().getName();
                            }
                        }

                        for (WhatsAppWebhookPayload.Message msg : val.getMessages()) {
                            String fromWaId = msg.getFrom();
                            String messageId = msg.getId();
                            String textBody = (msg.getText() != null) ? msg.getText().getBody() : "";

                            if (textBody != null && !textBody.trim().isEmpty()) {
                                // Dispatch to asynchronous queue/worker
                                queueService.enqueueIncomingMessage(fromWaId, senderName, fromWaId, textBody, messageId);
                            }
                        }
                    }

                    // Handle Message Status Delivery Receipts (DELIVERED, READ)
                    if (val.getStatuses() != null && !val.getStatuses().isEmpty()) {
                        for (WhatsAppWebhookPayload.Status st : val.getStatuses()) {
                            log.info("WhatsApp Message Status update: id={}, status={}", st.getId(), st.getStatus());
                        }
                    }
                }
            }

            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Event queued for processing"));
        } catch (Exception e) {
            log.error("Error processing Meta webhook payload: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "HANDLED_WITH_ERROR", "error", e.getMessage()));
        }
    }

    /**
     * 3. Dev Simulator Endpoint
     * Enables direct manual testing of AI Chatbot without requiring live WhatsApp credentials.
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateMessage(@RequestBody WhatsAppSimulateRequest req) {
        String phone = (req.getPhone() != null && !req.getPhone().isBlank()) ? req.getPhone().trim() : "917219194211";
        String name = (req.getName() != null && !req.getName().isBlank()) ? req.getName().trim() : "Prospective Student";
        String message = (req.getMessage() != null && !req.getMessage().isBlank()) ? req.getMessage().trim() : "Tell me about Graphic Design course";

        String messageId = "sim_" + UUID.randomUUID();

        // Process message synchronously for instant test feedback
        queueService.processIncomingMessageAsync(phone, name, phone, message, messageId);

        // Also generate response for immediate preview response
        WhatsAppAIService.AIResponseResult preview = aiService.generateResponse(message, name, phone);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("simulatedPhone", phone);
        resp.put("simulatedName", name);
        resp.put("userMessage", message);
        resp.put("aiReply", preview.getReplyText());
        resp.put("intent", preview.getIntent());
        resp.put("requiresHuman", preview.isRequiresHuman());

        return ResponseEntity.ok(resp);
    }
}
