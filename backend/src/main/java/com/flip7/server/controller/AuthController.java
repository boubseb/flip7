package com.flip7.server.controller;

import com.flip7.server.model.User;
import com.flip7.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        if (username == null || password == null) return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        if (userRepository.findByUsername(username).isPresent()) return ResponseEntity.status(409).body(Map.of("error", "username taken"));
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setEmail(email);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("message","ok"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error","invalid"));
        var user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) return ResponseEntity.status(401).body(Map.of("error","invalid"));
        // For simplicity return a mocked token (replace with JWT in production)
        return ResponseEntity.ok(Map.of("token", "fake-jwt-token", "userId", user.getId()));
    }
}
