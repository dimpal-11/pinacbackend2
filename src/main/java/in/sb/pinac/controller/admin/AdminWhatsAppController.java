package in.sb.pinac.controller.admin;

import in.sb.pinac.dto.whatsapp.*;
import in.sb.pinac.entity.whatsapp.*;
import in.sb.pinac.service.whatsapp.WhatsAppAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/whatsapp")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminWhatsAppController {

    @Autowired
    private WhatsAppAdminService adminService;

    /**
     * 1. Analytics Summary & Insights
     */
    @GetMapping("/analytics")
    public ResponseEntity<WhatsAppAnalyticsDto> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    /**
     * 2. List & Search Conversations (Paginated)
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<WhatsAppConversation>> getConversations(
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getConversations(status, search, page, size));
    }

    /**
     * 3. Get Conversation Details with Full Message Thread History
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<WhatsAppConversationDetailDto> getConversationDetails(@PathVariable("id") Long id) {
        return ResponseEntity.ok(adminService.getConversationDetails(id));
    }

    /**
     * 4. Admin Manual Reply (Human Takeover)
     */
    @PostMapping("/conversations/{id}/reply")
    public ResponseEntity<WhatsAppMessage> sendAdminReply(
            @PathVariable("id") Long id,
            @RequestBody WhatsAppSendReplyRequest request) {
        return ResponseEntity.ok(adminService.sendAdminReply(id, request));
    }

    /**
     * 5. Update Conversation Status
     */
    @PatchMapping("/conversations/{id}/status")
    public ResponseEntity<WhatsAppConversation> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody WhatsAppStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateStatus(id, request.getStatus()));
    }

    /**
     * 6. FAQs Management
     */
    @GetMapping("/faqs")
    public ResponseEntity<List<WhatsAppFaq>> getAllFaqs() {
        return ResponseEntity.ok(adminService.getAllFaqs());
    }

    @PostMapping("/faqs")
    public ResponseEntity<WhatsAppFaq> createFaq(@RequestBody WhatsAppFaq faq) {
        return ResponseEntity.ok(adminService.createFaq(faq));
    }

    @PutMapping("/faqs/{id}")
    public ResponseEntity<WhatsAppFaq> updateFaq(@PathVariable("id") Long id, @RequestBody WhatsAppFaq faq) {
        return ResponseEntity.ok(adminService.updateFaq(id, faq));
    }

    @DeleteMapping("/faqs/{id}")
    public ResponseEntity<Map<String, Object>> deleteFaq(@PathVariable("id") Long id) {
        adminService.deleteFaq(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "FAQ deleted successfully"));
    }

    /**
     * 7. Knowledge Base Management
     */
    @GetMapping("/knowledge-base")
    public ResponseEntity<List<WhatsAppKnowledgeBase>> getAllKnowledgeBase() {
        return ResponseEntity.ok(adminService.getAllKnowledgeBase());
    }

    @PostMapping("/knowledge-base")
    public ResponseEntity<WhatsAppKnowledgeBase> createKnowledgeBase(@RequestBody WhatsAppKnowledgeBase kb) {
        return ResponseEntity.ok(adminService.createKnowledgeBase(kb));
    }

    @PutMapping("/knowledge-base/{id}")
    public ResponseEntity<WhatsAppKnowledgeBase> updateKnowledgeBase(@PathVariable("id") Long id, @RequestBody WhatsAppKnowledgeBase kb) {
        return ResponseEntity.ok(adminService.updateKnowledgeBase(id, kb));
    }

    @DeleteMapping("/knowledge-base/{id}")
    public ResponseEntity<Map<String, Object>> deleteKnowledgeBase(@PathVariable("id") Long id) {
        adminService.deleteKnowledgeBase(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Knowledge item deleted successfully"));
    }

    /**
     * 8. Support Tickets (Escalations)
     */
    @GetMapping("/tickets")
    public ResponseEntity<List<WhatsAppSupportTicket>> getTickets(
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status) {
        return ResponseEntity.ok(adminService.getAllTickets(status));
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<WhatsAppSupportTicket> updateTicketStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "CONTACTED");
        String adminNotes = body.get("adminNotes");
        return ResponseEntity.ok(adminService.updateTicketStatus(id, status, adminNotes));
    }

    /**
     * 9. Broadcast Campaigns
     */
    @GetMapping("/broadcasts")
    public ResponseEntity<List<WhatsAppBroadcast>> getBroadcasts() {
        return ResponseEntity.ok(adminService.getAllBroadcasts());
    }

    @PostMapping("/broadcasts")
    public ResponseEntity<WhatsAppBroadcast> createAndSendBroadcast(@RequestBody WhatsAppBroadcastRequest request) {
        WhatsAppBroadcast b = new WhatsAppBroadcast(
                request.getTitle(),
                request.getMessageTemplate(),
                request.getTargetAudience()
        );
        return ResponseEntity.ok(adminService.createAndSendBroadcast(b));
    }

    /**
     * 10. WhatsApp & AI Settings
     */
    @GetMapping("/settings")
    public ResponseEntity<WhatsAppSetting> getSettings() {
        return ResponseEntity.ok(adminService.getSettings());
    }

    @PostMapping("/settings")
    public ResponseEntity<WhatsAppSetting> updateSettings(@RequestBody WhatsAppSetting settings) {
        return ResponseEntity.ok(adminService.updateSettings(settings));
    }

    /**
     * 11. Export Chat Transcripts (CSV)
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportChats() {
        String csvData = adminService.exportChatHistoryCsv();
        byte[] bytes = csvData.getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "pinac_whatsapp_chat_history.csv");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
