package com.flip7.flip7.dto;

public class RoomCreateRequest {
    private String password;
    private Integer maxPlayers = 12;
    private boolean statisticsEnabled = false;
    private Integer targetScore = 200; // Score cible pour gagner (défaut: 200)
    private boolean teamMode = false;
    private Integer numTeams = 2;
    private boolean persistentDeck = false;
    
    public Integer getTargetScore() {
        return targetScore;
    }
    
    public void setTargetScore(Integer targetScore) {
        this.targetScore = targetScore;
    }
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

    public boolean isTeamMode() { return teamMode; }
    public void setTeamMode(boolean teamMode) { this.teamMode = teamMode; }

    public Integer getNumTeams() { return numTeams; }
    public void setNumTeams(Integer numTeams) { this.numTeams = numTeams != null ? numTeams : 2; }

    public boolean isPersistentDeck() { return persistentDeck; }
    public void setPersistentDeck(boolean persistentDeck) { this.persistentDeck = persistentDeck; }
}
