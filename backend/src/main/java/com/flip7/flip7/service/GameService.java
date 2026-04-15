package com.flip7.flip7.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flip7.flip7.entity.GameHistory;
import com.flip7.flip7.entity.GameSnapshot;
import com.flip7.flip7.entity.Room;
import com.flip7.flip7.game.card.Card;
import com.flip7.flip7.game.card.NumberCard;
import com.flip7.flip7.game.card.OperatorCard;
import com.flip7.flip7.game.card.SpecialCard;
import com.flip7.flip7.entity.User;
import com.flip7.flip7.game.model.Game;
import com.flip7.flip7.game.model.GamePlayer;
import com.flip7.flip7.game.model.GameState;
import com.flip7.flip7.game.model.PlayerStatus;
import com.flip7.flip7.event.GameStartEvent;
import com.flip7.flip7.repository.GameHistoryRepository;
import com.flip7.flip7.repository.GameSnapshotRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service gérant la logique du jeu Flip7
 */
@Service
public class GameService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RoomService roomService;

    @Autowired
    private GameHistoryRepository gameHistoryRepository;
    
    @Autowired
    private GameSnapshotRepository gameSnapshotRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        // Do not fail when snapshot JSON contains older/extra properties
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    // Map des parties en cours (roomId -> Game)
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();
    
    // Map pour lier une partie en cours à son historique (roomId -> GameHistory.id)
    private final Map<String, String> gameHistoryIds = new ConcurrentHashMap<>();
    
    /**
     * Restaure les parties en cours depuis la base de données au démarrage du serveur
     */
    @PostConstruct
    public void restoreGamesFromDatabase() {
        System.out.println("🔄 Restauration des parties en cours depuis la base de données...");

        try {
            List<GameSnapshot> snapshots = gameSnapshotRepository.findByGameStatusIn(
                Arrays.asList("PLAYING", "WAITING_NEXT_ROUND", "DISTRIBUTING")
            );

            System.out.println("📊 Snapshots trouvés: " + snapshots.size());

            for (GameSnapshot snapshot : snapshots) {
                try {
                    System.out.println("🔄 Tentative de restauration pour room: " + snapshot.getRoomId());
                    Game game = objectMapper.readValue(snapshot.getGameStateJson(), Game.class);
                    activeGames.put(snapshot.getRoomId(), game);
                    System.out.println("✅ Partie restaurée pour room: " + snapshot.getRoomId() +
                                     " (round " + snapshot.getCurrentRound() + ")");

                    // Restaurer aussi le gameHistoryId pour que les stats continuent à être sauvegardées
                    List<GameHistory> histories = gameHistoryRepository.findByRoomId(snapshot.getRoomId());
                    GameHistory activeHistory = histories.stream()
                        .filter(h -> h.getStatus() == GameHistory.GameStatus.IN_PROGRESS)
                        .findFirst()
                        .orElse(null);
                    if (activeHistory != null) {
                        gameHistoryIds.put(snapshot.getRoomId(), activeHistory.getId());
                        System.out.println("✅ GameHistory ID restauré pour room: " + snapshot.getRoomId() + " → " + activeHistory.getId());
                    } else {
                        // Pas d'historique IN_PROGRESS : en créer un nouveau
                        GameHistory newHistory = new GameHistory(snapshot.getRoomId(), game.getPlayers().stream()
                            .map(p -> p.getUserId()).collect(java.util.stream.Collectors.toList()));
                        newHistory = gameHistoryRepository.save(newHistory);
                        gameHistoryIds.put(snapshot.getRoomId(), newHistory.getId());
                        System.out.println("✅ Nouveau GameHistory créé pour room restaurée: " + snapshot.getRoomId());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur lors de la restauration de la partie " +
                                     snapshot.getRoomId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("✅ " + activeGames.size() + " partie(s) restaurée(s)");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la restauration des parties: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sauvegarde l'état d'une partie en base de données
     */
    private void saveGameSnapshot(String roomId, Game game) {
        try {
            String gameJson = objectMapper.writeValueAsString(game);
            
            GameSnapshot snapshot = gameSnapshotRepository.findById(roomId)
                .orElse(new GameSnapshot(roomId, gameJson));
            
            snapshot.setGameStateJson(gameJson);
            snapshot.setCurrentRound(game.getRoundNumber());
            snapshot.setGameStatus(game.getGameState().toString());
            
            gameSnapshotRepository.save(snapshot);
            System.out.println("💾 Snapshot sauvegardé pour room " + roomId + " (round " + game.getRoundNumber() + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde snapshot pour " + roomId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Supprime le snapshot d'une partie terminée
     */
    private void deleteGameSnapshot(String roomId) {
        gameSnapshotRepository.deleteById(roomId);
    }

    /**
     * Initialise une nouvelle partie pour une room
     */
    public Game initializeGame(String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            throw new RuntimeException("Room not found");
        }

        // Récupérer les noms des joueurs
        Map<String, String> playerNames = new HashMap<>();
        for (String playerId : room.getPlayers()) {
            try {
                User user = roomService.getUserRepository()
                    .findById(playerId)
                    .orElse(null);
                if (user != null) {
                    String displayName = user.getPseudo(); // Utiliser le pseudo
                    playerNames.put(playerId, displayName);
                } else {
                    playerNames.put(playerId, "Player " + playerId.substring(0, Math.min(8, playerId.length())));
                }
            } catch (Exception e) {
                // Fallback si l'utilisateur n'est pas trouvé
                playerNames.put(playerId, "Player " + playerId.substring(0, Math.min(8, playerId.length())));
            }
        }

        int targetScore = room.getTargetScore() != null ? room.getTargetScore() : 200;
        boolean teamMode = room.isTeamMode();
        Map<String, Integer> teamAssignments = room.getTeamAssignments();
        boolean persistentDeck = room.isPersistentDeck();
        Game game = new Game(roomId, room.getPlayers(), playerNames, targetScore, teamMode, teamAssignments, persistentDeck);
        activeGames.put(roomId, game);
        
        // Créer un nouvel historique de partie
        GameHistory history = new GameHistory(roomId, room.getPlayers());
        history = gameHistoryRepository.save(history);
        gameHistoryIds.put(roomId, history.getId());
        
        return game;
    }

    /**
     * Listener pour démarrer automatiquement le jeu quand une room passe en IN_GAME
     */
    @EventListener
    public void onGameStart(GameStartEvent event) {
        try {
            System.out.println("🎮 GameStartEvent received for room: " + event.getRoomId());
            startNewRound(event.getRoomId());
            System.out.println("✅ Game initialized and first round started for room: " + event.getRoomId());
        } catch (Exception e) {
            System.err.println("❌ Error initializing game for room " + event.getRoomId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Démarre un nouveau round avec distribution progressive carte par carte
     */
    public void startNewRound(String roomId) {
        Game game = activeGames.get(roomId);
        if (game == null) {
            game = initializeGame(roomId);
        }

        game.startNewRound();
        
        // Sauvegarder le début du round dans l'historique
        saveRoundStart(roomId, game);

        // Démarrer la distribution initiale (1 carte par joueur)
        // La distribution s'interrompt si une carte spéciale est piochée
        System.out.println("🎴 Starting initial distribution...");
        continueDistributionWithBroadcast(roomId, game);
    }
    
    /**
     * Continue la distribution en appelant game.continueInitialDistribution()
     * et broadcaste l'état après chaque carte distribuée
     */
    private void continueDistributionWithBroadcast(String roomId, Game game) {
        System.out.println("📡 Distributing next card and broadcasting...");
        
        boolean shouldContinue = game.continueInitialDistribution();
        
        // Broadcaster l'état après la distribution de cette carte
        broadcastGameState(roomId, game);
        
        // Si la distribution doit continuer (pas de carte spéciale, pas terminé)
        if (shouldContinue) {
            System.out.println("   ✅ Card distributed, continuing...");
            continueDistributionWithBroadcast(roomId, game); // Récursion
        } else if (game.getGameState() == GameState.DISTRIBUTING) {
            System.out.println("   ⏸️  Distribution paused - waiting for special card assignment");
        } else {
            System.out.println("   ✅ Distribution complete - game started!");
        }
    }
    
    /**
     * Un joueur pioche une carte
     */
    public Game.DrawResult drawCard(String roomId, String playerId) {
        System.out.println("🎲 drawCard called - roomId: " + roomId + ", playerId: " + playerId);
        System.out.println("📊 Active games count: " + activeGames.size());
        System.out.println("🗂️ Active game rooms: " + activeGames.keySet());
        
        Game game = activeGames.get(roomId);
        if (game == null) {
            System.err.println("❌ Game not found for roomId: " + roomId);
            System.err.println("💡 Available rooms: " + activeGames.keySet());
            return new Game.DrawResult(false, "Partie non trouvée", null);
        }

        System.out.println("✅ Game found, calling game.drawCard()");
        Game.DrawResult result = game.drawCard(playerId);
        
        System.out.println("📊 DrawResult: success=" + result.isSuccess() + ", roundEnded=" + result.isRoundEnded());
        System.out.println("📊 Game state after drawCard: " + game.getGameState());
        System.out.println("📊 Is round over? " + game.isRoundOver());

        // Broadcaster l'état du jeu
        broadcastGameState(roomId, game);

        // Si le round est terminé, calculer les scores
        if (result.isRoundEnded() || game.isRoundOver()) {
            System.out.println("🔚 Round ended detected, calling handleRoundEnd()...");
            handleRoundEnd(roomId, game);
        } else {
            System.out.println("▶️ Round continues...");
        }

        return result;
    }

    /**
     * Un joueur décide de s'arrêter
     */
    public Game.ActionResult stopDrawing(String roomId, String playerId) {
        Game game = activeGames.get(roomId);
        if (game == null) {
            return new Game.ActionResult(false, "Partie non trouvée");
        }

        Game.ActionResult result = game.stopDrawing(playerId);

        // Broadcaster l'état du jeu
        broadcastGameState(roomId, game);

        // Si le round est terminé, calculer les scores
        if (game.isRoundOver()) {
            handleRoundEnd(roomId, game);
        }

        return result;
    }

    /**
     * Assigner une carte Stop à un joueur
     */
    public Game.ActionResult assignStopCard(String roomId, String playerId, String cardId, String targetPlayerId) {
        System.out.println("🛑 assignStopCard called - roomId: " + roomId + ", playerId: " + playerId + ", target: " + targetPlayerId);
        
        Game game = activeGames.get(roomId);
        if (game == null) {
            return new Game.ActionResult(false, "Partie non trouvée");
        }

        Game.ActionResult result = game.assignStopCard(playerId, cardId, targetPlayerId);

        // Broadcaster l'état du jeu
        broadcastGameState(roomId, game);

        // Si on est en mode DISTRIBUTING et qu'il n'y a plus de cartes spéciales en attente, reprendre la distribution
        if (game.getGameState() == GameState.DISTRIBUTING && game.getPendingSpecialCards().isEmpty()) {
            System.out.println("🎴 Reprise de la distribution après assignation STOP");
            continueDistributionWithBroadcast(roomId, game);
        }

        // Si le round est terminé, calculer les scores
        if (game.isRoundOver()) {
            handleRoundEnd(roomId, game);
        }

        return result;
    }

    /**
     * Assigner une carte DrawThree (+3) à un joueur
     */
    public Game.ActionResult assignDrawThreeCard(String roomId, String playerId, String cardId, String targetPlayerId) {
        System.out.println("➕3️⃣ assignDrawThreeCard called - roomId: " + roomId + ", playerId: " + playerId + ", target: " + targetPlayerId);
        
        Game game = activeGames.get(roomId);
        if (game == null) {
            return new Game.ActionResult(false, "Partie non trouvée");
        }

        Game.ActionResult result = game.assignDrawThreeCard(playerId, cardId, targetPlayerId);

        // Broadcaster l'état du jeu
        broadcastGameState(roomId, game);

        // Si on est en mode DISTRIBUTING et qu'il n'y a plus de cartes spéciales en attente, reprendre la distribution
        if (game.getGameState() == GameState.DISTRIBUTING && game.getPendingSpecialCards().isEmpty()) {
            System.out.println("🎴 Reprise de la distribution après assignation +3");
            continueDistributionWithBroadcast(roomId, game);
        }

        // Si le round est terminé, calculer les scores
        if (game.isRoundOver()) {
            handleRoundEnd(roomId, game);
        }

        return result;
    }

    /**
     * Assigner une carte Vie à un coéquipier (mode équipe uniquement)
     */
    public Game.ActionResult assignLifeCard(String roomId, String playerId, String cardId, String targetPlayerId) {
        System.out.println("💚 assignLifeCard called - roomId: " + roomId + ", playerId: " + playerId + ", target: " + targetPlayerId);
        Game game = activeGames.get(roomId);
        if (game == null) {
            return new Game.ActionResult(false, "Partie non trouvée");
        }
        Game.ActionResult result = game.assignLifeCard(playerId, cardId, targetPlayerId);
        broadcastGameState(roomId, game);
        if (game.isRoundOver()) {
            handleRoundEnd(roomId, game);
        }
        return result;
    }

    /**
     * Démarrer le prochain round
     */
    public String startNextRound(String roomId, String userId) {
        System.out.println("🚀 startNextRound called - roomId: " + roomId + ", userId: " + userId);
        
        Game game = activeGames.get(roomId);
        if (game == null) {
            throw new IllegalStateException("Partie non trouvée");
        }

        // Vérifier que le jeu est en attente du prochain round
        if (game.getGameState() != GameState.WAITING_NEXT_ROUND) {
            throw new IllegalStateException("Le jeu n'est pas en attente du prochain round");
        }

        GamePlayer currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null || !currentPlayer.getUserId().equals(userId)) {
            throw new IllegalStateException("Ce n'est pas à vous de démarrer le round");
        }

        // Appeler la méthode startNewRound sans distribution
        game.startNewRound(); // Initialise le round en mode DISTRIBUTING
        saveRoundStart(roomId, game); // Sauvegarde
        
        // Démarrer la distribution initiale
        System.out.println("🎴 Starting initial distribution for next round...");
        continueDistributionWithBroadcast(roomId, game);

        System.out.println("🎮 C'est au tour de " + currentPlayer.getUsername());
        return "Round " + game.getRoundNumber() + " démarré !";
    }

    /**
     * Un joueur joue une carte spéciale
     */
    public Game.ActionResult playSpecialCard(String roomId, String playerId, String cardId, String targetPlayerId) {
        Game game = activeGames.get(roomId);
        if (game == null) {
            return new Game.ActionResult(false, "Partie non trouvée");
        }

        Game.ActionResult result = game.playSpecialCard(playerId, cardId, targetPlayerId);

        // Broadcaster l'état du jeu
        broadcastGameState(roomId, game);

        // Si le round est terminé, calculer les scores
        if (game.isRoundOver()) {
            handleRoundEnd(roomId, game);
        }

        return result;
    }

    /**
     * Gère la fin d'un round
     */
    private void handleRoundEnd(String roomId, Game game) {
        // Sauvegarder la fin du round dans l'historique
        saveRoundEnd(roomId, game);
        
        // Broadcaster les scores finaux du round
        broadcastRoundEnd(roomId, game);

        // Si la partie est terminée (un joueur a atteint 200 points)
        if (game.isGameOver()) {
            System.out.println("🏆 Game Over! Winner: " + game.getWinner().getUsername());
            
            // Marquer la partie comme terminée dans l'historique
            saveGameEnd(roomId, game);
            
            broadcastGameOver(roomId, game);
            activeGames.remove(roomId);
            gameHistoryIds.remove(roomId);
        } else {
            // Personne n'a atteint 200 points, passer en attente du prochain round
            System.out.println("🔄 Round " + game.getRoundNumber() + " ended, no winner yet.");
            System.out.println("   Current scores:");
            for (GamePlayer player : game.getPlayers()) {
                System.out.println("   - " + player.getUsername() + ": " + player.getTotalScore() + " points");
            }
            System.out.println("⏳ Waiting for " + game.getCurrentPlayer().getUsername() + " to start round " + (game.getRoundNumber() + 1));
            
            // Broadcaster l'état WAITING_NEXT_ROUND pour afficher le popup
            broadcastGameState(roomId, game);
        }
    }

    /**
     * Récupère l'état d'une partie
     */
    public Game getGame(String roomId) {
        return activeGames.get(roomId);
    }
    
    /**
     * Abandonne une partie en cours
     */
    public void abandonGame(String roomId, String reason) {
        Game game = activeGames.get(roomId);
        if (game == null) {
            return;
        }
        
        // Marquer la partie comme abandonnée dans l'historique
        String historyId = gameHistoryIds.get(roomId);
        if (historyId != null) {
            GameHistory history = gameHistoryRepository.findById(historyId).orElse(null);
            if (history != null) {
                history.setStatus(GameHistory.GameStatus.ABANDONED);
                history.setEndedAt(LocalDateTime.now());
                
                // Sauvegarder les scores actuels même si la partie est abandonnée
                for (GamePlayer player : game.getPlayers()) {
                    GameHistory.PlayerScore score = new GameHistory.PlayerScore(
                        player.getUserId(),
                        player.getUsername(),
                        player.getTotalScore()
                    );
                    history.getFinalScores().add(score);
                }

                history.serializeJsonFields(); // Force serialization before merge
                gameHistoryRepository.save(history);
            }
        }

        // Supprimer la partie active
        activeGames.remove(roomId);
        gameHistoryIds.remove(roomId);
        
        // Broadcaster l'abandon
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomId + "/game-abandoned",
            Map.of("message", reason != null ? reason : "La partie a été abandonnée")
        );
    }
    
    /**
     * Un joueur quitte la partie
     */
    public void playerLeaveGame(String roomId, String playerId) {
        Game game = activeGames.get(roomId);
        if (game == null) {
            return;
        }
        
        // Si tous les joueurs ont quitté, abandonner la partie
        long remainingPlayers = game.getPlayers().stream()
            .filter(p -> !p.getUserId().equals(playerId))
            .count();
            
        if (remainingPlayers == 0) {
            abandonGame(roomId, "Tous les joueurs ont quitté la partie");
        } else if (remainingPlayers == 1) {
            // S'il ne reste qu'un joueur, on peut aussi abandonner
            abandonGame(roomId, "Pas assez de joueurs pour continuer");
        }
    }

    /**
     * Broadcaster l'état du jeu à tous les joueurs de la room
     */
    private void broadcastGameState(String roomId, Game game) {
        GameStateDTO dto = createGameStateDTO(game);
        System.out.println("📡 Broadcasting game state to /topic/rooms/" + roomId + "/game");
        System.out.println("   - Players count: " + dto.getPlayers().size());
        for (PlayerDTO player : dto.getPlayers()) {
            System.out.println("   - Player " + player.getUsername() + ": " + player.getHand().size() + " cards, score: " + player.getRoundScore());
        }
        System.out.println("   🔍 DTO.pendingSpecialCards size before send: " + dto.getPendingSpecialCards().size());
        if (!dto.getPendingSpecialCards().isEmpty()) {
            System.out.println("   🔍 First pending card: " + dto.getPendingSpecialCards().get(0));
        }
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/game", dto);
        
        // Sauvegarder le snapshot en BDD
        saveGameSnapshot(roomId, game);
    }

    /**
     * Broadcaster la fin d'un round
     */
    private void broadcastRoundEnd(String roomId, Game game) {
        RoundEndDTO dto = createRoundEndDTO(game);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/round-end", dto);
    }

    /**
     * Broadcaster la fin de la partie
     */
    private void broadcastGameOver(String roomId, Game game) {
    GameOverDTO dto = createGameOverDTO(game);
    // Ajout du flag waitingForAdmin pour le front
    dto.setWaitingForAdmin(true);
    messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/game-over", dto);
    }

    /**
     * Crée un DTO de l'état du jeu
     * PUBLIC pour être accessible depuis GameController
     */
    public GameStateDTO createGameStateDTO(Game game) {
    // Récupérer la room pour l'option statistiques
    Room room = roomService.getRoom(game.getRoomId());
    boolean statisticsEnabled = room != null && room.isStatisticsEnabled();
        System.out.println("\n🎮 === CREATE GAME STATE DTO ===");
        System.out.println("   Round: " + game.getRoundNumber());
        System.out.println("   Game State: " + game.getGameState());
        System.out.println("   Players count: " + game.getPlayers().size());
        
        GameStateDTO dto = new GameStateDTO();
        dto.setGameState(game.getGameState());
        dto.setRoundNumber(game.getRoundNumber());
        dto.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
        dto.setRemainingCards(game.getRemainingCards());
    dto.setStatisticsEnabled(statisticsEnabled);
        
        // Convertir les joueurs
        for (GamePlayer player : game.getPlayers()) {
            System.out.println("\n   👤 Player: " + player.getUsername() + " (ID: " + player.getUserId() + ")");
            System.out.println("      Round Score: " + player.getRoundScore());
            System.out.println("      Total Score (sécurité): " + player.getTotalScore());
            System.out.println("      Theoretical Total: " + (player.getTotalScore() + player.getRoundScore()));
            System.out.println("      Status: " + player.getStatus());
            System.out.println("      Hand Size: " + player.getHandSize());
            System.out.println("      Life Cards: " + player.getLifeCardsInHand());
            
            PlayerDTO playerDTO = new PlayerDTO();
            playerDTO.setUserId(player.getUserId());
            playerDTO.setUsername(player.getUsername());
            playerDTO.setStatus(player.getStatus());
            playerDTO.setHandSize(player.getHandSize());
            playerDTO.setRoundScore(player.getRoundScore());
            playerDTO.setTotalScore(player.getTotalScore());
            playerDTO.setTheoreticalTotal(player.getTotalScore() + player.getRoundScore());
            playerDTO.setLifeCardsInHand(player.getLifeCardsInHand());
            playerDTO.setTeamId(player.getTeamId());
            playerDTO.setStoppedByUsername(player.getStoppedByUsername());
            playerDTO.setDrawThreeByUsername(player.getDrawThreeByUsername());

            // Ajouter les cartes de la main (révélées)
            java.util.List<java.util.Map<String, Object>> handCards = new java.util.ArrayList<>();
            System.out.println("      🃏 Player " + player.getUsername() + " has " + player.getHand().size() + " cards in hand");
            for (Card card : player.getHand()) {
                java.util.Map<String, Object> cardMap = new java.util.HashMap<>();
                cardMap.put("id", card.getId()); // 🔑 AJOUT DE L'ID DE LA CARTE
                cardMap.put("cardType", card.getCardType().toString());
                cardMap.put("cancelled", card.isCancelled());
                
                if (card instanceof NumberCard) {
                    NumberCard numCard = (NumberCard) card;
                    cardMap.put("value", numCard.getValue());
                    cardMap.put("special", false);
                    System.out.println("         - NumberCard: " + numCard.getValue() + " (ID: " + card.getId() + ")" + (card.isCancelled() ? " (barrée)" : ""));
                } else if (card instanceof OperatorCard) {
                    OperatorCard opCard = (OperatorCard) card;
                    cardMap.put("operator", opCard.getOperatorType().toString());
                    cardMap.put("special", false);
                    System.out.println("         - OperatorCard: " + opCard.getOperatorType() + " (ID: " + card.getId() + ")");
                } else if (card instanceof SpecialCard) {
                    SpecialCard specCard = (SpecialCard) card;
                    cardMap.put("specialType", specCard.getSpecialType().toString());
                    cardMap.put("special", true);
                    cardMap.put("used", specCard.isUsed());
                    cardMap.put("pending", specCard.isPending());
                    System.out.println("         - SpecialCard: " + specCard.getSpecialType() + " (ID: " + card.getId() + ")" + (specCard.isUsed() ? " (utilisée)" : "") + (specCard.isPending() ? " (pending)" : ""));
                }
                
                handCards.add(cardMap);
            }
            playerDTO.setHand(handCards);
            System.out.println("      ✅ PlayerDTO.hand size: " + playerDTO.getHand().size());
            
            // Ajouter la liste des rounds du joueur
            java.util.List<RoundDTO> roundDTOs = new java.util.ArrayList<>();
            for (com.flip7.flip7.game.model.GamePlayer.RoundData roundData : player.getRounds()) {
                RoundDTO roundDTO = new RoundDTO();
                roundDTO.setRoundNumber(roundData.getRoundNumber());
                roundDTO.setRoundScore(roundData.getRoundScore());
                roundDTO.setTotalScore(roundData.getTotalScore());
                roundDTO.setTheoreticalTotal(roundData.getTheoreticalTotal());
                roundDTO.setStatus(roundData.getStatus());
                
                // Convertir les cartes du round
                java.util.List<java.util.Map<String, Object>> roundHandCards = new java.util.ArrayList<>();
                for (Card card : roundData.getHand()) {
                    java.util.Map<String, Object> cardMap = new java.util.HashMap<>();
                    cardMap.put("cardType", card.getCardType().toString());
                    cardMap.put("cancelled", card.isCancelled());
                    
                    if (card instanceof NumberCard) {
                        NumberCard numCard = (NumberCard) card;
                        cardMap.put("value", numCard.getValue());
                        cardMap.put("special", false);
                    } else if (card instanceof OperatorCard) {
                        OperatorCard opCard = (OperatorCard) card;
                        cardMap.put("operator", opCard.getOperatorType().toString());
                        cardMap.put("special", false);
                    } else if (card instanceof SpecialCard) {
                        SpecialCard specCard = (SpecialCard) card;
                        cardMap.put("specialType", specCard.getSpecialType().toString());
                        cardMap.put("special", true);
                        cardMap.put("used", specCard.isUsed());
                        cardMap.put("pending", specCard.isPending());
                    }
                    roundHandCards.add(cardMap);
                }
                roundDTO.setHand(roundHandCards);
                roundDTOs.add(roundDTO);
            }
            playerDTO.setRounds(roundDTOs);
            System.out.println("      📚 Player rounds: " + roundDTOs.size());
            
            dto.addPlayer(playerDTO);
        }
        
        // Ajouter les cartes spéciales en attente d'assignation (depuis la queue)
        java.util.List<java.util.Map<String, Object>> pendingCards = new java.util.ArrayList<>();
        for (com.flip7.flip7.game.model.PendingSpecialCard pendingCard : game.getPendingSpecialCards()) {
            java.util.Map<String, Object> cardData = new java.util.HashMap<>();
            cardData.put("cardId", pendingCard.getCard().getId());
            cardData.put("cardType", pendingCard.getCard().getCardType().toString());
            cardData.put("specialType", ((SpecialCard) pendingCard.getCard()).getSpecialType().toString());
            cardData.put("sourcePlayerId", pendingCard.getSourcePlayerId());
            cardData.put("targetPlayerId", pendingCard.getTargetPlayerId());
            cardData.put("remainingForcedDraws", pendingCard.getRemainingForcedDraws());
            pendingCards.add(cardData);
            System.out.println("   📋 Pending card added to DTO: " + ((SpecialCard) pendingCard.getCard()).getSpecialType() + 
                             " | source=" + pendingCard.getSourcePlayerId() + 
                             " | target=" + pendingCard.getTargetPlayerId() + 
                             " | remaining=" + pendingCard.getRemainingForcedDraws());
        }
        dto.setPendingSpecialCards(pendingCards);
        System.out.println("   📋 Total pending special cards in DTO: " + pendingCards.size());

        // Ajouter les infos équipe si mode équipe activé
        dto.setTeamMode(game.isTeamMode());
        dto.setWinningTeamId(game.getWinningTeamId());
        if (game.isTeamMode()) {
            java.util.Map<Integer, java.util.Map<String, Object>> teamsMap = new java.util.HashMap<>();
            java.util.Map<Integer, Integer> teamScores = new java.util.HashMap<>();
            java.util.Map<Integer, java.util.List<String>> teamPlayers = new java.util.HashMap<>();
            java.util.Map<Integer, Long> teamSizes = new java.util.HashMap<>();
            for (GamePlayer p : game.getPlayers()) {
                int tid = p.getTeamId();
                if (tid <= 0) continue;
                teamScores.merge(tid, p.getTotalScore(), Integer::sum);
                teamPlayers.computeIfAbsent(tid, k -> new java.util.ArrayList<>()).add(p.getUserId());
                teamSizes.merge(tid, 1L, Long::sum);
            }
            for (Map.Entry<Integer, Integer> e : teamScores.entrySet()) {
                int tid = e.getKey();
                java.util.Map<String, Object> teamData = new java.util.HashMap<>();
                teamData.put("teamId", tid);
                teamData.put("totalScore", e.getValue());
                teamData.put("targetScore", (long) game.getTargetScore() * teamSizes.getOrDefault(tid, 1L));
                teamData.put("players", teamPlayers.getOrDefault(tid, java.util.Collections.emptyList()));
                teamsMap.put(tid, teamData);
            }
            dto.setTeams(teamsMap);
        }

        dto.setEventLog(game.getEventLog());
        return dto;
    }

    /**
     * Crée un DTO de fin de round
     */
    private RoundEndDTO createRoundEndDTO(Game game) {
        RoundEndDTO dto = new RoundEndDTO();
        dto.setRoundNumber(game.getRoundNumber());
        
        for (GamePlayer player : game.getPlayers()) {
            RoundPlayerScore score = new RoundPlayerScore();
            score.setUserId(player.getUserId());
            score.setUsername(player.getUsername());
            score.setRoundScore(player.getRoundScore());
            score.setTotalScore(player.getTotalScore());
            score.setEliminated(player.getStatus().name().equals("ELIMINATED"));
            dto.addPlayerScore(score);
        }
        
        return dto;
    }

    /**
     * Crée un DTO de fin de partie
     */
    private GameOverDTO createGameOverDTO(Game game) {
        GameOverDTO dto = new GameOverDTO();
        GamePlayer winner = game.getWinner();
        if (winner != null) {
            dto.setWinnerId(winner.getUserId());
            dto.setWinnerName(winner.getUsername());
            dto.setWinningScore(winner.getTotalScore());
        }

        for (GamePlayer player : game.getPlayers()) {
            FinalPlayerScore score = new FinalPlayerScore();
            score.setUserId(player.getUserId());
            score.setUsername(player.getUsername());
            score.setTotalScore(player.getTotalScore());
            dto.addFinalScore(score);
        }

        // Infos équipe
        dto.setTeamMode(game.isTeamMode());
        dto.setWinningTeamId(game.getWinningTeamId());
        if (game.isTeamMode()) {
            java.util.Map<Integer, java.util.Map<String, Object>> teamsMap = new java.util.HashMap<>();
            java.util.Map<Integer, Integer> teamScores = new java.util.HashMap<>();
            java.util.Map<Integer, java.util.List<String>> teamPlayers = new java.util.HashMap<>();
            for (GamePlayer p : game.getPlayers()) {
                int tid = p.getTeamId();
                if (tid <= 0) continue;
                teamScores.merge(tid, p.getTotalScore(), Integer::sum);
                teamPlayers.computeIfAbsent(tid, k -> new java.util.ArrayList<>()).add(p.getUserId());
            }
            for (Map.Entry<Integer, Integer> e : teamScores.entrySet()) {
                int tid = e.getKey();
                java.util.Map<String, Object> teamData = new java.util.HashMap<>();
                teamData.put("teamId", tid);
                teamData.put("totalScore", e.getValue());
                teamData.put("players", teamPlayers.getOrDefault(tid, java.util.Collections.emptyList()));
                teamsMap.put(tid, teamData);
            }
            dto.setTeams(teamsMap);
        }

        dto.setEventLog(game.getEventLog());
        return dto;
    }

    // DTOs internes
    public static class GameStateDTO {
    private GameState gameState;
    private int roundNumber;
    private int currentPlayerIndex;
    private int remainingCards;
    private java.util.List<PlayerDTO> players = new java.util.ArrayList<>();
    private java.util.List<java.util.Map<String, Object>> pendingSpecialCards = new java.util.ArrayList<>();
    private boolean statisticsEnabled;
    public boolean isStatisticsEnabled() { return statisticsEnabled; }
    public void setStatisticsEnabled(boolean statisticsEnabled) { this.statisticsEnabled = statisticsEnabled; }

        // Getters et Setters
        public GameState getGameState() { return gameState; }
        public void setGameState(GameState gameState) { this.gameState = gameState; }
        public int getRoundNumber() { return roundNumber; }
        public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
        public int getCurrentPlayerIndex() { return currentPlayerIndex; }
        public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
        public int getRemainingCards() { return remainingCards; }
        public void setRemainingCards(int remainingCards) { this.remainingCards = remainingCards; }
        public java.util.List<PlayerDTO> getPlayers() { return players; }
        public void addPlayer(PlayerDTO player) { this.players.add(player); }
        public java.util.List<java.util.Map<String, Object>> getPendingSpecialCards() { return pendingSpecialCards; }
        public void setPendingSpecialCards(java.util.List<java.util.Map<String, Object>> pendingSpecialCards) {
            this.pendingSpecialCards = pendingSpecialCards;
        }

        private boolean teamMode = false;
        private int winningTeamId = 0;
        private java.util.Map<Integer, java.util.Map<String, Object>> teams = new java.util.HashMap<>();
        private java.util.List<java.util.Map<String, Object>> eventLog = new java.util.ArrayList<>();
        public boolean isTeamMode() { return teamMode; }
        public void setTeamMode(boolean teamMode) { this.teamMode = teamMode; }
        public int getWinningTeamId() { return winningTeamId; }
        public void setWinningTeamId(int winningTeamId) { this.winningTeamId = winningTeamId; }
        public java.util.Map<Integer, java.util.Map<String, Object>> getTeams() { return teams; }
        public void setTeams(java.util.Map<Integer, java.util.Map<String, Object>> teams) { this.teams = teams; }
        public java.util.List<java.util.Map<String, Object>> getEventLog() { return eventLog; }
        public void setEventLog(java.util.List<java.util.Map<String, Object>> eventLog) { this.eventLog = eventLog; }
    }

    public static class PlayerDTO {
        private String userId;
        private String username;
        private com.flip7.flip7.game.model.PlayerStatus status;
        private int handSize;
        private int roundScore;        // Score du round en cours
        private int totalScore;        // Score sécurisé (rounds précédents)
        private int theoreticalTotal;  // Score total théorique (totalScore + roundScore)
        private int lifeCardsInHand;
        private int teamId = 0;
        private java.util.List<java.util.Map<String, Object>> hand = new java.util.ArrayList<>(); // Cartes révélées
        private java.util.List<RoundDTO> rounds = new java.util.ArrayList<>(); // Liste des rounds (le dernier = actuel)
        private String stoppedByUsername;
        private String drawThreeByUsername;

        // Getters et Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public com.flip7.flip7.game.model.PlayerStatus getStatus() { return status; }
        public void setStatus(com.flip7.flip7.game.model.PlayerStatus status) { this.status = status; }
        public int getHandSize() { return handSize; }
        public void setHandSize(int handSize) { this.handSize = handSize; }
        public int getRoundScore() { return roundScore; }
        public void setRoundScore(int roundScore) { this.roundScore = roundScore; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public int getTheoreticalTotal() { return theoreticalTotal; }
        public void setTheoreticalTotal(int theoreticalTotal) { this.theoreticalTotal = theoreticalTotal; }
        public int getLifeCardsInHand() { return lifeCardsInHand; }
        public void setLifeCardsInHand(int lifeCardsInHand) { this.lifeCardsInHand = lifeCardsInHand; }
        public int getTeamId() { return teamId; }
        public void setTeamId(int teamId) { this.teamId = teamId; }
        public String getStoppedByUsername() { return stoppedByUsername; }
        public void setStoppedByUsername(String stoppedByUsername) { this.stoppedByUsername = stoppedByUsername; }
        public String getDrawThreeByUsername() { return drawThreeByUsername; }
        public void setDrawThreeByUsername(String drawThreeByUsername) { this.drawThreeByUsername = drawThreeByUsername; }
        public java.util.List<java.util.Map<String, Object>> getHand() { return hand; }
        public void setHand(java.util.List<java.util.Map<String, Object>> hand) { this.hand = hand; }
        public java.util.List<RoundDTO> getRounds() { return rounds; }
        public void setRounds(java.util.List<RoundDTO> rounds) { this.rounds = rounds; }
    }

    public static class RoundEndDTO {
        private int roundNumber;
        private java.util.List<RoundPlayerScore> playerScores = new java.util.ArrayList<>();

        public int getRoundNumber() { return roundNumber; }
        public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
        public java.util.List<RoundPlayerScore> getPlayerScores() { return playerScores; }
        public void addPlayerScore(RoundPlayerScore score) { this.playerScores.add(score); }
    }

    public static class RoundPlayerScore {
        private String userId;
        private String username;
        private int roundScore;
        private int totalScore;
        private boolean eliminated;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public int getRoundScore() { return roundScore; }
        public void setRoundScore(int roundScore) { this.roundScore = roundScore; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public boolean isEliminated() { return eliminated; }
        public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }
    }

    public static class GameOverDTO {
        private String winnerId;
        private String winnerName;
        private int winningScore;
        private java.util.List<FinalPlayerScore> finalScores = new java.util.ArrayList<>();
        private boolean waitingForAdmin;
        private java.util.List<java.util.Map<String, Object>> eventLog = new java.util.ArrayList<>();

        public String getWinnerId() { return winnerId; }
        public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
        public String getWinnerName() { return winnerName; }
        public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
        public int getWinningScore() { return winningScore; }
        public void setWinningScore(int winningScore) { this.winningScore = winningScore; }
        public java.util.List<FinalPlayerScore> getFinalScores() { return finalScores; }
        public void addFinalScore(FinalPlayerScore score) { this.finalScores.add(score); }
        public boolean isWaitingForAdmin() { return waitingForAdmin; }
        public void setWaitingForAdmin(boolean waitingForAdmin) { this.waitingForAdmin = waitingForAdmin; }
        public java.util.List<java.util.Map<String, Object>> getEventLog() { return eventLog; }
        public void setEventLog(java.util.List<java.util.Map<String, Object>> eventLog) { this.eventLog = eventLog; }

    private boolean teamMode = false;
    private int winningTeamId = 0;
    private java.util.Map<Integer, java.util.Map<String, Object>> teams = new java.util.HashMap<>();
    public boolean isTeamMode() { return teamMode; }
    public void setTeamMode(boolean teamMode) { this.teamMode = teamMode; }
    public int getWinningTeamId() { return winningTeamId; }
    public void setWinningTeamId(int winningTeamId) { this.winningTeamId = winningTeamId; }
    public java.util.Map<Integer, java.util.Map<String, Object>> getTeams() { return teams; }
    public void setTeams(java.util.Map<Integer, java.util.Map<String, Object>> teams) { this.teams = teams; }
    }

    public static class FinalPlayerScore {
        private String userId;
        private String username;
        private int totalScore;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    }
    
    // ========== Méthodes de sauvegarde de l'historique ==========
    
    /**
     * Sauvegarde le début d'un round
     */
    private void saveRoundStart(String roomId, Game game) {
        String historyId = gameHistoryIds.get(roomId);
        if (historyId == null) {
            System.err.println("⚠️ saveRoundStart: historyId null pour room " + roomId + " (gameHistoryIds vide ?)");
            return;
        }

        GameHistory history = gameHistoryRepository.findById(historyId).orElse(null);
        if (history == null) {
            System.err.println("⚠️ saveRoundStart: GameHistory introuvable pour id=" + historyId);
            return;
        }

        GameHistory.RoundHistory roundHistory = new GameHistory.RoundHistory(game.getRoundNumber());
        history.addRound(roundHistory);
        history.serializeJsonFields(); // Force serialization before merge to avoid @PreUpdate stale-data overwrite
        gameHistoryRepository.save(history);
        System.out.println("📝 saveRoundStart: round " + game.getRoundNumber() + " ajouté (historyId=" + historyId + ", total rounds=" + history.getRounds().size() + ")");
    }
    
    /**
     * Sauvegarde la fin d'un round
     */
    private void saveRoundEnd(String roomId, Game game) {
        String historyId = gameHistoryIds.get(roomId);
        if (historyId == null) {
            System.err.println("⚠️ saveRoundEnd: historyId null pour room " + roomId + " — données du round perdues !");
            return;
        }

        GameHistory history = gameHistoryRepository.findById(historyId).orElse(null);
        if (history == null) {
            System.err.println("⚠️ saveRoundEnd: GameHistory introuvable pour id=" + historyId);
            return;
        }

        final int roundNum = game.getRoundNumber();
        System.out.println("📊 saveRoundEnd: round=" + roundNum + " history.rounds.size=" + history.getRounds().size());

        // Chercher le round par son numéro (plus robuste qu'un index position)
        GameHistory.RoundHistory roundHistory = history.getRounds().stream()
            .filter(r -> r.getRoundNumber() == roundNum)
            .findFirst()
            .orElse(null);

        if (roundHistory == null) {
            // saveRoundStart n'a pas été appelé (reconnexion, serveur redémarré…) — on crée le round à la volée
            System.err.println("⚠️ saveRoundEnd: round " + roundNum + " introuvable dans l'historique — création à la volée");
            roundHistory = new GameHistory.RoundHistory(roundNum);
            history.addRound(roundHistory);
        }
        roundHistory.setEndedAt(LocalDateTime.now());
        
        // Sauvegarder les données de chaque joueur
        int bestScore = -1;
        String roundWinnerId = null;
        
        for (GamePlayer player : game.getPlayers()) {
            GameHistory.PlayerRoundData data = new GameHistory.PlayerRoundData(
                player.getUserId(), 
                player.getUsername()
            );
            data.setRoundScore(player.getRoundScore());
            data.setEliminated(player.getStatus() == PlayerStatus.ELIMINATED);
            data.setStopped(player.getStatus() == PlayerStatus.STOPPED);
            data.setHasSevenDifferent(player.hasFlip7());
            data.setUsedLife(player.hasUsedLife());
            data.setCardsDrawn(player.getHandSize());

            // PATCH: life cards obtained this round
            data.setLifeCardsObtained(player.getLifeCardsObtainedThisRound());

            // Received a Stop card: only FORCED_STOP players were stopped by a card.
            // STOPPED = voluntary stop; FORCED_STOP = Stop card received from another player.
            data.setReceivedStopCard(player.getStatus() == PlayerStatus.FORCED_STOP);

            // Eliminated by drawing a double (not during a +3 sequence)
            data.setEliminatedByDouble(
                player.getStatus() == PlayerStatus.ELIMINATED &&
                player.getDrawThreeByUsername() == null
            );

            // PATCH: +3 logic - only if drawThreeByUsername is set and player status is not WAITING
            boolean receivedDrawThree = player.getDrawThreeByUsername() != null && player.getStatus() != PlayerStatus.WAITING;
            data.setReceivedDrawThree(receivedDrawThree);

            if (receivedDrawThree) {
                // +3 réussi (sans élimination)
                data.setCompletedDrawThree(player.getStatus() != PlayerStatus.ELIMINATED);

                // Éliminé pendant le +3 ?
                if (player.getStatus() == PlayerStatus.ELIMINATED) {
                    Map<Integer, Integer> numberCounts = new HashMap<>();
                    for (Card card : player.getHand()) {
                        if (card instanceof NumberCard && !card.isCancelled()) {
                            int num = ((NumberCard) card).getValue();
                            numberCounts.put(num, numberCounts.getOrDefault(num, 0) + 1);
                        }
                    }
                    boolean hasDouble = numberCounts.values().stream().anyMatch(count -> count >= 2);
                    data.setEliminatedByDrawThree(hasDouble);
                }
            }

            // Nouvelles stats détaillées
            data.setStopCardsDrawn(player.getStopCardsDrawnThisRound());
            data.setDrawThreeDrawn(player.getDrawThreeDrawnThisRound());
            data.setLifeCardsDrawn(player.getLifeCardsDrawnThisRound());
            data.setSelfAssignedSpecialCards(player.getSelfAssignedThisRound());
            data.setTeamId(player.getTeamId());

            roundHistory.addPlayerData(data);

            // Déterminer le gagnant du round (meilleur score non éliminé)
            if (player.getStatus() != PlayerStatus.ELIMINATED && player.getRoundScore() > bestScore) {
                bestScore = player.getRoundScore();
                roundWinnerId = player.getUserId();
            }
        }

        // Post-processing drawThreeDealtSuccess (perspective source)
        for (GamePlayer target : game.getPlayers()) {
            if (target.getDrawThreeByUserId() != null && target.getStatus() != PlayerStatus.ELIMINATED) {
                String sourceId = target.getDrawThreeByUserId();
                roundHistory.getPlayerData().stream()
                    .filter(d -> d.getPlayerId().equals(sourceId))
                    .findFirst()
                    .ifPresent(d -> d.setDrawThreeDealtSuccess(d.getDrawThreeDealtSuccess() + 1));
            }
        }

        // Scores par équipe pour ce round (mode équipe)
        if (game.isTeamMode()) {
            Map<Integer, Integer> teamRoundScores = new HashMap<>();
            for (GamePlayer player : game.getPlayers()) {
                int tid = player.getTeamId();
                if (tid > 0) {
                    teamRoundScores.merge(tid, player.getRoundScore(), Integer::sum);
                }
            }
            roundHistory.setTeamScores(teamRoundScores);
        }

        roundHistory.setRoundWinnerId(roundWinnerId);

        history.serializeJsonFields(); // Force serialization before merge to avoid @PreUpdate stale-data overwrite
        gameHistoryRepository.save(history);
        System.out.println("✅ saveRoundEnd: round " + game.getRoundNumber() + " sauvegardé pour room " + roomId + " (historyId=" + historyId + ")");
    }

    /**
     * Sauvegarde la fin de la partie
     */
    private void saveGameEnd(String roomId, Game game) {
        String historyId = gameHistoryIds.get(roomId);
        if (historyId == null) return;
        
        GameHistory history = gameHistoryRepository.findById(historyId).orElse(null);
        if (history == null) return;
        
        // Marquer la partie comme terminée
        history.setStatus(GameHistory.GameStatus.COMPLETED);
        history.setEndedAt(LocalDateTime.now());

        // Infos mode équipe
        history.setTeamMode(game.isTeamMode());
        history.setWinnerTeamId(game.getWinningTeamId());

        // Sauvegarder le gagnant
        GamePlayer winner = game.getWinner();
        if (winner != null) {
            history.setWinnerId(winner.getUserId());
        }

        // Sauvegarder les scores finaux
        for (GamePlayer player : game.getPlayers()) {
            GameHistory.PlayerScore score = new GameHistory.PlayerScore(
                player.getUserId(),
                player.getUsername(),
                player.getTotalScore()
            );

            // Compter les rounds gagnés
            int roundsWon = 0;
            int roundsPlayed = 0;
            for (GameHistory.RoundHistory round : history.getRounds()) {
                if (round.getRoundWinnerId() != null && round.getRoundWinnerId().equals(player.getUserId())) {
                    roundsWon++;
                }
                // Compter les rounds où le joueur n'a pas été éliminé
                for (GameHistory.PlayerRoundData data : round.getPlayerData()) {
                    if (data.getPlayerId().equals(player.getUserId()) && !data.isEliminated()) {
                        roundsPlayed++;
                        break;
                    }
                }
            }

            score.setRoundsWon(roundsWon);
            score.setRoundsPlayed(roundsPlayed);
            score.setTeamId(player.getTeamId());

            history.getFinalScores().add(score);
        }

        history.serializeJsonFields(); // Force serialization before merge to avoid @PreUpdate stale-data overwrite
        gameHistoryRepository.save(history);
    }

    /**
     * Supprime la référence mémoire vers un historique donné.
     * À appeler depuis AdminController avant de supprimer l'entrée en base,
     * afin d'éviter qu'un save() concurrent (saveRoundEnd, saveGameEnd…)
     * ne ré-insère l'enregistrement via JPA merge().
     */
    public void clearHistoryId(String historyId) {
        gameHistoryIds.entrySet().removeIf(e -> historyId.equals(e.getValue()));
    }

    /**
     * Supprime toute trace en mémoire et en base pour une room.
     * Garantit que @PostConstruct ne recrée pas d'historique au prochain redémarrage.
     */
    public void purgeRoomGameState(String roomId) {
        activeGames.remove(roomId);
        gameHistoryIds.remove(roomId);
        try {
            gameSnapshotRepository.deleteById(roomId);
        } catch (EmptyResultDataAccessException ignored) {
            // Pas de snapshot pour cette room, rien à faire
        }
    }

    /**
     * Récupère l'historique des parties d'une room
     */
    public java.util.List<GameHistory> getRoomGameHistory(String roomId) {
        return gameHistoryRepository.findByRoomId(roomId);
    }
    
    /**
     * Récupère l'historique des parties d'un joueur
     */
    public java.util.List<GameHistory> getPlayerGameHistory(String playerId) {
        return gameHistoryRepository.findByPlayerIdsContaining(playerId);
    }
    
    /**
     * Récupère les statistiques d'un joueur
     */
    public PlayerStats getPlayerStats(String playerId) {
        java.util.List<GameHistory> completedGames = gameHistoryRepository
            .findByPlayerIdsContainingAndStatus(playerId, GameHistory.GameStatus.COMPLETED);
        
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(playerId);
        stats.setTotalGamesPlayed(completedGames.size());
        
        int wins = 0;
        int totalScore = 0;
        int totalRoundsPlayed = 0;
        int totalRoundsWon = 0;
        
        for (GameHistory game : completedGames) {
            if (playerId.equals(game.getWinnerId())) {
                wins++;
            }
            
            for (GameHistory.PlayerScore score : game.getFinalScores()) {
                if (score.getPlayerId().equals(playerId)) {
                    totalScore += score.getTotalScore();
                    totalRoundsPlayed += score.getRoundsPlayed();
                    totalRoundsWon += score.getRoundsWon();
                }
            }
        }
        
        stats.setTotalWins(wins);
        stats.setTotalScore(totalScore);
        stats.setTotalRoundsPlayed(totalRoundsPlayed);
        stats.setTotalRoundsWon(totalRoundsWon);
        
        if (completedGames.size() > 0) {
            stats.setAverageScore(totalScore / completedGames.size());
            stats.setWinRate((double) wins / completedGames.size() * 100);
        }
        
        return stats;
    }
    
    /**
     * DTO pour les statistiques d'un joueur
     */
    public static class PlayerStats {
        private String playerId;
        private int totalGamesPlayed;
        private int totalWins;
        private int totalScore;
        private int averageScore;
        private double winRate;
        private int totalRoundsPlayed;
        private int totalRoundsWon;
        
        // Getters et Setters
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public int getTotalGamesPlayed() { return totalGamesPlayed; }
        public void setTotalGamesPlayed(int totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }
        public int getTotalWins() { return totalWins; }
        public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public int getAverageScore() { return averageScore; }
        public void setAverageScore(int averageScore) { this.averageScore = averageScore; }
        public double getWinRate() { return winRate; }
        public void setWinRate(double winRate) { this.winRate = winRate; }
        public int getTotalRoundsPlayed() { return totalRoundsPlayed; }
        public void setTotalRoundsPlayed(int totalRoundsPlayed) { this.totalRoundsPlayed = totalRoundsPlayed; }
        public int getTotalRoundsWon() { return totalRoundsWon; }
        public void setTotalRoundsWon(int totalRoundsWon) { this.totalRoundsWon = totalRoundsWon; }
    }

    /**
     * DTO pour un round dans la liste des rounds d'un joueur
     */
    public static class RoundDTO {
        private int roundNumber;
        private int roundScore;
        private int totalScore;
        private int theoreticalTotal;
        private com.flip7.flip7.game.model.PlayerStatus status;
        private java.util.List<java.util.Map<String, Object>> hand = new java.util.ArrayList<>();

        public int getRoundNumber() { return roundNumber; }
        public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
        public int getRoundScore() { return roundScore; }
        public void setRoundScore(int roundScore) { this.roundScore = roundScore; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public int getTheoreticalTotal() { return theoreticalTotal; }
        public void setTheoreticalTotal(int theoreticalTotal) { this.theoreticalTotal = theoreticalTotal; }
        public com.flip7.flip7.game.model.PlayerStatus getStatus() { return status; }
        public void setStatus(com.flip7.flip7.game.model.PlayerStatus status) { this.status = status; }
        public java.util.List<java.util.Map<String, Object>> getHand() { return hand; }
        public void setHand(java.util.List<java.util.Map<String, Object>> hand) { this.hand = hand; }
    }

    /**
     * MÉTHODE DE TEST - Simule une distribution avec une carte Stop au 2ème joueur
     * Cette méthode permet de tester facilement le scénario:
     * - Joueur 1 reçoit une carte normale
     * - Joueur 2 reçoit une carte Stop (distribution s'arrête)
     * - Joueur 2 doit pouvoir assigner à TOUS les autres joueurs (même ceux sans carte)
     */
    public void testDistributionWithStopCard(String roomId) {
        Game game = getGame(roomId);
        if (game == null) {
            throw new RuntimeException("Game not found");
        }

        System.out.println("\n🧪 === TEST: Distribution avec carte Stop ===");
        
        // 1. Vérifier qu'on est dans le bon état
        if (game.getGameState() != GameState.WAITING && game.getGameState() != GameState.WAITING_NEXT_ROUND) {
            throw new RuntimeException("Le jeu doit être en attente pour lancer ce test");
        }

        // 2. Démarrer un nouveau round normalement
        game.startNewRound();
        System.out.println("✅ Round démarré");
        
        // 3. Forcer la distribution manuelle:
        //    - Joueur 1: carte normale (NumberCard 1)
        //    - Joueur 2: carte Stop
        
        if (game.getPlayers().size() < 2) {
            throw new RuntimeException("Il faut au moins 2 joueurs pour ce test");
        }

        // Donner une carte normale au joueur 1
        GamePlayer player1 = game.getPlayers().get(0);
        Card normalCard = new NumberCard(1);
        player1.addCard(normalCard);
        player1.setStatus(PlayerStatus.PLAYING);
        System.out.println("   ✅ " + player1.getUsername() + " a reçu: " + normalCard.getDisplayName());

        // Donner une carte Stop au joueur 2
        GamePlayer player2 = game.getPlayers().get(1);
        SpecialCard stopCard = new SpecialCard(com.flip7.flip7.game.card.SpecialType.STOP);
        stopCard.setPending(true); // IMPORTANT: marquer comme pending
        player2.addCard(stopCard);
        player2.setStatus(PlayerStatus.PLAYING);
        // game.setCurrentPlayerIndex(1); // COMMENTÉ - méthode n'existe pas
        System.out.println("   🛑 " + player2.getUsername() + " a reçu: CARTE STOP (pending=true)");
        System.out.println("   ⏸️  Distribution en pause - " + player2.getUsername() + " doit assigner");
        
        // Les autres joueurs restent en WAITING (pas encore de carte)
        for (int i = 2; i < game.getPlayers().size(); i++) {
            System.out.println("   ⏳ " + game.getPlayers().get(i).getUsername() + " attend sa carte...");
        }

        // 4. Broadcaster l'état
        // broadcastGameState(roomId); // COMMENTÉ - signature incorrecte
        broadcastGameState(roomId, game);
        
        System.out.println("🧪 === TEST PRÊT ===");
        System.out.println("   " + player2.getUsername() + " doit maintenant assigner la carte Stop");
        System.out.println("   Il devrait voir TOUS les joueurs (sauf lui-même) dans la modale");
        System.out.println("   Y compris ceux qui n'ont pas encore de carte");
    }
    
    /**
     * Redémarre une nouvelle partie dans la même room
     * Sauvegarde la partie actuelle si elle est terminée et crée une nouvelle partie
     */
    @Transactional
    public void restartGame(String roomId, String userId) {
        // Récupérer la room
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            throw new RuntimeException("Room not found");
        }
        
        // Vérifier que l'utilisateur est l'admin de la room
        if (!room.getAdminId().equals(userId)) {
            throw new RuntimeException("Only the room admin can restart the game");
        }
        
        // Récupérer la partie actuelle
        Game currentGame = activeGames.get(roomId);
        
        // Si une partie existe et est en GAME_OVER, la sauvegarder
        if (currentGame != null && currentGame.getGameState() == GameState.GAME_OVER) {
            // Supprimer le snapshot de la BDD (partie terminée)
            deleteGameSnapshot(roomId);
            
            String historyId = gameHistoryIds.get(roomId);
            if (historyId != null) {
                GameHistory history = gameHistoryRepository.findById(historyId).orElse(null);
                if (history != null && history.getStatus() == GameHistory.GameStatus.IN_PROGRESS) {
                    // Marquer la partie comme terminée
                    history.setStatus(GameHistory.GameStatus.COMPLETED);
                    history.setEndedAt(LocalDateTime.now());
                    
                    // Sauvegarder le gagnant
                    GamePlayer winner = currentGame.getWinner();
                    if (winner != null) {
                        history.setWinnerId(winner.getUserId());
                    }
                    
                    // Sauvegarder les scores finaux
                    for (GamePlayer player : currentGame.getPlayers()) {
                        GameHistory.PlayerScore score = new GameHistory.PlayerScore(
                            player.getUserId(),
                            player.getUsername(),
                            player.getTotalScore()
                        );
                        history.getFinalScores().add(score);
                    }
                    
                    gameHistoryRepository.save(history);
                    System.out.println("✅ Partie terminée sauvegardée dans l'historique: " + historyId);
                }
            }
        }
        
        // Supprimer l'ancienne partie de la mémoire
        activeGames.remove(roomId);
        gameHistoryIds.remove(roomId);

        // Stocker temporairement les anciens joueurs (hors admin) pour rejoin sans mot de passe
        List<String> previousPlayers = new java.util.ArrayList<>(room.getPlayers());
        previousPlayers.remove(room.getAdminId());
        room.setPreviousPlayers(previousPlayers);

        // Réinitialiser la room comme neuve (sauf admin, mot de passe, maxPlayers)
        String adminId = room.getAdminId();
        room.getPlayers().clear();
        room.getPlayers().add(adminId);
        room.setTurnIndex(0);
        room.setGameState(null);
        room.setStatus(Room.RoomStatus.WAITING);
        System.out.println("[RESTART] Room reset (avant save): status=" + room.getStatus()
            + ", players=" + room.getPlayers()
            + ", turnIndex=" + room.getTurnIndex()
            + ", gameState=" + room.getGameState()
            + ", previousPlayers=" + room.getPreviousPlayers());

        Room managedRoom = roomService.getRoomRepository().save(room);
        System.out.println("[RESTART] Après save: status=" + managedRoom.getStatus()
            + ", players=" + managedRoom.getPlayers()
            + ", turnIndex=" + managedRoom.getTurnIndex()
            + ", gameState=" + managedRoom.getGameState());
        roomService.getRoomRepository().flush();
        Room roomAfterSave = roomService.getRoomRepository().findById(roomId).orElse(null);
        if (roomAfterSave != null) {
            System.out.println("[RESTART] Après save+flush: room.status=" + roomAfterSave.getStatus()
                + ", players=" + roomAfterSave.getPlayers()
                + ", turnIndex=" + roomAfterSave.getTurnIndex()
                + ", gameState=" + roomAfterSave.getGameState());
        } else {
            System.out.println("[RESTART] ERREUR: Room non trouvée après save+flush!");
        }
        // Broadcast the updated room status to all clients (so room browser and join logic get the update)
        roomService.broadcastRoomUpdate(roomAfterSave != null ? roomAfterSave : room);

        // Notifier tous les joueurs encore sur la page de jeu de revenir en salle d'attente
        Map<String, Object> restartData = new HashMap<>();
        restartData.put("message", "Game restarted");
        restartData.put("roomStatus", "WAITING");
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/game-restarted", restartData);
        System.out.println("📡 Event game-restarted envoyé à /topic/rooms/" + roomId + "/game-restarted");

        System.out.println("🔄 Partie redémarrée pour la room: " + roomId);
    }
    
    /**
     * Nettoie les games en mémoire pour les rooms qui n'existent plus dans la base de données
     * ou qui sont inactives depuis longtemps.
     * Cette méthode est appelée périodiquement par le scheduler de RoomService.
     */
    public void cleanupInactiveGames() {
        System.out.println("   🎮 Nettoyage des games en mémoire...");
        
        int initialSize = activeGames.size();
        if (initialSize == 0) {
            System.out.println("      ✅ Aucun game en mémoire");
            return;
        }
        
        System.out.println("      📊 Games en mémoire avant nettoyage: " + initialSize);
        
        // Supprimer les games dont la room n'existe plus en base
        activeGames.entrySet().removeIf(entry -> {
            String roomId = entry.getKey();
            try {
                Room room = roomService.getRoom(roomId);
                if (room == null) {
                    System.out.println("      🗑️ Suppression du game pour room inexistante: " + roomId);
                    gameHistoryIds.remove(roomId);
                    return true;
                }
                return false;
            } catch (Exception e) {
                System.out.println("      🗑️ Suppression du game pour room avec erreur: " + roomId);
                gameHistoryIds.remove(roomId);
                return true;
            }
        });
        
        int finalSize = activeGames.size();
        int removed = initialSize - finalSize;
        
        if (removed > 0) {
            System.out.println("      ✅ " + removed + " game(s) supprimé(s) de la mémoire");
            System.out.println("      📊 Games restants en mémoire: " + finalSize);
        } else {
            System.out.println("      ✅ Aucun game à supprimer (tous valides)");
        }
    }
}
