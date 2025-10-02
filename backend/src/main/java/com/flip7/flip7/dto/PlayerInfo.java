package com.flip7.flip7.dto;

public class PlayerInfo {
    private String id;
    private String pseudo;
    
    public PlayerInfo(String id, String pseudo) {
        this.id = id;
        this.pseudo = pseudo;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPseudo() {
        return pseudo;
    }
    
    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }
}
