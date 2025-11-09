package com.flip7.flip7.controller;

import com.flip7.flip7.game.card.Card;
import com.flip7.flip7.game.card.NumberCard;
import com.flip7.flip7.game.card.OperatorCard;
import com.flip7.flip7.game.card.SpecialCard;
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
@CrossOrigin(originPatterns = "*")
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
            response.put("needsStopAssignment", result.isNeedsStopAssignment());

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
     * Assigner une carte Stop à un joueur (après pioche)
     */
    @PostMapping("/{roomId}/assign-stop")
    public ResponseEntity<?> assignStopCard(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token,
            @RequestBody AssignStopCardRequest request) {
        try {
            String userId = token.replace("Bearer ", "");
            Game.ActionResult result = gameService.assignStopCard(
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
     * Assigner une carte DrawThree (+3) à un joueur (après pioche)
     */
    @PostMapping("/{roomId}/assign-draw-three")
    public ResponseEntity<?> assignDrawThreeCard(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token,
            @RequestBody AssignDrawThreeCardRequest request) {
        try {
            String userId = token.replace("Bearer ", "");
            Game.ActionResult result = gameService.assignDrawThreeCard(
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
     * Démarrer le prochain round (appelé par le joueur actif)
     */
    @PostMapping("/{roomId}/start-next-round")
    public ResponseEntity<?> startNextRound(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            String message = gameService.startNextRound(roomId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);

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
            System.out.println("🔄 REST getGameState called for room: " + roomId + " by user: " + userId);
            
            Game game = gameService.getGame(roomId);
            
            if (game == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Partie non trouvée"));
            }

            // Utiliser createGameStateDTO pour avoir EXACTEMENT les mêmes données que le WebSocket
            // y compris pendingSpecialCards
            GameService.GameStateDTO dto = gameService.createGameStateDTO(game);
            
            return ResponseEntity.ok(dto);
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
        dto.put("cardType", card.getCardType().toString());
        dto.put("cancelled", card.isCancelled());
        
        if (card instanceof NumberCard) {
            NumberCard numCard = (NumberCard) card;
            dto.put("value", numCard.getValue());
            dto.put("special", false);
        } else if (card instanceof OperatorCard) {
            OperatorCard opCard = (OperatorCard) card;
            dto.put("operator", opCard.getOperatorType().toString());
            dto.put("special", false);
        } else if (card instanceof SpecialCard) {
            SpecialCard specCard = (SpecialCard) card;
            dto.put("specialType", specCard.getSpecialType().toString());
            dto.put("special", true);
            dto.put("used", specCard.isUsed());
            dto.put("pending", specCard.isPending());
            if (specCard.getAssignedToPlayerId() != null) {
                dto.put("assignedToPlayerId", specCard.getAssignedToPlayerId());
            }
        }

        return dto;
    }

    // DTOs de requête/réponse
    public static class AssignStopCardRequest {
        private String cardId;
        private String targetPlayerId;

        public String getCardId() { return cardId; }
        public void setCardId(String cardId) { this.cardId = cardId; }
        public String getTargetPlayerId() { return targetPlayerId; }
        public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }
    }

    public static class AssignDrawThreeCardRequest {
        private String cardId;
        private String targetPlayerId;

        public String getCardId() { return cardId; }
        public void setCardId(String cardId) { this.cardId = cardId; }
        public String getTargetPlayerId() { return targetPlayerId; }
        public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }
    }

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
        private String winnerId;
        private List<PlayerInfo> players = new java.util.ArrayList<>();

        public String getGameState() { return gameState; }
        public void setGameState(String gameState) { this.gameState = gameState; }
        public int getRoundNumber() { return roundNumber; }
        public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
        public int getCurrentPlayerIndex() { return currentPlayerIndex; }
        public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
        public int getRemainingCards() { return remainingCards; }
        public void setRemainingCards(int remainingCards) { this.remainingCards = remainingCards; }
        public String getWinnerId() { return winnerId; }
        public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
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

    /**
     * Redémarre une nouvelle partie dans la même room
     * Sauvegarde la partie actuelle et en crée une nouvelle
     */
    @PostMapping("/{roomId}/restart-game")
    public ResponseEntity<?> restartGame(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = token.replace("Bearer ", "");
            gameService.restartGame(roomId, userId);
            return ResponseEntity.ok(Map.of("message", "Nouvelle partie créée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ENDPOINT DE TEST - Force une distribution avec carte Stop au 2ème joueur
     * Usage: POST /api/game/{roomId}/test/distribution-stop
     */
    @PostMapping("/{roomId}/test/distribution-stop")
    public ResponseEntity<?> testDistributionWithStop(
            @PathVariable String roomId,
            @RequestHeader("Authorization") String token) {
        try {
            gameService.testDistributionWithStopCard(roomId);
            return ResponseEntity.ok(Map.of("message", "Test: Distribution avec carte Stop au 2ème joueur"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
