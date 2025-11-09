package com.flip7.flip7.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.flip7.flip7.dto.PlayerInfo;
import com.flip7.flip7.dto.RoomResponse;
import com.flip7.flip7.entity.Room;
import com.flip7.flip7.entity.User;
import com.flip7.flip7.entity.Room.RoomStatus;
import com.flip7.flip7.event.GameStartEvent;
import com.flip7.flip7.repository.RoomRepository;
import com.flip7.flip7.repository.UserRepository;

@Service
public class RoomService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public Room createRoom(String password, String adminId, Integer maxPlayers) {
        Room room = new Room();
        room.setPassword(passwordEncoder.encode(password));
        room.setAdminId(adminId);
        room.setMaxPlayers(maxPlayers != null ? maxPlayers : 12);
        room.getPlayers().add(adminId);
        room.setStatus(RoomStatus.WAITING);
        
        return roomRepository.save(room);
    }
    
    public Room joinRoom(String roomId, String password, String playerId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        
        if (!passwordEncoder.matches(password, room.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }
        
        // Si le joueur était déjà dans la room, c'est une reconnexion - toujours autorisée
        if (room.getPlayers().contains(playerId)) {
            System.out.println("🔄 Reconnexion du joueur " + playerId + " à la room " + roomId);
            // Notifier les autres joueurs de la reconnexion
            broadcastRoomUpdate(room);
            return room;
        }
        
        // Nouvelle connexion - vérifier les conditions
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game already started - cannot join");
        }
        
        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is full");
        }
        
        // Ajouter le nouveau joueur
        room.getPlayers().add(playerId);
        room = roomRepository.save(room);
        
        System.out.println("➕ Nouveau joueur " + playerId + " rejoint la room " + roomId);
        
        // Notify all players in room
        broadcastRoomUpdate(room);
        
        return room;
    }
    
    public Room kickPlayer(String roomId, String adminId, String playerToKick) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        
        if (!room.getAdminId().equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can kick players");
        }
        
        if (playerToKick.equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot kick themselves");
        }
        
        if (!room.getPlayers().contains(playerToKick)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in room");
        }
        
        room.getPlayers().remove(playerToKick);
        room = roomRepository.save(room);
        
        // Broadcast room update to all remaining players
        broadcastRoomUpdate(room);
        
        return room;
    }
    
    public Room startGame(String roomId, String adminId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        
        if (!room.getAdminId().equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can start the game");
        }
        
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game already started");
        }
        
        if (room.getPlayers().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 players");
        }
        
        room.setStatus(RoomStatus.IN_GAME);
        room.setTurnIndex(0);
        room.setGameState("{}");
        
        room = roomRepository.save(room);
        
        // Publier un événement pour initialiser le jeu (découplage pour éviter dépendance circulaire)
        eventPublisher.publishEvent(new GameStartEvent(this, roomId));
        System.out.println("✅ GameStartEvent published for room: " + roomId);
        
        // Broadcast game start to all players
        broadcastGameStart(room);
        
        return room;
    }
    
    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }
    
    public List<Room> getAvailableRooms() {
        // Retourne les rooms en WAITING (accessibles à tous)
        List<Room> waitingRooms = roomRepository.findByStatus(RoomStatus.WAITING);
        
        // Retourne aussi les rooms en IN_GAME (pour que les joueurs puissent les rejoindre)
        List<Room> inGameRooms = roomRepository.findByStatus(RoomStatus.IN_GAME);
        
        // Combine les deux listes
        waitingRooms.addAll(inGameRooms);
        
        return waitingRooms;
    }
    
    public Room playTurn(String roomId, String playerId, String moveData) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        
        if (room.getStatus() != RoomStatus.IN_GAME) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game not in progress");
        }
        
        String currentPlayerId = room.getPlayers().get(room.getTurnIndex());
        if (!currentPlayerId.equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");
        }
        
        // Process the move (add your game logic here)
        // Update gameState with moveData
        
        // Move to next player
        int nextTurn = (room.getTurnIndex() + 1) % room.getPlayers().size();
        room.setTurnIndex(nextTurn);
        
        room = roomRepository.save(room);
        
        // Broadcast turn update
        broadcastTurnUpdate(room, moveData);
        
        return room;
    }
    
    public RoomResponse toRoomResponse(Room room) {
        // Fetch player infos with pseudos
        List<PlayerInfo> playerInfos = room.getPlayers().stream()
            .map(playerId -> {
                User user = userRepository.findById(playerId).orElse(null);
                String pseudo = user != null ? user.getPseudo() : "Unknown";
                return new PlayerInfo(playerId, pseudo);
            })
            .collect(Collectors.toList());
        
        RoomResponse response = new RoomResponse(
            room.getId(),
            room.getAdminId(),
            room.getPlayers(),
            room.getStatus(),
            room.getTurnIndex(),
            room.getCreatedAt(),
            room.getMaxPlayers()
        );
        response.setPlayerInfos(playerInfos);
        return response;
    }
    
    private void broadcastRoomUpdate(Room room) {
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + room.getId(),
            toRoomResponse(room)
        );
    }
    
    private void broadcastGameStart(Room room) {
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + room.getId() + "/start",
            toRoomResponse(room)
        );
    }
    
    private void broadcastTurnUpdate(Room room, String moveData) {
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + room.getId() + "/turn",
            new TurnUpdate(toRoomResponse(room), moveData)
        );
    }
    
    // Inner class for turn updates
    public static class TurnUpdate {
        private RoomResponse room;
        private String moveData;
        
        public TurnUpdate(RoomResponse room, String moveData) {
            this.room = room;
            this.moveData = moveData;
        }
        
        public RoomResponse getRoom() {
            return room;
        }
        
        public String getMoveData() {
            return moveData;
        }
    }
    
    // Getter pour UserService (utilisé par GameService)
    public UserRepository getUserRepository() {
        return userRepository;
    }
}
