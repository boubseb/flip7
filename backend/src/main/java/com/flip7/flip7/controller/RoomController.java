// ...existing code...
package com.flip7.flip7.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flip7.flip7.dto.RoomCreateRequest;
import com.flip7.flip7.dto.RoomJoinRequest;
import com.flip7.flip7.dto.RoomResponse;
import com.flip7.flip7.entity.Room;
import com.flip7.flip7.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    @PostMapping("/{roomId}/rejoin-after-restart")
    public ResponseEntity<RoomResponse> rejoinAfterRestart(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromAuth(authHeader);
        Room room = roomService.rejoinAfterRestart(roomId, userId);
        return ResponseEntity.ok(roomService.toRoomResponse(room));
    }
    
    @Autowired
    private RoomService roomService;
    
    @PostMapping("/create")
    public ResponseEntity<RoomResponse> createRoom(
            @RequestBody RoomCreateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        // Extract user ID from token (simplified - you should use proper token validation)
        String userId = extractUserIdFromAuth(authHeader);
        
        Room room = roomService.createRoom(request.getPassword(), userId, request.getMaxPlayers());
        
        RoomResponse response = roomService.toRoomResponse(room);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(
            @RequestBody RoomJoinRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserIdFromAuth(authHeader);
        
        Room room = roomService.joinRoom(request.getRoomId(), request.getPassword(), userId);
        
        return ResponseEntity.ok(roomService.toRoomResponse(room));
    }
    
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        return ResponseEntity.ok(roomService.toRoomResponse(room));
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms() {
        List<RoomResponse> rooms = roomService.getAvailableRooms()
            .stream()
            .map(roomService::toRoomResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(rooms);
    }
    
    @PostMapping("/{roomId}/start")
    public ResponseEntity<RoomResponse> startGame(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserIdFromAuth(authHeader);
        
        Room room = roomService.startGame(roomId, userId);
        
        return ResponseEntity.ok(roomService.toRoomResponse(room));
    }
    
    @PostMapping("/{roomId}/kick")
    public ResponseEntity<RoomResponse> kickPlayer(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {
        
        String adminId = extractUserIdFromAuth(authHeader);
        String playerToKick = request.get("playerId");
        
        Room room = roomService.kickPlayer(roomId, adminId, playerToKick);
        
        return ResponseEntity.ok(roomService.toRoomResponse(room));
    }
    
    @PostMapping("/{roomId}/play")
    public ResponseEntity<RoomResponse> playTurn(
            @PathVariable String roomId,
            @RequestBody Map<String, String> move,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserIdFromAuth(authHeader);
        String moveData = move.get("move");
        
        Room room = roomService.playTurn(roomId, userId, moveData);
        
        return ResponseEntity.ok(roomService.toRoomResponse(room));
    }
    
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllRooms() {
        int deletedCount = roomService.deleteAllRooms();
        Map<String, String> response = new HashMap<>();
        response.put("message", deletedCount + " room(s) supprimée(s)");
        return ResponseEntity.ok(response);
    }
    
    private String extractUserIdFromAuth(String authHeader) {
        // Simplified - replace with proper JWT token extraction
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}
