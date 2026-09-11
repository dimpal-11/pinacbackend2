package in.sb.pinac.config;

import in.sb.pinac.entity.whatsapp.*;
import in.sb.pinac.repository.whatsapp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class WhatsAppChatbotInitializer {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppChatbotInitializer.class);

    @Bean
    public CommandLineRunner initWhatsAppChatbotData(
            WhatsAppSettingRepository settingRepository,
            WhatsAppFaqRepository faqRepository,
            WhatsAppKnowledgeBaseRepository kbRepository) {
        return args -> {
            // 1. Initialize Settings if empty
            if (settingRepository.count() == 0) {
                WhatsAppSetting setting = new WhatsAppSetting();
                setting.setPhoneNumberId("100654321987654");
                setting.setBusinessAccountId("100987654321098");
                setting.setAccessToken("EAAx_pinac_mock_meta_whatsapp_cloud_token_2026");
                setting.setVerifyToken("pinac_wa_webhook_verify_token_2026");
                setting.setAiEnabled(true);
                setting.setAiModel("gemini-1.5-flash");
                setting.setAutoReplyEnabled(true);
                setting.setHumanHandoverKeywords("human, sir, support, call, talk to human, agent, counselor");
                setting.setWelcomeMessage("Hello 👋 Welcome to PINAC Institute. How can I help you today?");
                setting.setFallbackMessage("I couldn't find the exact information. Your message has been forwarded to our support team. They will contact you shortly.");
                settingRepository.save(setting);
                log.info("Initialized default WhatsApp Chatbot settings.");
            }

            // 2. Initialize Seed FAQs if empty
            if (faqRepository.count() == 0) {
                List<WhatsAppFaq> defaultFaqs = List.of(
                        new WhatsAppFaq("What are the class timings?", "Classes are available 24/7 as recorded HD modules on our LMS with weekend live mentorship sessions every Saturday & Sunday (6:00 PM – 8:00 PM IST).", "TIMINGS", 1),
                        new WhatsAppFaq("Do I get a certificate upon course completion?", "Yes! You receive an industry-recognized, verifiable digital certificate with QR code validation upon submitting all milestone projects.", "CERTIFICATE", 2),
                        new WhatsAppFaq("Are there any installment / EMI payment options?", "Yes, we support flexible zero-cost EMI options via credit card and popular UPI pay-later providers on our Razorpay checkout gateway.", "PAYMENT", 3),
                        new WhatsAppFaq("Is placement assistance provided?", "100% placement support! We help build your showreel, review your portfolio, conduct mock interviews, and refer top students directly to partner studios.", "PLACEMENT", 4),
                        new WhatsAppFaq("Can I learn if I have no prior animation or design experience?", "Absolutely! All our courses start with core fundamentals and step-by-step guidance suitable for beginners to advanced learners.", "COURSES", 5)
                );
                faqRepository.saveAll(defaultFaqs);
                log.info("Initialized default WhatsApp Chatbot FAQs.");
            }

            // 3. Initialize Seed Knowledge Base if empty
            if (kbRepository.count() == 0) {
                List<WhatsAppKnowledgeBase> defaultKb = List.of(
                        new WhatsAppKnowledgeBase(
                                "Academy Overview & Mentorship",
                                "INSTITUTE",
                                "PINAC Institute (PINACXTREME) is a premier digital media academy founded by Pankaj Patil (12+ years industry experience). We train students in 3D Modeling, Animation, Visual Effects (VFX), Game Development with Unreal Engine, Graphic Design, and Video Editing.",
                                "pinac, institute, academy, pankaj patil, founder, mentor, about"
                        ),
                        new WhatsAppKnowledgeBase(
                                "Nashik Campus Location & Office Address",
                                "LOCATION",
                                "PINAC Animation & VFX Academy is located at College Road / Gangapur Road, Nashik, Maharashtra 422005. Office open Mon-Sat 10 AM to 7 PM. Google Maps: https://maps.google.com/?q=PINAC+Animation+Academy+Nashik",
                                "location, address, nashik, where, campus, office, visit, map"
                        ),
                        new WhatsAppKnowledgeBase(
                                "Online Learning Management System (LMS)",
                                "LMS",
                                "Enrolled students receive instant login credentials to our high-speed LMS portal with lifetime access to video modules, downloadable 3D project assets, software plugins, and a private discord community.",
                                "lms, portal, login, access, dashboard, video player, assets"
                        )
                );
                kbRepository.saveAll(defaultKb);
                log.info("Initialized default WhatsApp Chatbot Knowledge Base.");
            }
        };
    }
}
