package com.flip7.flip7.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.flip7.flip7.entity.Room.RoomStatus;

public class RoomResponse {
    private boolean statisticsEnabled;
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }
    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }
    private String id;
    private String adminId;
    private List<String> players; // kept for backward compatibility
    private List<PlayerInfo> playerInfos; // new field with pseudo
    private RoomStatus status;
    private Integer turnIndex;
    private LocalDateTime createdAt;
    private Integer maxPlayers;
    private Integer currentPlayers; // Nombre actuel de joueurs dans la room
    private String currentPlayerId; // player whose turn it is
    
    public RoomResponse(String id, String adminId, List<String> players, RoomStatus status, 
                       Integer turnIndex, LocalDateTime createdAt, Integer maxPlayers) {
        this.id = id;
        this.adminId = adminId;
        this.players = players;
        this.status = status;
        this.turnIndex = turnIndex;
        this.createdAt = createdAt;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = players != null ? players.size() : 0; // Calculer le nombre actuel
        if (players != null && !players.isEmpty() && turnIndex != null && turnIndex < players.size()) {
            this.currentPlayerId = players.get(turnIndex);
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public Integer getCurrentPlayers() {
        return currentPlayers;
    }
    
    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
    
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }
    
    public void setCurrentPlayerId(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }
    
    public List<PlayerInfo> getPlayerInfos() {
        return playerInfos;
    }
    
    public void setPlayerInfos(List<PlayerInfo> playerInfos) {
        this.playerInfos = playerInfos;
    }
}
