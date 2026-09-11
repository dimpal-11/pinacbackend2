package in.sb.pinac.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class BrevoSmsService {

    private static final Logger log = LoggerFactory.getLogger(BrevoSmsService.class);

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sms.sender:PINAC}")
    private String senderName;

    @Value("${brevo.sms.url:https://api.brevo.com/v3/transactionalSMS/send}")
    private String brevoSmsUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send Transactional OTP SMS to student's mobile number via Brevo REST API.
     */
    public boolean sendOtpSms(String mobileNumber, String otp) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            log.warn("Cannot send OTP SMS: Mobile number or OTP is empty.");
            return false;
        }

        // Format recipient to E.164 without plus: e.g. 919876543210
        String cleanPhone = mobileNumber.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 10) {
            cleanPhone = "91" + cleanPhone; // Default to India (+91)
        }

        final String recipient = cleanPhone;
        final String messageContent = "Your PINACXTREME verification code is " + otp.trim()
                + ". Valid for 5 minutes. Do not share this OTP with anyone.";

        System.out.println("========================================");
        System.out.println("DISPATCHING OTP SMS TO MOBILE");
        System.out.println("Recipient : +" + recipient);
        System.out.println("OTP       : " + otp);
        System.out.println("Gateway   : Brevo Transactional SMS API");
        System.out.println("========================================");

        // Run asynchronously so UI does not block
        CompletableFuture.runAsync(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey != null ? brevoApiKey.trim() : "");
                headers.set("accept", "application/json");

                Map<String, Object> payload = new HashMap<>();
                payload.put("sender", senderName != null && !senderName.isBlank() ? senderName : "PINAC");
                payload.put("recipient", recipient);
                payload.put("content", messageContent);
                payload.put("type", "transactional");

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        brevoSmsUrl,
                        HttpMethod.POST,
                        request,
                        Map.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    System.out.println("✅ Brevo Transactional SMS sent successfully to +" + recipient);
                    log.info("Brevo OTP SMS sent to +{}. Response: {}", recipient, response.getBody());
                } else {
                    System.err.println("⚠️ Brevo SMS responded with status: " + response.getStatusCode());
                    log.warn("Brevo SMS failed with status: {}", response.getStatusCode());
                }
            } catch (HttpClientErrorException e) {
                System.err.println("⚠️ Brevo SMS API Notice: " + e.getResponseBodyAsString());
                log.warn("Brevo SMS HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            } catch (Exception e) {
                System.err.println("⚠️ Could not reach Brevo SMS gateway: " + e.getMessage());
                log.error("Brevo SMS connection error: {}", e.getMessage());
            }
        });

        return true;
    }
}
