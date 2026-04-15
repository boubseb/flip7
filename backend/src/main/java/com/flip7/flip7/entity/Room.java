// ...existing code...

package com.flip7.flip7.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room {
    @jakarta.persistence.Transient
    private List<String> previousPlayers = new ArrayList<>();

    public List<String> getPreviousPlayers() {
        return previousPlayers;
    }

    public void setPreviousPlayers(List<String> previousPlayers) {
        this.previousPlayers = previousPlayers;
    }
    
    @Id
    private String id;

    @PrePersist
    private void generateId() {
        if (this.id == null || this.id.isEmpty()) {
            String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            java.security.SecureRandom rng = new java.security.SecureRandom();
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
            this.id = sb.toString();
        }
    }
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String adminId;
    
    @ElementCollection
    @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "player_id")
    private List<String> players = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.WAITING;
    
    @Column
    private Integer turnIndex = 0;
    
    @Column(columnDefinition = "TEXT")
    private String gameState; // JSON string pour l'état du jeu
    
    @Column
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime lastActivityAt = LocalDateTime.now();
    
    @Column
    private Integer maxPlayers = 12;
    
    @Column(nullable = false)
    private boolean statisticsEnabled = false;
    
    @Column(nullable = false)
    private Integer targetScore = 200; // Score cible pour gagner

    @Column(nullable = false)
    private boolean teamMode = false;

    @Column
    private Integer numTeams = 2;

    @Column(nullable = false)
    private boolean persistentDeck = false;

    // JSON: Map<String, Integer> (playerId → teamId, 1-indexed)
    @Column(name = "team_assignments", columnDefinition = "TEXT")
    private String teamAssignmentsJson = "{}";

    private static final ObjectMapper roomObjectMapper = new ObjectMapper();

    public Map<String, Integer> getTeamAssignments() {
        try {
            if (teamAssignmentsJson == null || teamAssignmentsJson.isBlank()) return new HashMap<>();
            return roomObjectMapper.readValue(teamAssignmentsJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void setTeamAssignments(Map<String, Integer> assignments) {
        try {
            this.teamAssignmentsJson = roomObjectMapper.writeValueAsString(assignments != null ? assignments : new HashMap<>());
        } catch (Exception e) {
            this.teamAssignmentsJson = "{}";
        }
    }

    public boolean isTeamMode() { return teamMode; }
    public void setTeamMode(boolean teamMode) { this.teamMode = teamMode; }

    public Integer getNumTeams() { return numTeams; }
    public void setNumTeams(Integer numTeams) { this.numTeams = numTeams != null ? numTeams : 2; }

    public boolean isPersistentDeck() { return persistentDeck; }
    public void setPersistentDeck(boolean persistentDeck) { this.persistentDeck = persistentDeck; }

    // Getters and Setters
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }
    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }
    
    public Integer getTargetScore() {
        return targetScore;
    }
    
    public void setTargetScore(Integer targetScore) {
        this.targetScore = targetScore != null ? targetScore : 200;
    }
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getAdminId() {
        return adminId;
    }
    
    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }
    
    public List<String> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<String> players) {
        this.players = players;
    }
    
    public RoomStatus getStatus() {
        return status;
    }
    
    public void setStatus(RoomStatus status) {
        this.status = status;
    }
    
    public Integer getTurnIndex() {
        return turnIndex;
    }
    
    public void setTurnIndex(Integer turnIndex) {
        this.turnIndex = turnIndex;
    }
    
    public String getGameState() {
        return gameState;
    }
    
    public void setGameState(String gameState) {
        this.gameState = gameState;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Integer getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }
    
    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
    
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    public enum RoomStatus {
        WAITING,
        IN_GAME,
        FINISHED
    }
}
