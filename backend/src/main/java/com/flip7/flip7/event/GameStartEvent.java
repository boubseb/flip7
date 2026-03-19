package com.flip7.flip7.event;

import org.springframework.context.ApplicationEvent;

/**
 * Événement publié quand une partie démarre
 * Utilisé pour découpler RoomService et GameService
 */
public class GameStartEvent extends ApplicationEvent {
    
    private final String roomId;
    
    public GameStartEvent(Object source, String roomId) {
        super(source);
        this.roomId = roomId;
    }
    
    public String getRoomId() {
        return roomId;
    }
}
