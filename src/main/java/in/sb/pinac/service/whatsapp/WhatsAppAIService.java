package in.sb.pinac.service.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppFaq;
import in.sb.pinac.entity.whatsapp.WhatsAppKnowledgeBase;
import in.sb.pinac.entity.whatsapp.WhatsAppSetting;
import in.sb.pinac.repository.whatsapp.WhatsAppFaqRepository;
import in.sb.pinac.repository.whatsapp.WhatsAppKnowledgeBaseRepository;
import in.sb.pinac.repository.whatsapp.WhatsAppSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class WhatsAppAIService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppAIService.class);

    @Autowired
    private WhatsAppSettingRepository settingRepository;

    @Autowired
    private WhatsAppFaqRepository faqRepository;

    @Autowired
    private WhatsAppKnowledgeBaseRepository knowledgeBaseRepository;

    @Value("${whatsapp.ai.gemini-api-key:}")
    private String defaultGeminiApiKey;

    @Value("${whatsapp.ai.model:gemini-1.5-flash}")
    private String defaultAiModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public static class AIResponseResult {
        private String replyText;
        private String intent; // GREETING, COURSE_ENQUIRY, FEE_ENQUIRY, BATCH_ENQUIRY, ADMISSION_ENQUIRY, HUMAN_SUPPORT, FAQ, UNKNOWN
        private boolean requiresHuman;

        public AIResponseResult(String replyText, String intent, boolean requiresHuman) {
            this.replyText = replyText;
            this.intent = intent;
            this.requiresHuman = requiresHuman;
        }

        public String getReplyText() {
            return replyText;
        }

        public String getIntent() {
            return intent;
        }

        public boolean isRequiresHuman() {
            return requiresHuman;
        }
    }

    /**
     * Main entry point to process a user's WhatsApp message and generate an AI reply.
     */
    public AIResponseResult generateResponse(String userMessage, String userName, String userPhone) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return new AIResponseResult(
                    "Hello! Welcome to PINAC Institute. How can I help you today?",
                    "GREETING",
                    false
            );
        }

        String cleanedMsg = userMessage.trim().toLowerCase();
        WhatsAppSetting settings = settingRepository.findFirstByOrderByIdAsc().orElse(new WhatsAppSetting());

        // 1. Check for Human Support / "Connect with Sir" Intent
        if (isHumanSupportRequested(cleanedMsg, settings)) {
            return new AIResponseResult(
                    "Your request has been forwarded to our support team and Sir will contact you shortly on this number (" + userPhone + "). 📞\n\nFor urgent assistance, you can also reach us at +91 72191 94211.",
                    "HUMAN_SUPPORT",
                    true
            );
        }

        // 2. Check for Greetings
        if (isGreeting(cleanedMsg)) {
            String welcome = settings.getWelcomeMessage() != null ? settings.getWelcomeMessage() :
                    "Hello 👋\nWelcome to PINAC Institute.\nHow can I help you today?";
            return new AIResponseResult(
                    welcome + "\n\nYou can ask me about:\n1️⃣ Course Details & Syllabus\n2️⃣ Fees & Offers\n3️⃣ Upcoming Batches\n4️⃣ Admission Process\n5️⃣ Placement Support",
                    "GREETING",
                    false
            );
        }

        // 3. Try to call Gemini AI with full PINAC knowledge context if API key is configured
        String apiKey = settings.getGeminiApiKey() != null && !settings.getGeminiApiKey().isBlank()
                ? settings.getGeminiApiKey()
                : defaultGeminiApiKey;

        if (settings.getAiEnabled() && apiKey != null && !apiKey.isBlank() && !apiKey.contains("mock")) {
            try {
                String aiReply = callGeminiAI(userMessage, userName, apiKey, settings.getAiModel());
                if (aiReply != null && !aiReply.isBlank()) {
                    String detectedIntent = detectIntent(cleanedMsg);
                    return new AIResponseResult(aiReply.trim(), detectedIntent, false);
                }
            } catch (Exception e) {
                log.warn("Gemini AI API call failed, falling back to built-in knowledge base: {}", e.getMessage());
            }
        }

        // 4. Built-in High-Accuracy Intelligent Knowledge Engine (Zero external dependency fallback)
        return generateFromKnowledgeBase(cleanedMsg, userMessage);
    }

    private boolean isHumanSupportRequested(String msg, WhatsAppSetting settings) {
        String keywords = settings.getHumanHandoverKeywords();
        if (keywords == null || keywords.isBlank()) {
            keywords = "human, sir, support, call, talk to human, connect me to sir, agent, counselor, helpdesk, talk to person";
        }

        String[] kwArray = keywords.split(",");
        for (String kw : kwArray) {
            if (!kw.trim().isEmpty() && msg.contains(kw.trim().toLowerCase())) {
                return true;
            }
        }

        return msg.contains("talk to human") ||
               msg.contains("connect me to sir") ||
               msg.contains("need support") ||
               msg.contains("call me") ||
               msg.contains("talk to sir") ||
               msg.contains("speak with sir");
    }

    private boolean isGreeting(String msg) {
        String m = msg.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        return m.equals("hi") || m.equals("hello") || m.equals("hey") || m.equals("hola") ||
               m.equals("namaste") || m.equals("good morning") || m.equals("good afternoon") ||
               m.equals("good evening") || m.equals("hii") || m.equals("hiii") || m.equals("start");
    }

    private String detectIntent(String msg) {
        if (msg.contains("fee") || msg.contains("price") || msg.contains("cost") || msg.contains("discount") || msg.contains("offer")) {
            return "FEE_ENQUIRY";
        }
        if (msg.contains("batch") || msg.contains("time") || msg.contains("timing") || msg.contains("schedule") || msg.contains("start date")) {
            return "BATCH_ENQUIRY";
        }
        if (msg.contains("admission") || msg.contains("enroll") || msg.contains("join") || msg.contains("register") || msg.contains("apply")) {
            return "ADMISSION_ENQUIRY";
        }
        if (msg.contains("course") || msg.contains("graphic") || msg.contains("3d") || msg.contains("animation") ||
            msg.contains("vfx") || msg.contains("unreal") || msg.contains("game") || msg.contains("video editing") ||
            msg.contains("ui") || msg.contains("ux") || msg.contains("modeling") || msg.contains("lighting")) {
            return "COURSE_ENQUIRY";
        }
        if (msg.contains("location") || msg.contains("address") || msg.contains("where") || msg.contains("map") || msg.contains("city")) {
            return "LOCATION_ENQUIRY";
        }
        if (msg.contains("placement") || msg.contains("job") || msg.contains("career") || msg.contains("salary")) {
            return "PLACEMENT_ENQUIRY";
        }
        if (msg.contains("demo") || msg.contains("trial") || msg.contains("preview")) {
            return "DEMO_ENQUIRY";
        }
        return "GENERAL_ENQUIRY";
    }

    /**
     * Built-in PINAC Knowledge Base Matcher
     */
    private AIResponseResult generateFromKnowledgeBase(String cleanedMsg, String originalMsg) {
        // Check dynamic database FAQs first
        List<WhatsAppFaq> faqs = faqRepository.findByActiveTrueOrderByOrderIndexAsc();
        for (WhatsAppFaq faq : faqs) {
            String q = faq.getQuestion().toLowerCase();
            if (cleanedMsg.contains(q) || calculateSimilarity(cleanedMsg, q) > 0.6) {
                return new AIResponseResult(faq.getAnswer(), "FAQ", false);
            }
        }

        // Check dynamic knowledge base items
        List<WhatsAppKnowledgeBase> kbList = knowledgeBaseRepository.findByActiveTrue();
        for (WhatsAppKnowledgeBase kb : kbList) {
            if (kb.getKeywords() != null) {
                for (String kw : kb.getKeywords().split(",")) {
                    if (!kw.trim().isEmpty() && cleanedMsg.contains(kw.trim().toLowerCase())) {
                        return new AIResponseResult(kb.getContent(), "KNOWLEDGE_BASE", false);
                    }
                }
            }
        }

        // Specific Course Matches
        if (cleanedMsg.contains("graphic") || cleanedMsg.contains("ui/ux") || cleanedMsg.contains("ui ux") || cleanedMsg.contains("product design")) {
            return new AIResponseResult(
                    "🎨 *Graphic Design & UI/UX Masterclass*\n\n" +
                    "• *Summary*: Master industry-standard visual design, Figma, Adobe Photoshop, Illustrator, design systems, and mobile/web UX workflows.\n" +
                    "• *Instructor*: Pankaj Patil (Founder & Lead Mentor)\n" +
                    "• *Duration*: 30h 00m (Self-paced + Live Project Guidance)\n" +
                    "• *Fee*: ₹2,999 (Special limited offer)\n" +
                    "• *Key Topics*: Figma Auto-Layout, Wireframing, Brand Identity, UI Design Systems, Portfolio Building.\n" +
                    "• *Placement Support*: 100% Portfolio & Resume Guidance + Mock Interviews.\n\n" +
                    "👉 *Enroll Online*: https://pinacxtreme.com/courses/7\n" +
                    "👉 *Free Demo Class Available!* Reply 'Demo' to watch a preview.",
                    "COURSE_ENQUIRY",
                    false
            );
        }

        if (cleanedMsg.contains("3d modeling") || cleanedMsg.contains("texturing") || cleanedMsg.contains("maya") || cleanedMsg.contains("blender")) {
            return new AIResponseResult(
                    "🧊 *3D Modeling & Texturing Masterclass*\n\n" +
                    "• *Summary*: Learn hard-surface & organic 3D modeling, UV unwrapping, and realistic PBR texturing with Autodesk Maya & Substance Painter.\n" +
                    "• *Instructor*: Pankaj Patil\n" +
                    "• *Duration*: 36h 20m\n" +
                    "• *Fee*: ₹2,999\n" +
                    "• *Software*: Autodesk Maya, Blender, Substance 3D Painter, ZBrush.\n" +
                    "• *Career Options*: 3D Environment Artist, Prop Modeler, Game Asset Creator.\n\n" +
                    "👉 *Enroll Online*: https://pinacxtreme.com/courses/1\n" +
                    "Reply 'Admission' for step-by-step joining process.",
                    "COURSE_ENQUIRY",
                    false
            );
        }

        if (cleanedMsg.contains("character animation") || cleanedMsg.contains("3d animation") || cleanedMsg.contains("walk cycle")) {
            return new AIResponseResult(
                    "🎬 *3D Character Animation Masterclass*\n\n" +
                    "• *Summary*: Master complete Maya character animation workflow, body mechanics, walk/run cycles, acting, and facial lip-sync.\n" +
                    "• *Instructor*: Pankaj Patil\n" +
                    "• *Duration*: 34h 00m\n" +
                    "• *Fee*: ₹2,999\n" +
                    "• *Includes*: Production-grade character rigs & portfolio animation assignments.\n\n" +
                    "👉 *Enroll Online*: https://pinacxtreme.com/courses/3",
                    "COURSE_ENQUIRY",
                    false
            );
        }

        if (cleanedMsg.contains("vfx") || cleanedMsg.contains("dynamics") || cleanedMsg.contains("houdini") || cleanedMsg.contains("particles")) {
            return new AIResponseResult(
                    "💥 *VFX & Dynamics Masterclass*\n\n" +
                    "• *Summary*: Master cinematic simulations: Fire, Smoke, Water, Explosions, Destruction, and Multi-pass Compositing for feature films.\n" +
                    "• *Instructor*: Pankaj Patil\n" +
                    "• *Duration*: 42h 00m\n" +
                    "• *Fee*: ₹4,999\n" +
                    "• *Software*: Houdini, Maya Bifrost, Nuke, After Effects.\n\n" +
                    "👉 *Enroll Online*: https://pinacxtreme.com/courses/6",
                    "COURSE_ENQUIRY",
                    false
            );
        }

        if (cleanedMsg.contains("unreal") || cleanedMsg.contains("game dev") || cleanedMsg.contains("unreal engine")) {
            return new AIResponseResult(
                    "🎮 *Unreal Engine 5 Game Development*\n\n" +
                    "• *Summary*: Build AAA-grade games with UE5 Blueprints, Lumen lighting, Nanite geometry, physics, and interactive gameplay mechanics.\n" +
                    "• *Duration*: 45h 00m\n" +
                    "• *Fee*: ₹3,999\n\n" +
                    "👉 *Enroll Online*: https://pinacxtreme.com/courses/5",
                    "COURSE_ENQUIRY",
                    false
            );
        }

        if (cleanedMsg.contains("video editing") || cleanedMsg.contains("premiere") || cleanedMsg.contains("after effects") || cleanedMsg.contains("motion graphics")) {
            return new AIResponseResult(
                    "✂️ *Video Editing & Motion Graphics*\n\n" +
                    "• *Summary*: Master Adobe Premiere Pro & After Effects for cinematic editing, color grading, sound design, and 2D/3D motion graphics.\n" +
                    "• *Duration*: 32h 00m\n" +
                    "• *Fee*: ₹2,999\n\n" +
                    "👉 *Enroll Online*: https://pinacxtreme.com/courses/8",
                    "COURSE_ENQUIRY",
                    false
            );
        }

        // General Fee Query
        if (cleanedMsg.contains("fee") || cleanedMsg.contains("fees") || cleanedMsg.contains("price") || cleanedMsg.contains("cost")) {
            return new AIResponseResult(
                    "💰 *PINAC Masterclass Course Fees*:\n\n" +
                    "1️⃣ 3D Modeling & Texturing: *₹2,999*\n" +
                    "2️⃣ 3D Lighting & Rendering: *₹2,999*\n" +
                    "3️⃣ 3D Animation: *₹2,999*\n" +
                    "4️⃣ 3D Character Animation: *₹4,999*\n" +
                    "5️⃣ 3D Architecture: *₹3,999*\n" +
                    "6️⃣ Master's In Video Editing: *₹999*\n" +
                    "7️⃣ Master's In Graphic Design: *₹999*\n" +
                    "8️⃣ Basic VFX: *₹2,999*\n" +
                    "9️⃣ Advance VFX: *₹2,999*\n\n" +
                    "💳 *Payment Options*: UPI (GPay, PhonePe, Paytm), Credit/Debit Cards, Net Banking, and Instant EMI available via Razorpay.\n\n" +
                    "Reply with any course name for detailed syllabus!",
                    "FEE_ENQUIRY",
                    false
            );
        }

        // Batch Enquiry
        if (cleanedMsg.contains("batch") || cleanedMsg.contains("timing") || cleanedMsg.contains("schedule") || cleanedMsg.contains("next batch")) {
            return new AIResponseResult(
                    "📅 *Upcoming Batches & Schedule*:\n\n" +
                    "• *Self-Paced Recorded Modules*: Instant 24/7 Access right upon enrollment.\n" +
                    "• *Live Doubt Clearance & Mentorship*: Every Saturday & Sunday (6:00 PM – 8:00 PM IST).\n" +
                    "• *New Live Cohort Batch*: Starts on 1st & 15th of every month.\n" +
                    "• *Mode*: 100% Online with Lifetime Video Access + Offline Studio Workshops at Nashik campus.\n\n" +
                    "Would you like to reserve your seat in the upcoming batch?",
                    "BATCH_ENQUIRY",
                    false
            );
        }

        // Admission Process Enquiry
        if (cleanedMsg.contains("admission") || cleanedMsg.contains("how to join") || cleanedMsg.contains("how can i take") || cleanedMsg.contains("enroll") || cleanedMsg.contains("register")) {
            return new AIResponseResult(
                    "📝 *Step-by-Step Admission Process*:\n\n" +
                    "1️⃣ *Step 1*: Visit our website: https://pinacxtreme.com\n" +
                    "2️⃣ *Step 2*: Click on *Sign Up / Login* with your mobile number & OTP.\n" +
                    "3️⃣ *Step 3*: Browse our courses & click *Buy Now* on your chosen course.\n" +
                    "4️⃣ *Step 4*: Complete the secure payment via UPI, Card, or Net Banking.\n" +
                    "5️⃣ *Step 5*: Instant LMS Dashboard activation! Access all video modules, software project files, and student community immediately.\n\n" +
                    "Need assistance? Reply *'Connect me to sir'* to get personal admission guidance!",
                    "ADMISSION_ENQUIRY",
                    false
            );
        }

        // Institute Location & Contact
        if (cleanedMsg.contains("location") || cleanedMsg.contains("address") || cleanedMsg.contains("where") || cleanedMsg.contains("contact") || cleanedMsg.contains("office")) {
            return new AIResponseResult(
                    "📍 *PINAC Animation & VFX Academy*\n\n" +
                    "🏢 *Address*: PINAC Animation & VFX Academy, College Road / Gangapur Road, Nashik, Maharashtra 422005, India.\n" +
                    "📞 *Phone / WhatsApp*: +91 72191 94211 / +91 96047 32429\n" +
                    "📧 *Email*: support@pinacinstitute.com\n" +
                    "🌐 *Website*: https://pinacxtreme.com\n" +
                    "⏰ *Office Timings*: Monday to Saturday (10:00 AM – 7:00 PM IST)\n" +
                    "🗺️ *Google Maps*: https://maps.google.com/?q=PINAC+Animation+Academy+Nashik",
                    "LOCATION_ENQUIRY",
                    false
            );
        }

        // Placement & Career
        if (cleanedMsg.contains("placement") || cleanedMsg.contains("job") || cleanedMsg.contains("career") || cleanedMsg.contains("certificate")) {
            return new AIResponseResult(
                    "🏆 *Placement & Certification Support*:\n\n" +
                    "• *Industry-Recognized Certificate*: Verified QR-code digital certificate issued on course completion.\n" +
                    "• *100% Portfolio & Showreel Mentorship*: Direct 1-on-1 feedback from Pankaj Patil sir.\n" +
                    "• *Placement Assistance*: Direct referrals to Animation Studios, VFX Houses, Gaming Companies, and Advertising Agencies across Mumbai, Pune, Hyderabad, and Bangalore.\n" +
                    "• *Freelancing Guide*: Learn how to land international clients on Upwork, Fiverr, and Behance.",
                    "PLACEMENT_ENQUIRY",
                    false
            );
        }

        // Demo Class
        if (cleanedMsg.contains("demo") || cleanedMsg.contains("trial") || cleanedMsg.contains("sample")) {
            return new AIResponseResult(
                    "🎥 *Free Demo Lectures & Curriculum Walkthrough*:\n\n" +
                    "You can watch free previews of all courses on our website & YouTube channel!\n\n" +
                    "📺 *YouTube Channel*: https://youtube.com/@pinacanimation\n" +
                    "🌐 *Website Previews*: https://pinacxtreme.com/courses\n\n" +
                    "Which course demo are you interested in?",
                    "DEMO_ENQUIRY",
                    false
            );
        }

        // Software Requirements
        if (cleanedMsg.contains("software") || cleanedMsg.contains("laptop") || cleanedMsg.contains("pc") || cleanedMsg.contains("system requirement")) {
            return new AIResponseResult(
                    "💻 *Recommended System Requirements*:\n\n" +
                    "• *Processor*: Intel Core i5 / AMD Ryzen 5 or above\n" +
                    "• *RAM*: 16 GB Recommended (8 GB minimum)\n" +
                    "• *Graphics Card*: Dedicated NVIDIA GTX 1650 / RTX series (4GB+ VRAM)\n" +
                    "• *Storage*: 512 GB SSD recommended\n" +
                    "• *OS*: Windows 10/11 64-bit or macOS\n\n" +
                    "We provide complete software installation guidance and student license links upon enrollment!",
                    "SOFTWARE_ENQUIRY",
                    false
            );
        }

        // Fallback for Unknown Query
        return new AIResponseResult(
                "I couldn't find the exact information. Your message has been forwarded to our support team. They will contact you shortly. 🤝\n\nIn the meantime, feel free to explore all our certified courses at: https://pinacxtreme.com",
                "UNKNOWN",
                false
        );
    }

    /**
     * Call Google Gemini API with comprehensive prompt
     */
    private String callGeminiAI(String userQuery, String userName, String apiKey, String model) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        String systemPrompt = "You are the official AI Counselor and WhatsApp Assistant for PINAC Institute (PINACXTREME - Premier 3D Animation, VFX & Creative Media Academy led by Founder Pankaj Patil in Nashik, Maharashtra).\n" +
                "Your role is to assist prospective and current students 24/7 in a friendly, polite, highly professional, and encouraging manner.\n\n" +
                "Key Institute Facts:\n" +
                "- Founder & Lead Mentor: Pankaj Patil (12+ years experience in 3D, VFX, and Animation).\n" +
                "- Courses: 3D Modeling & Texturing (₹2999), 3D Lighting & Rendering (₹2999), 3D Animation (₹2999), 3D Character Animation (₹4999), 3D Architecture (₹3999), Master's In Video Editing (₹999), Master's In Graphic Design (₹999), Basic VFX (₹2999), Advance VFX (₹2999).\n" +
                "- Format: 100% online self-paced modules + weekend live Q&A masterclasses + offline studio workshops.\n" +
                "- Contact: +91 72191 94211 / support@pinacinstitute.com / Website: https://pinacxtreme.com.\n" +
                "- Office Address: PINAC Animation & VFX Academy, Nashik, Maharashtra.\n\n" +
                "Guidelines:\n" +
                "1. Keep responses clear, concise, well-formatted with emojis and bullet points suitable for WhatsApp reading.\n" +
                "2. Include website links and actionable next steps.\n" +
                "3. If user asks to talk to human/sir/admin, reassure them their request is forwarded to support.\n" +
                "4. If unknown, state gracefully that the query is forwarded to our mentors.";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", systemPrompt + "\n\nStudent (" + (userName != null ? userName : "Student") + ") says: " + userQuery)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 800
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map body = response.getBody();
            List candidates = (List) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map candidate = (Map) candidates.get(0);
                Map content = (Map) candidate.get("content");
                if (content != null) {
                    List parts = (List) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map part = (Map) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
        }

        return null;
    }

    private double calculateSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) return 1.0;
        int editDistance = getLevenshteinDistance(longer, shorter);
        return (longerLength - editDistance) / (double) longerLength;
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
}
