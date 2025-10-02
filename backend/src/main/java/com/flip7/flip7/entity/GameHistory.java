package com.flip7.flip7.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant l'historique d'une partie de Flip7
 * Une room peut avoir plusieurs parties (GameHistory)
 */
@Document(collection = "game_history")
public class GameHistory {
    
    @Id
    private String id;
    
    private String roomId;              // Référence à la room
    private List<String> playerIds;     // Liste des joueurs
    private LocalDateTime startedAt;    // Date de début
    private LocalDateTime endedAt;      // Date de fin (null si non terminée)
    private GameStatus status;          // Statut de la partie
    private String winnerId;            // ID du gagnant (null si pas terminé)
    private int totalRounds;            // Nombre de rounds joués
    
    // Historique détaillé de chaque round
    private List<RoundHistory> rounds;
    
    // Scores finaux des joueurs
    private List<PlayerScore> finalScores;

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

        public RoundHistory() {
            this.playerData = new ArrayList<>();
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
        private boolean hasSevenDifferent;  // A gagné avec 7 cartes différentes
        private int cardsDrawn;             // Nombre de cartes piochées
        private boolean usedLife;           // A utilisé une carte Vie

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
