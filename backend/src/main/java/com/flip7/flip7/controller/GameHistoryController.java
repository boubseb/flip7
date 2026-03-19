package com.flip7.flip7.controller;

import com.flip7.flip7.entity.GameHistory;
import com.flip7.flip7.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour l'historique des parties
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(originPatterns = "*")
public class GameHistoryController {

    @Autowired
    private GameService gameService;

    /**
     * Récupère l'historique des parties d'une room
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomHistory(@PathVariable String roomId) {
        try {
            List<GameHistory> history = gameService.getRoomGameHistory(roomId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère l'historique des parties d'un joueur
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getPlayerHistory(@PathVariable String playerId) {
        try {
            List<GameHistory> history = gameService.getPlayerGameHistory(playerId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère l'historique des parties du joueur connecté
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyHistory(@RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            List<GameHistory> history = gameService.getPlayerGameHistory(userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère les statistiques d'un joueur
     */
    @GetMapping("/player/{playerId}/stats")
    public ResponseEntity<?> getPlayerStats(@PathVariable String playerId) {
        try {
            GameService.PlayerStats stats = gameService.getPlayerStats(playerId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère les statistiques du joueur connecté
     */
    @GetMapping("/me/stats")
    public ResponseEntity<?> getMyStats(@RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            GameService.PlayerStats stats = gameService.getPlayerStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
