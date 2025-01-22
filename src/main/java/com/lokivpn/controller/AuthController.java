package com.lokivpn.controller;

import com.lokivpn.repository.AdminRepository;
import com.lokivpn.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password must be provided."));
        }

        return adminRepository.findByUsername(username)
                .map(admin -> {
                    if (passwordEncoder.matches(password, admin.getPassword())) {
                        // Генерация токена
                        String token = jwtService.generateToken(admin.getUsername());
                        return ResponseEntity.ok(Map.of("token", token));
                    } else {
                        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials."));
                    }
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Admin not found.")));
    }
}
