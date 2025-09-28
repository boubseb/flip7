package com.flip7.server.controller;

import com.flip7.server.model.GameRoom;
import com.flip7.server.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomRepository roomRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public List<GameRoom> list() {
        return roomRepository.findAll();
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String,String> body) {
        String name = body.get("name");
        String password = body.get("password");
        if (name == null) return ResponseEntity.badRequest().body(Map.of("error","name required"));
        if (roomRepository.findByName(name).isPresent()) return ResponseEntity.status(409).body(Map.of("error","name taken"));
        GameRoom r = new GameRoom();
        r.setName(name);
        if (password != null && !password.isBlank()) r.setPasswordHash(passwordEncoder.encode(password));
        roomRepository.save(r);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody Map<String,String> body) {
        String name = body.get("name");
        String password = body.get("password");
        if (name == null) return ResponseEntity.badRequest().body(Map.of("error","name required"));
        Optional<GameRoom> opt = roomRepository.findByName(name);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error","not found"));
        GameRoom r = opt.get();
        if (r.getPasswordHash() != null) {
            if (password == null || !passwordEncoder.matches(password, r.getPasswordHash())) return ResponseEntity.status(401).body(Map.of("error","wrong password"));
        }
        return ResponseEntity.ok(r);
    }
}
