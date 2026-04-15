package com.flip7.flip7.dto;

import java.time.LocalDateTime;

public class RoomAdminDTO {
    private String id;
    private String adminId;
    private String status;
    private int playerCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    public RoomAdminDTO(String id, String adminId, String status, int playerCount,
                        LocalDateTime createdAt, LocalDateTime lastActivityAt) {
        this.id = id;
        this.adminId = adminId;
        this.status = status;
        this.playerCount = playerCount;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
    }

    public String getId() { return id; }
    public String getAdminId() { return adminId; }
    public String getStatus() { return status; }
    public int getPlayerCount() { return playerCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
}
