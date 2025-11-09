package com.flip7.flip7.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Snapshot d'une partie en cours pour restauration après redémarrage serveur
 */
@Entity
@Table(name = "game_snapshots")
public class GameSnapshot {
    
    @Id
    private String roomId;  // Utiliser roomId comme clé primaire (1 snapshot par room)
    
    @Column(name = "game_state", columnDefinition = "TEXT", nullable = false)
    private String gameStateJson;  // État complet de la partie sérialisé en JSON
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "current_round")
    private int currentRound;
    
    @Column(name = "game_status")
    private String gameStatus;  // PLAYING, WAITING_NEXT_ROUND, GAME_OVER
    
    public GameSnapshot() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    public GameSnapshot(String roomId, String gameStateJson) {
        this();
        this.roomId = roomId;
        this.gameStateJson = gameStateJson;
    }

    // Getters et Setters
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getGameStateJson() {
        return gameStateJson;
    }
    
    public void setGameStateJson(String gameStateJson) {
        this.gameStateJson = gameStateJson;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public int getCurrentRound() {
        return currentRound;
    }
    
    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }
    
    public String getGameStatus() {
        return gameStatus;
    }
    
    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }
}
