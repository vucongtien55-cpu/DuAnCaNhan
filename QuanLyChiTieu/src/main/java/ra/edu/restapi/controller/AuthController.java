package ra.edu.restapi.controller;


import ra.edu.restapi.dto.LoginRequest;
import ra.edu.restapi.model.User;
import ra.edu.restapi.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // In log ra màn hình kiểm tra cho chắc chắn
        System.out.println("Frontend gõ: " + loginRequest.getUsername() + " / " + loginRequest.getPassword());

        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // SO SÁNH CHỮ THÔ CƠ BẢN BẰNG EQUALS
            if (loginRequest.getPassword().equals(user.getPassword())) {
                user.setPassword(null); // Xóa pass trước khi trả về
                System.out.println("-> ĐĂNG NHẬP THÀNH CÔNG!");
                return ResponseEntity.ok(user);
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai tài khoản hoặc mật khẩu!");
    }
}