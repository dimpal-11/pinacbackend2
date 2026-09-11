package in.sb.pinac.controller.admin;

import in.sb.pinac.service.admin.AdminPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    @Autowired
    private AdminPaymentService adminPaymentService;

    // GET ALL PAYMENTS
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(adminPaymentService.getAllPayments(search, status, startDate, endDate));
    }

    // GET PAYMENT BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminPaymentService.getPaymentById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
