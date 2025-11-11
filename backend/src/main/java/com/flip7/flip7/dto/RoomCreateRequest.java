package com.flip7.flip7.dto;

public class RoomCreateRequest {
    private String password;
    private Integer maxPlayers = 12;
    private boolean statisticsEnabled = false;
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }
    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Integer getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
