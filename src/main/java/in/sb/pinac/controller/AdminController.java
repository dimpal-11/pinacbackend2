package in.sb.pinac.controller;

import in.sb.pinac.entity.Coupon;
import in.sb.pinac.entity.User;
import in.sb.pinac.repository.CouponRepository;
import in.sb.pinac.repository.UserRepository;
import in.sb.pinac.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/legacy")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    // GET ALL USERS
    @GetMapping("/users")
    public List<User> getUsers() {
        return adminService.getAllUsers();
    }

    // TOGGLE USER ACTIVE STATUS
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setActive(!Boolean.TRUE.equals(user.getActive()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "active", user.getActive()));
    }

    // GET ALL COUPONS
    @GetMapping("/coupons")
    public List<Coupon> getCoupons() {
        return couponRepository.findAll();
    }

    // CREATE COUPON
    @PostMapping("/coupons")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        if (coupon.getActive() == null) coupon.setActive(true);
        Coupon saved = couponRepository.save(coupon);
        return ResponseEntity.ok(saved);
    }
}
