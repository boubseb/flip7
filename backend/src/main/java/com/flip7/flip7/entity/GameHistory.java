package com.flip7.flip7.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entité représentant l'historique d'une partie de Flip7
 * Une room peut avoir plusieurs parties (GameHistory)
 */
@Entity
@Table(name = "game_history")
public class GameHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "room_id", nullable = false)
    private String roomId;              // Référence à la room
    
    @ElementCollection
    @CollectionTable(name = "game_history_players", joinColumns = @JoinColumn(name = "game_history_id"))
    @Column(name = "player_id")
    private List<String> playerIds;     // Liste des joueurs
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;    // Date de début
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;      // Date de fin (null si non terminée)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GameStatus status;          // Statut de la partie
    
    @Column(name = "winner_id")
    private String winnerId;            // ID du gagnant (null si pas terminé)

    @Column(name = "team_mode")
    private Boolean teamMode = false;   // Partie en mode équipe

    @Column(name = "winner_team_id")
    private int winnerTeamId = 0;       // ID équipe gagnante (0 = mode solo)

    @Column(name = "persistent_deck")
    private Boolean persistentDeck = false; // Paquet persistant entre rounds

    @Column(name = "statistics_enabled")
    private Boolean statisticsEnabled = false; // Stats (proba élimination) activées

    @Column(name = "total_rounds")
    private int totalRounds;            // Nombre de rounds joués
    
    // Historique détaillé de chaque round - Stocké en JSON pour simplicité
    @Column(name = "rounds", columnDefinition = "TEXT")
    private String roundsJson;
    
    @Transient
    private List<RoundHistory> rounds;
    
    // Scores finaux des joueurs - Stocké en JSON pour simplicité
    @Column(name = "final_scores", columnDefinition = "TEXT")
    private String finalScoresJson;
    
    @Transient
    private List<PlayerScore> finalScores;

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public GameHistory() {
        this.rounds = new ArrayList<>();
        this.finalScores = new ArrayList<>();
        this.startedAt = LocalDateTime.now();
        this.status = GameStatus.IN_PROGRESS;
    }

    public GameHistory(String roomId, List<String> playerIds) {
        this();
        this.roomId = roomId;
        this.playerIds = new ArrayList<>(playerIds);
    }
    
    @PrePersist
    public void serializeJsonFields() {
        try {
            this.roundsJson = objectMapper.writeValueAsString(this.rounds);
            this.finalScoresJson = objectMapper.writeValueAsString(this.finalScores);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la sérialisation JSON", e);
        }
    }
    
    @PostLoad
    private void deserializeJsonFields() {
        try {
            this.rounds = objectMapper.readValue(
                roundsJson != null ? roundsJson : "[]",
                new TypeReference<List<RoundHistory>>() {}
            );
            this.finalScores = objectMapper.readValue(
                finalScoresJson != null ? finalScoresJson : "[]",
                new TypeReference<List<PlayerScore>>() {}
            );
        } catch (JsonProcessingException e) {
            System.err.println("❌ GameHistory @PostLoad deserialization error (id=" + id + "): " + e.getMessage());
            this.rounds = new ArrayList<>();
            this.finalScores = new ArrayList<>();
        }
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<String> playerIds) {
        this.playerIds = playerIds;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }

    public List<RoundHistory> getRounds() {
        return rounds;
    }

    public void setRounds(List<RoundHistory> rounds) {
        this.rounds = rounds;
    }

    public void addRound(RoundHistory round) {
        this.rounds.add(round);
        this.totalRounds = this.rounds.size();
    }

    public boolean isTeamMode() { return teamMode != null && teamMode; }
    public void setTeamMode(Boolean teamMode) { this.teamMode = teamMode != null ? teamMode : false; }

    public int getWinnerTeamId() { return winnerTeamId; }
    public void setWinnerTeamId(int winnerTeamId) { this.winnerTeamId = winnerTeamId; }

    public boolean isPersistentDeck() { return persistentDeck != null && persistentDeck; }
    public void setPersistentDeck(Boolean persistentDeck) { this.persistentDeck = persistentDeck != null ? persistentDeck : false; }

    public boolean isStatisticsEnabled() { return statisticsEnabled != null && statisticsEnabled; }
    public void setStatisticsEnabled(Boolean statisticsEnabled) { this.statisticsEnabled = statisticsEnabled != null ? statisticsEnabled : false; }

    public List<PlayerScore> getFinalScores() {
        return finalScores;
    }

    public void setFinalScores(List<PlayerScore> finalScores) {
        this.finalScores = finalScores;
    }

    /**
     * Classe interne représentant l'historique d'un round
     */
    public static class RoundHistory {
        private int roundNumber;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private List<PlayerRoundData> playerData;
        private String roundWinnerId;     // Joueur avec le meilleur score du round
        private Map<Integer, Integer> teamScores; // teamId → score du round (mode équipe)

        public RoundHistory() {
            this.playerData = new ArrayList<>();
            this.teamScores = new HashMap<>();
            this.startedAt = LocalDateTime.now();
        }

        public RoundHistory(int roundNumber) {
            this();
            this.roundNumber = roundNumber;
        }

        // Getters et Setters
        public int getRoundNumber() {
            return roundNumber;
        }

        public void setRoundNumber(int roundNumber) {
            this.roundNumber = roundNumber;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
        }

        public LocalDateTime getEndedAt() {
            return endedAt;
        }

        public void setEndedAt(LocalDateTime endedAt) {
            this.endedAt = endedAt;
        }

        public List<PlayerRoundData> getPlayerData() {
            return playerData;
        }

        public void setPlayerData(List<PlayerRoundData> playerData) {
            this.playerData = playerData;
        }

        public void addPlayerData(PlayerRoundData data) {
            this.playerData.add(data);
        }

        public String getRoundWinnerId() {
            return roundWinnerId;
        }

        public void setRoundWinnerId(String roundWinnerId) {
            this.roundWinnerId = roundWinnerId;
        }

        public Map<Integer, Integer> getTeamScores() { return teamScores; }
        public void setTeamScores(Map<Integer, Integer> teamScores) { this.teamScores = teamScores; }
    }

    /**
     * Données d'un joueur pour un round
     */
    public static class PlayerRoundData {
        private String playerId;
        private String username;
        private int roundScore;
        private boolean eliminated;
        private boolean stopped;
        private boolean hasSevenDifferent;  // A gagné avec 7 cartes différentes (Flip7)
        private int cardsDrawn;             // Nombre de cartes piochées
        private boolean usedLife;           // A utilisé une carte Vie
        private boolean eliminatedByDouble; // Éliminé par un double
        private boolean receivedStopCard;   // A reçu une carte Stop
        private int lifeCardsObtained;      // Nombre de cartes Vie obtenues
        private boolean receivedDrawThree;  // A reçu une carte +3
        private boolean completedDrawThree; // A réussi le +3 (sans élimination)
        private boolean eliminatedByDrawThree; // Éliminé par un double lors du +3
        private int stopCardsDrawn;         // Nombre de cartes Stop piochées
        private int x2CardsDrawn;           // Nombre de cartes ×2 piochées
        private int drawThreeDrawn;         // Nombre de cartes +3 piochées
        private int lifeCardsDrawn;         // Nombre de cartes Vie piochées (comptées en mode équipe)
        private int selfAssignedSpecialCards; // Nombre de fois auto-attribution carte spéciale
        private int drawThreeDealtSuccess;  // Nombre de +3 distribués qui ont réussi (perspective source)
        private int teamId;                 // ID équipe du joueur (0 si mode solo)
        private Map<String, Integer> cardDrawCounts = new HashMap<>(); // Distribution des cartes piochées
        private int eliminatingCardValue = -1; // Valeur de la carte double éliminatrice (-1 si non éliminé)

        public PlayerRoundData() {
        }

        public PlayerRoundData(String playerId, String username) {
            this.playerId = playerId;
            this.username = username;
        }

        // Getters et Setters
        public String getPlayerId() {
            return playerId;
        }

        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getRoundScore() {
            return roundScore;
        }

        public void setRoundScore(int roundScore) {
            this.roundScore = roundScore;
        }

        public boolean isEliminated() {
            return eliminated;
        }

        public void setEliminated(boolean eliminated) {
            this.eliminated = eliminated;
        }

        public boolean isStopped() {
            return stopped;
        }

        public void setStopped(boolean stopped) {
            this.stopped = stopped;
        }

        public boolean isHasSevenDifferent() {
            return hasSevenDifferent;
        }

        public void setHasSevenDifferent(boolean hasSevenDifferent) {
            this.hasSevenDifferent = hasSevenDifferent;
        }

        public int getCardsDrawn() {
            return cardsDrawn;
        }

        public void setCardsDrawn(int cardsDrawn) {
            this.cardsDrawn = cardsDrawn;
        }

        public boolean isUsedLife() {
            return usedLife;
        }

        public void setUsedLife(boolean usedLife) {
            this.usedLife = usedLife;
        }

        public boolean isEliminatedByDouble() {
            return eliminatedByDouble;
        }

        public void setEliminatedByDouble(boolean eliminatedByDouble) {
            this.eliminatedByDouble = eliminatedByDouble;
        }

        public boolean isReceivedStopCard() {
            return receivedStopCard;
        }

        public void setReceivedStopCard(boolean receivedStopCard) {
            this.receivedStopCard = receivedStopCard;
        }

        public int getLifeCardsObtained() {
            return lifeCardsObtained;
        }

        public void setLifeCardsObtained(int lifeCardsObtained) {
            this.lifeCardsObtained = lifeCardsObtained;
        }

        public boolean isReceivedDrawThree() {
            return receivedDrawThree;
        }

        public void setReceivedDrawThree(boolean receivedDrawThree) {
            this.receivedDrawThree = receivedDrawThree;
        }

        public boolean isCompletedDrawThree() {
            return completedDrawThree;
        }

        public void setCompletedDrawThree(boolean completedDrawThree) {
            this.completedDrawThree = completedDrawThree;
        }

        public boolean isEliminatedByDrawThree() {
            return eliminatedByDrawThree;
        }

        public void setEliminatedByDrawThree(boolean eliminatedByDrawThree) {
            this.eliminatedByDrawThree = eliminatedByDrawThree;
        }

        public int getStopCardsDrawn() { return stopCardsDrawn; }
        public void setStopCardsDrawn(int stopCardsDrawn) { this.stopCardsDrawn = stopCardsDrawn; }

        public int getX2CardsDrawn() { return x2CardsDrawn; }
        public void setX2CardsDrawn(int x2CardsDrawn) { this.x2CardsDrawn = x2CardsDrawn; }

        public int getDrawThreeDrawn() { return drawThreeDrawn; }
        public void setDrawThreeDrawn(int drawThreeDrawn) { this.drawThreeDrawn = drawThreeDrawn; }

        public int getLifeCardsDrawn() { return lifeCardsDrawn; }
        public void setLifeCardsDrawn(int lifeCardsDrawn) { this.lifeCardsDrawn = lifeCardsDrawn; }

        public int getSelfAssignedSpecialCards() { return selfAssignedSpecialCards; }
        public void setSelfAssignedSpecialCards(int selfAssignedSpecialCards) { this.selfAssignedSpecialCards = selfAssignedSpecialCards; }

        public int getDrawThreeDealtSuccess() { return drawThreeDealtSuccess; }
        public void setDrawThreeDealtSuccess(int drawThreeDealtSuccess) { this.drawThreeDealtSuccess = drawThreeDealtSuccess; }

        public int getTeamId() { return teamId; }
        public void setTeamId(int teamId) { this.teamId = teamId; }

        public Map<String, Integer> getCardDrawCounts() { return cardDrawCounts; }
        public void setCardDrawCounts(Map<String, Integer> cardDrawCounts) { this.cardDrawCounts = cardDrawCounts != null ? cardDrawCounts : new HashMap<>(); }

        public int getEliminatingCardValue() { return eliminatingCardValue; }
        public void setEliminatingCardValue(int eliminatingCardValue) { this.eliminatingCardValue = eliminatingCardValue; }
    }

    /**
     * Score final d'un joueur
     */
    public static class PlayerScore {
        private String playerId;
        private String username;
        private int totalScore;
        private int roundsWon;      // Nombre de rounds gagnés
        private int roundsPlayed;   // Nombre de rounds joués (non éliminé)
        private int teamId;         // ID équipe (0 si mode solo)

        public PlayerScore() {
        }

        public PlayerScore(String playerId, String username, int totalScore) {
            this.playerId = playerId;
            this.username = username;
            this.totalScore = totalScore;
        }

        // Getters et Setters
        public String getPlayerId() {
            return playerId;
        }

        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }

        public int getRoundsWon() {
            return roundsWon;
        }

        public void setRoundsWon(int roundsWon) {
            this.roundsWon = roundsWon;
        }

        public int getRoundsPlayed() {
            return roundsPlayed;
        }

        public void setRoundsPlayed(int roundsPlayed) {
            this.roundsPlayed = roundsPlayed;
        }

        public int getTeamId() { return teamId; }
        public void setTeamId(int teamId) { this.teamId = teamId; }
    }

    /**
     * Statut d'une partie
     */
    public enum GameStatus {
        IN_PROGRESS,    // Partie en cours
        COMPLETED,      // Partie terminée normalement (un joueur a atteint 200 points)
        ABANDONED       // Partie abandonnée (joueurs ont quitté)
    }
}
