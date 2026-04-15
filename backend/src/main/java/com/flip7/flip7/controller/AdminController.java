package com.flip7.flip7.controller;

import com.flip7.flip7.dto.*;
import com.flip7.flip7.entity.GameHistory;
import com.flip7.flip7.repository.GameHistoryRepository;
import com.flip7.flip7.repository.RoomRepository;
import com.flip7.flip7.repository.UserRepository;
import com.flip7.flip7.service.RoomService;
import com.flip7.flip7.service.UserService;
import com.flip7.flip7.entity.Room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(originPatterns = "*")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    // ─── Helper ───────────────────────────────────────────────────────────────

    private String extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token manquant");
        }
        return authHeader.substring(7).trim();
    }

    private void requireAdmin(String authHeader) {
        String userId = extractUserId(authHeader);
        if (!userService.isAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs");
        }
    }

    private void requireSuperAdmin(String authHeader) {
        String userId = extractUserId(authHeader);
        if (!userService.isSuperAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé au super-admin");
        }
    }

    // ─── Dashboard stats ──────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats(
            @RequestHeader("Authorization") String auth) {
        requireAdmin(auth);

        long totalUsers = userRepository.count();
        long totalRooms = roomRepository.count();
        long activeRooms = roomRepository.findByStatus(Room.RoomStatus.IN_GAME).size();
        long totalGames = gameHistoryRepository.count();
        long gamesLast7Days = gameHistoryRepository
            .findByStartedAtBetween(LocalDateTime.now().minusDays(7), LocalDateTime.now())
            .size();

        return ResponseEntity.ok(new AdminStatsDTO(totalUsers, totalRooms, activeRooms, totalGames, gamesLast7Days));
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<UserAdminDTO>> listUsers(
            @RequestHeader("Authorization") String auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(userService.getAllUsersForAdmin());
    }

    @PutMapping("/users/{targetId}/role")
    public ResponseEntity<Map<String, String>> updateRole(
            @RequestHeader("Authorization") String auth,
            @PathVariable String targetId,
            @RequestBody Map<String, String> body) {
        String requesterId = extractUserId(auth);
        userService.updateRole(targetId, body.get("role"), requesterId);
        return ResponseEntity.ok(Map.of("message", "Rôle mis à jour"));
    }

    @PutMapping("/users/{targetId}/password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestHeader("Authorization") String auth,
            @PathVariable String targetId,
            @RequestBody Map<String, String> body) {
        String requesterId = extractUserId(auth);
        userService.adminResetPassword(targetId, body.get("newPassword"), requesterId);
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé"));
    }

    @DeleteMapping("/users/{targetId}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @RequestHeader("Authorization") String auth,
            @PathVariable String targetId) {
        String requesterId = extractUserId(auth);
        userService.adminDeleteUser(targetId, requesterId);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }

    // ─── Rooms ────────────────────────────────────────────────────────────────

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomAdminDTO>> listRooms(
            @RequestHeader("Authorization") String auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(roomService.getAllRoomsForAdmin());
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, String>> deleteRoom(
            @RequestHeader("Authorization") String auth,
            @PathVariable String roomId) {
        requireAdmin(auth);
        roomService.adminDeleteRoom(roomId);
        return ResponseEntity.ok(Map.of("message", "Room supprimée"));
    }

    // ─── History ──────────────────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<List<GameHistoryAdminDTO>> listHistory(
            @RequestHeader("Authorization") String auth) {
        requireAdmin(auth);
        List<GameHistoryAdminDTO> list = gameHistoryRepository.findAll().stream()
            .map(h -> new GameHistoryAdminDTO(
                h.getId(),
                h.getRoomId(),
                h.getStatus() != null ? h.getStatus().name() : "UNKNOWN",
                h.getWinnerId(),
                h.getStartedAt(),
                h.getEndedAt(),
                h.getTotalRounds()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/history/{gameId}")
    public ResponseEntity<Map<String, String>> deleteHistory(
            @RequestHeader("Authorization") String auth,
            @PathVariable String gameId) {
        requireAdmin(auth);
        if (!gameHistoryRepository.existsById(gameId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Historique non trouvé");
        }
        gameHistoryRepository.deleteById(gameId);
        return ResponseEntity.ok(Map.of("message", "Historique supprimé"));
    }

    @DeleteMapping("/history/room/{roomId}")
    public ResponseEntity<Map<String, String>> deleteHistoryByRoom(
            @RequestHeader("Authorization") String auth,
            @PathVariable String roomId) {
        requireAdmin(auth);
        List<GameHistory> entries = gameHistoryRepository.findByRoomId(roomId);
        gameHistoryRepository.deleteAll(entries);
        return ResponseEntity.ok(Map.of("message", entries.size() + " entrée(s) supprimée(s)"));
    }
}
