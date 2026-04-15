package com.flip7.flip7.dto;

import java.time.LocalDateTime;

public class GameHistoryAdminDTO {
    private String id;
    private String roomId;
    private String status;
    private String winnerId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int totalRounds;

    public GameHistoryAdminDTO(String id, String roomId, String status, String winnerId,
                               LocalDateTime startedAt, LocalDateTime endedAt, int totalRounds) {
        this.id = id;
        this.roomId = roomId;
        this.status = status;
        this.winnerId = winnerId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.totalRounds = totalRounds;
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getStatus() { return status; }
    public String getWinnerId() { return winnerId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public int getTotalRounds() { return totalRounds; }
}
