package in.sb.pinac.controller;

import in.sb.pinac.entity.User;
import in.sb.pinac.repository.UserRepository;
import in.sb.pinac.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    // REGISTER / CREATE USER
    @PostMapping
    public ResponseEntity<Map<String, Object>> addUser(@RequestBody User user) {
        Map<String, Object> reg = authService.registerUser(
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getPasswordHash(),
                user.getRole()
        );
        return ResponseEntity.ok(reg);
    }

    // LOGIN OR LOCATE USER BY MOBILE OR EMAIL
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier");
        String mobile = body.get("mobile");
        String email = body.get("email");
        String password = body.get("password");
        String otp = body.get("otp");

        String target = identifier != null ? identifier : (email != null ? email : mobile);

        try {
            Map<String, Object> result = authService.login(target, password, otp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // GET ALL USERS
    @GetMapping
    public Iterable<User> getUsers() {
        return userRepository.findAll();
    }

    // GET USER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET USER BY EMAIL
    @GetMapping("/by-email")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET USER BY MOBILE
    @GetMapping("/by-mobile")
    public ResponseEntity<User> getUserByMobile(@RequestParam String mobile) {
        return userRepository.findByMobile(mobile.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}