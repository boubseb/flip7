package com.flip7.flip7.dto;

public class AdminStatsDTO {
    private long totalUsers;
    private long totalRooms;
    private long activeRooms;
    private long totalGames;
    private long gamesLast7Days;

    public AdminStatsDTO(long totalUsers, long totalRooms, long activeRooms,
                         long totalGames, long gamesLast7Days) {
        this.totalUsers = totalUsers;
        this.totalRooms = totalRooms;
        this.activeRooms = activeRooms;
        this.totalGames = totalGames;
        this.gamesLast7Days = gamesLast7Days;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getTotalRooms() { return totalRooms; }
    public long getActiveRooms() { return activeRooms; }
    public long getTotalGames() { return totalGames; }
    public long getGamesLast7Days() { return gamesLast7Days; }
}
