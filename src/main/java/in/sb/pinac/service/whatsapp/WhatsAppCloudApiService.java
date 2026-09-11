package in.sb.pinac.service.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppSetting;
import in.sb.pinac.repository.whatsapp.WhatsAppSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppCloudApiService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudApiService.class);

    @Autowired
    private WhatsAppSettingRepository settingRepository;

    @Value("${whatsapp.api.url:https://graph.facebook.com/v20.0}")
    private String defaultApiUrl;

    @Value("${whatsapp.api.phone-number-id:}")
    private String defaultPhoneNumberId;

    @Value("${whatsapp.api.access-token:}")
    private String defaultAccessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send text message to user's WhatsApp number via Meta Cloud API.
     */
    public boolean sendTextMessage(String recipientPhone, String messageText) {
        if (recipientPhone == null || recipientPhone.trim().isEmpty() || messageText == null || messageText.trim().isEmpty()) {
            log.warn("Cannot send WhatsApp message: Recipient or messageText is empty");
            return false;
        }

        // Format phone: ensure digits only
        String cleanPhone = recipientPhone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 10) {
            cleanPhone = "91" + cleanPhone; // Default country code India (+91)
        }

        WhatsAppSetting settings = settingRepository.findFirstByOrderByIdAsc().orElse(new WhatsAppSetting());

        String phoneNumberId = settings.getPhoneNumberId() != null && !settings.getPhoneNumberId().isBlank()
                ? settings.getPhoneNumberId()
                : defaultPhoneNumberId;

        String accessToken = settings.getAccessToken() != null && !settings.getAccessToken().isBlank()
                ? settings.getAccessToken()
                : defaultAccessToken;

        // In development or mock mode, log message gracefully and return success
        if (phoneNumberId == null || phoneNumberId.isBlank() || accessToken == null || accessToken.isBlank() || accessToken.contains("mock")) {
            log.info("[WhatsApp Cloud API (Mock Mode)] Sending to +{}: \n{}", cleanPhone, messageText);
            return true;
        }

        String endpointUrl = defaultApiUrl + "/" + phoneNumberId + "/messages";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("recipient_type", "individual");
            body.put("to", cleanPhone);
            body.put("type", "text");

            Map<String, String> textObj = new HashMap<>();
            textObj.put("preview_url", "true");
            textObj.put("body", messageText);
            body.put("text", textObj);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpointUrl, HttpMethod.POST, requestEntity, String.class);

            log.info("Meta WhatsApp Cloud API response status: {}", response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message via Meta Cloud API to +{}: {}", cleanPhone, e.getMessage());
            return false;
        }
    }
}
