package in.sb.pinac.service.whatsapp;

import in.sb.pinac.entity.whatsapp.*;
import in.sb.pinac.repository.whatsapp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WhatsAppQueueService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppQueueService.class);

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WhatsAppConversationRepository conversationRepository;

    @Autowired
    private WhatsAppMessageRepository messageRepository;

    @Autowired
    private WhatsAppSupportTicketRepository supportTicketRepository;

    @Autowired
    private WhatsAppSettingRepository settingRepository;

    @Autowired
    private WhatsAppAIService aiService;

    @Autowired
    private WhatsAppCloudApiService cloudApiService;

    @Value("${whatsapp.rabbitmq.queue:whatsapp.incoming.messages}")
    private String queueName;

    @Value("${whatsapp.rabbitmq.exchange:whatsapp.direct.exchange}")
    private String exchangeName;

    @Value("${whatsapp.rabbitmq.routing-key:whatsapp.message.key}")
    private String routingKey;

    @Value("${whatsapp.rabbitmq.enabled:false}")
    private boolean rabbitMqEnabled;

    /**
     * Enqueue or asynchronously dispatch incoming message for high-throughput concurrency.
     */
    public void enqueueIncomingMessage(String waId, String userName, String userPhone, String userMessage, String messageId) {
        Map<String, String> payload = new HashMap<>();
        payload.put("waId", waId);
        payload.put("userName", userName != null ? userName : "Student");
        payload.put("userPhone", userPhone != null ? userPhone : waId);
        payload.put("userMessage", userMessage);
        payload.put("messageId", messageId != null ? messageId : "sim_" + UUID.randomUUID());

        if (rabbitMqEnabled && rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
                log.info("Message successfully pushed to RabbitMQ exchange: {}", exchangeName);
                return;
            } catch (Exception e) {
                log.warn("RabbitMQ dispatch failed, switching to Spring Async executor fallback: {}", e.getMessage());
            }
        }

        // Resilient fallback to Spring Async processing
        processIncomingMessageAsync(payload.get("waId"), payload.get("userName"), payload.get("userPhone"), payload.get("userMessage"), payload.get("messageId"));
    }

    /**
     * Core worker that processes the message and generates auto-replies.
     */
    @Async
    @Transactional
    public void processIncomingMessageAsync(String waId, String userName, String userPhone, String userMessage, String messageId) {
        try {
            log.info("Processing incoming WhatsApp message from {}: '{}'", waId, userMessage);

            // Deduplication check
            if (messageId != null && messageRepository.existsByMessageId(messageId)) {
                log.info("Skipping duplicate message with ID: {}", messageId);
                return;
            }

            // 1. Get or create conversation
            WhatsAppConversation conversation = conversationRepository.findByWaId(waId)
                    .orElseGet(() -> {
                        WhatsAppConversation newConv = new WhatsAppConversation(waId, userName, userPhone);
                        return conversationRepository.save(newConv);
                    });

            if (userName != null && (conversation.getUserName() == null || conversation.getUserName().equals("Student"))) {
                conversation.setUserName(userName);
            }
            if (userPhone != null) {
                conversation.setUserPhone(userPhone);
            }

            conversation.setLastMessageText(userMessage);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.setUnreadCount((conversation.getUnreadCount() != null ? conversation.getUnreadCount() : 0) + 1);

            // 2. Save incoming user message
            WhatsAppMessage incomingMsg = new WhatsAppMessage(conversation, "USER", userMessage, messageId, "INCOMING");
            messageRepository.save(incomingMsg);

            // 3. Check if conversation is currently held by a Human Admin
            if ("HUMAN_REQUIRED".equalsIgnoreCase(conversation.getStatus())) {
                log.info("Conversation {} is marked HUMAN_REQUIRED. Awaiting admin reply.", waId);
                conversationRepository.save(conversation);
                return;
            }

            // 4. Generate AI response
            WhatsAppAIService.AIResponseResult aiResult = aiService.generateResponse(userMessage, userName, userPhone);

            // 5. If user requested human support, escalate conversation & create support ticket
            if (aiResult.isRequiresHuman()) {
                conversation.setStatus("HUMAN_REQUIRED");
                conversation.setAiResolved(false);

                WhatsAppSupportTicket ticket = new WhatsAppSupportTicket(
                        conversation,
                        conversation.getUserName(),
                        conversation.getUserPhone(),
                        "Student requested human counselor assistance on WhatsApp: " + userMessage
                );
                supportTicketRepository.save(ticket);
                log.info("Created support ticket for conversation: {}", waId);
            } else {
                conversation.setAiResolved(true);
                conversation.setStatus("AI_ACTIVE");
            }

            conversationRepository.save(conversation);

            // 6. Save AI reply in message history
            WhatsAppMessage aiMsg = new WhatsAppMessage(
                    conversation,
                    "AI",
                    aiResult.getReplyText(),
                    "ai_" + UUID.randomUUID(),
                    aiResult.getIntent()
            );
            messageRepository.save(aiMsg);

            // 7. Dispatch outgoing message to student's WhatsApp via Meta Cloud API
            cloudApiService.sendTextMessage(waId, aiResult.getReplyText());

            log.info("Successfully processed and replied to WhatsApp message from {}", waId);

        } catch (Exception e) {
            log.error("Error processing WhatsApp message for {}: {}", waId, e.getMessage(), e);
        }
    }
}
