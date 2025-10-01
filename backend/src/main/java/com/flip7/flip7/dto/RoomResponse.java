package com.flip7.flip7.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.flip7.flip7.entity.Room.RoomStatus;

public class RoomResponse {
    private String id;
    private String adminId;
    private List<String> players;
    private RoomStatus status;
    private Integer turnIndex;
    private LocalDateTime createdAt;
    private Integer maxPlayers;
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
    
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }
    
    public void setCurrentPlayerId(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }
}
