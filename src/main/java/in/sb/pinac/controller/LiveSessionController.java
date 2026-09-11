package in.sb.pinac.controller;

import in.sb.pinac.dto.LiveSessionDTO;
import in.sb.pinac.entity.User;
import in.sb.pinac.service.LiveSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/live-sessions")
public class LiveSessionController {

    @Autowired
    private LiveSessionService liveSessionService;

    // GET SESSIONS FOR STUDENT (Auto-connected via course enrollments)
    @GetMapping("/my-sessions")
    public ResponseEntity<List<LiveSessionDTO>> getStudentSessions(
            @AuthenticationPrincipal User authUser,
            @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        return ResponseEntity.ok(liveSessionService.getSessionsForStudent(targetId));
    }

    // GET CURRENTLY LIVE COURSE (Requirement 8)
    @GetMapping("/current-live")
    public ResponseEntity<?> getCurrentLiveSession(
            @AuthenticationPrincipal User authUser,
            @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        LiveSessionDTO live = liveSessionService.getCurrentLiveSession(targetId);
        if (live != null) {
            return ResponseEntity.ok(Map.of("hasLive", true, "session", live));
        }
        return ResponseEntity.ok(Map.of("hasLive", false, "message", "No Live Session Available"));
    }

    // GET UPCOMING LIVE SESSIONS WITH COUNTDOWN (Requirement 9)
    @GetMapping("/upcoming")
    public ResponseEntity<List<LiveSessionDTO>> getUpcomingSessions(
            @AuthenticationPrincipal User authUser,
            @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        return ResponseEntity.ok(liveSessionService.getUpcomingSessions(targetId));
    }

    // GET COMPLETED SESSIONS WITH RECORDINGS & NOTES (Requirement 10 & 12)
    @GetMapping("/completed")
    public ResponseEntity<List<LiveSessionDTO>> getCompletedSessions(
            @AuthenticationPrincipal User authUser,
            @RequestParam(required = false) Long userId) {
        Long targetId = authUser != null ? authUser.getId() : userId;
        return ResponseEntity.ok(liveSessionService.getCompletedSessions(targetId));
    }
}
