package in.sb.pinac.controller.admin;

import in.sb.pinac.dto.LiveSessionDTO;
import in.sb.pinac.service.LiveSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/live-sessions")
public class AdminLiveSessionController {

    @Autowired
    private LiveSessionService liveSessionService;

    // GET ALL LIVE SESSIONS (Search, Status, Course filter)
    @GetMapping
    public ResponseEntity<List<LiveSessionDTO>> getAllLiveSessions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId) {
        return ResponseEntity.ok(liveSessionService.getAllSessions(search, status, courseId));
    }

    // GET SUMMARY METRICS (Total, Live Now, Upcoming, Completed)
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLiveSessionStats() {
        return ResponseEntity.ok(liveSessionService.getSessionStats());
    }

    // GET SINGLE SESSION
    @GetMapping("/{id}")
    public ResponseEntity<?> getLiveSessionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(liveSessionService.getSessionById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // CREATE LIVE SESSION (Auto-connected to enrolled students)
    @PostMapping
    public ResponseEntity<?> createLiveSession(@RequestBody LiveSessionDTO dto) {
        try {
            LiveSessionDTO created = liveSessionService.createSession(dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Live session published successfully and auto-connected to enrolled students.",
                    "session", created
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // UPDATE LIVE SESSION
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLiveSession(
            @PathVariable Long id,
            @RequestBody LiveSessionDTO dto) {
        try {
            LiveSessionDTO updated = liveSessionService.updateSession(id, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Live session updated successfully.",
                    "session", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // DELETE LIVE SESSION
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLiveSession(@PathVariable Long id) {
        try {
            liveSessionService.deleteSession(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Live session deleted successfully."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // UPLOAD RECORDED LECTURE & NOTES
    @PostMapping("/{id}/recording")
    public ResponseEntity<?> uploadRecording(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String recordingUrl = body.get("recordingUrl");
            String notesPdfUrl = body.get("notesPdfUrl");
            String description = body.get("description");

            LiveSessionDTO updated = liveSessionService.uploadRecording(id, recordingUrl, notesPdfUrl, description);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Recording and lecture notes published successfully.",
                    "session", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
