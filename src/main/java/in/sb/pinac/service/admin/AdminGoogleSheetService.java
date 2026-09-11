package in.sb.pinac.service.admin;

import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.Payment;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.EnrollmentRepository;
import in.sb.pinac.repository.PaymentRepository;
import in.sb.pinac.repository.UserRepository;
import in.sb.pinac.service.GoogleSheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminGoogleSheetService {

    @Autowired
    private GoogleSheetService googleSheetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Value("${google.sheets.spreadsheet-id:}")
    private String spreadsheetId;

    @Value("${google.sheets.webhook-url:}")
    private String webhookUrl;

    public Map<String, Object> getStatus() {
        Map<String, Object> map = new HashMap<>();
        boolean configured = (spreadsheetId != null && !spreadsheetId.trim().isEmpty())
                || (webhookUrl != null && !webhookUrl.trim().isEmpty());

        map.put("configured", configured);
        map.put("spreadsheetId", spreadsheetId != null && !spreadsheetId.trim().isEmpty() ? spreadsheetId : "Configured via Service Account");
        map.put("sheetName", "Purchases");
        map.put("backupStatus", configured ? "ACTIVE_SYNC" : "STANDBY_MODE");
        map.put("lastSync", "Real-time auto sync on events");
        return map;
    }

    public Map<String, Object> triggerSyncAll() {
        return googleSheetService.syncAllFromDatabase();
    }
}
