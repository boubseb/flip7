package com.flip7.flip7.controller;

import com.flip7.flip7.game.card.Card;
import com.flip7.flip7.game.model.Game;
import com.flip7.flip7.game.model.GamePlayer;
import com.flip7.flip7.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la logique du jeu Flip7
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {

    @Autowired
    private GameService gameService;

    /**
     * Démarre un nouveau round
     */
    @PostMapping("/{roomId}/start-round")
    public ResponseEntity<?> startRound(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            gameService.startNewRound(roomId);
            return ResponseEntity.ok(Map.of("message", "Round démarré"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Un joueur pioche une carte
     */
    @PostMapping("/{roomId}/draw")
    public ResponseEntity<?> drawCard(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            Game.DrawResult result = gameService.drawCard(roomId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            if (result.getDrawnCard() != null) {
                response.put("card", mapCardToDTO(result.getDrawnCard()));
            }
            response.put("roundEnded", result.isRoundEnded());
            response.put("lifeUsed", result.isLifeUsed());
            response.put("eliminated", result.isEliminated());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Un joueur décide de s'arrêter
     */
    @PostMapping("/{roomId}/stop")
    public ResponseEntity<?> stopDrawing(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            Game.ActionResult result = gameService.stopDrawing(roomId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Un joueur joue une carte spéciale
     */
    @PostMapping("/{roomId}/play-special")
    public ResponseEntity<?> playSpecialCard(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token,
            @RequestBody PlaySpecialCardRequest request) {
        try {
            String userId = token.replace("Bearer ", "");
            Game.ActionResult result = gameService.playSpecialCard(
                roomId, 
                userId, 
                request.getCardId(), 
                request.getTargetPlayerId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère l'état actuel de la partie
     */
    @GetMapping("/{roomId}/state")
    public ResponseEntity<?> getGameState(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            Game game = gameService.getGame(roomId);
            
            if (game == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Partie non trouvée"));
            }

            GameStateResponse response = new GameStateResponse();
            response.setGameState(game.getGameState().name());
            response.setRoundNumber(game.getRoundNumber());
            response.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
            response.setRemainingCards(game.getRemainingCards());

            // Ajouter les informations des joueurs
            for (GamePlayer player : game.getPlayers()) {
                PlayerInfo info = new PlayerInfo();
                info.setUserId(player.getUserId());
                info.setUsername(player.getUsername());
                info.setStatus(player.getStatus().name());
                info.setTotalScore(player.getTotalScore());
                info.setRoundScore(player.getRoundScore());
                
                // Si c'est le joueur actuel, inclure sa main complète
                if (player.getUserId().equals(userId)) {
                    info.setHand(player.getHand().stream()
                        .map(this::mapCardToDTO)
                        .collect(Collectors.toList()));
                } else {
                    // Pour les autres joueurs, seulement le nombre de cartes
                    info.setHandSize(player.getHandSize());
                }
                
                response.addPlayer(info);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Convertit une Card en DTO
     */
    private Map<String, Object> mapCardToDTO(Card card) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", card.getId());
        dto.put("type", card.getCardType().name());
        dto.put("displayName", card.getDisplayName());

        if (card instanceof com.flip7.flip7.game.card.NumberCard) {
            dto.put("value", ((com.flip7.flip7.game.card.NumberCard) card).getValue());
        } else if (card instanceof com.flip7.flip7.game.card.OperatorCard) {
            dto.put("operatorType", ((com.flip7.flip7.game.card.OperatorCard) card).getOperatorType().name());
        } else if (card instanceof com.flip7.flip7.game.card.SpecialCard) {
            dto.put("specialType", ((com.flip7.flip7.game.card.SpecialCard) card).getSpecialType().name());
        }

        return dto;
    }

    // DTOs de requête/réponse
    public static class PlaySpecialCardRequest {
        private String cardId;
        private String targetPlayerId;

        public String getCardId() { return cardId; }
        public void setCardId(String cardId) { this.cardId = cardId; }
        public String getTargetPlayerId() { return targetPlayerId; }
        public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }
    }

    public static class GameStateResponse {
        private String gameState;
        private int roundNumber;
        private int currentPlayerIndex;
        private int remainingCards;
        private List<PlayerInfo> players = new java.util.ArrayList<>();

        public String getGameState() { return gameState; }
        public void setGameState(String gameState) { this.gameState = gameState; }
        public int getRoundNumber() { return roundNumber; }
        public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
        public int getCurrentPlayerIndex() { return currentPlayerIndex; }
        public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
        public int getRemainingCards() { return remainingCards; }
        public void setRemainingCards(int remainingCards) { this.remainingCards = remainingCards; }
        public List<PlayerInfo> getPlayers() { return players; }
        public void addPlayer(PlayerInfo player) { this.players.add(player); }
    }

    public static class PlayerInfo {
        private String userId;
        private String username;
        private String status;
        private int totalScore;
        private int roundScore;
        private int handSize;
        private List<Map<String, Object>> hand;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public int getRoundScore() { return roundScore; }
        public void setRoundScore(int roundScore) { this.roundScore = roundScore; }
        public int getHandSize() { return handSize; }
        public void setHandSize(int handSize) { this.handSize = handSize; }
        public List<Map<String, Object>> getHand() { return hand; }
        public void setHand(List<Map<String, Object>> hand) { this.hand = hand; }
    }
    
    /**
     * Abandonne une partie
     */
    @PostMapping("/{roomId}/abandon")
    public ResponseEntity<?> abandonGame(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            gameService.abandonGame(roomId, reason);
            return ResponseEntity.ok(Map.of("message", "Partie abandonnée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Un joueur quitte la partie
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveGame(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            gameService.playerLeaveGame(roomId, userId);
            return ResponseEntity.ok(Map.of("message", "Vous avez quitté la partie"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
