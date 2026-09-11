package in.sb.pinac.controller.admin;

import in.sb.pinac.dto.admin.WebsiteSettingDto;
import in.sb.pinac.entity.WebsiteSetting;
import in.sb.pinac.service.admin.AdminSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingController {

    @Autowired
    private AdminSettingService adminSettingService;

    @GetMapping
    public ResponseEntity<WebsiteSetting> getSettings() {
        return ResponseEntity.ok(adminSettingService.getSettings());
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody WebsiteSettingDto dto) {
        try {
            WebsiteSetting updated = adminSettingService.updateSettings(dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
