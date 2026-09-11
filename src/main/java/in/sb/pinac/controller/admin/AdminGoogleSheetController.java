package in.sb.pinac.controller.admin;

import in.sb.pinac.service.admin.AdminGoogleSheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/google-sheets")
public class AdminGoogleSheetController {

    @Autowired
    private AdminGoogleSheetService adminGoogleSheetService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(adminGoogleSheetService.getStatus());
    }

    @PostMapping("/sync-all")
    public ResponseEntity<?> triggerSyncAll() {
        try {
            return ResponseEntity.ok(adminGoogleSheetService.triggerSyncAll());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
