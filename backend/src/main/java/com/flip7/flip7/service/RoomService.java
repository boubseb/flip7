// ...existing code...
package com.flip7.flip7.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
    /**
     * Permet à un ancien joueur de rejoindre la room après un restart sans mot de passe
     */
    public Room rejoinAfterRestart(String roomId, String playerId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        // Vérifier si le joueur est dans previousPlayers
        if (room.getPlayers().contains(playerId)) {
            // Déjà dans la room (reconnexion)
            return room;
        }
        List<String> previousPlayers = room.getPreviousPlayers();
        if (previousPlayers == null || !previousPlayers.contains(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to rejoin without password");
        }
        // Ajouter le joueur à la room
        room.getPlayers().add(playerId);
        // Retirer le joueur de previousPlayers
        previousPlayers.remove(playerId);
        room.setPreviousPlayers(previousPlayers);
        room = roomRepository.save(room);
        broadcastRoomUpdate(room);
        return room;
    }
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired(required = false)
    @Lazy
    private GameService gameService;
    
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
        System.out.println("[JOIN] roomId=" + roomId + " status(BDD)=" + room.getStatus());
        
        if (!passwordEncoder.matches(password, room.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }
        
        // Mettre à jour l'activité de la room
        room.updateActivity();
        
        // Si le joueur était déjà dans la room, c'est une reconnexion - toujours autorisée
        if (room.getPlayers().contains(playerId)) {
            System.out.println("🔄 Reconnexion du joueur " + playerId + " à la room " + roomId);
            // Notifier les autres joueurs de la reconnexion
            roomRepository.save(room);
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
        System.out.println("[START] roomId=" + roomId + " status(BDD)=" + room.getStatus());
        
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
    
    public void broadcastRoomUpdate(Room room) {
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
    
    // Getter pour RoomRepository (utilisé par GameService)
    public RoomRepository getRoomRepository() {
        return roomRepository;
    }
    
    /**
     * Supprime toutes les rooms
     * @return le nombre de rooms supprimées
     */
    public int deleteAllRooms() {
        List<Room> allRooms = roomRepository.findAll();
        int count = allRooms.size();
        roomRepository.deleteAll();
        System.out.println("🗑️ Toutes les rooms ont été supprimées (" + count + " rooms)");
        return count;
    }
    
    /**
     * Tâche planifiée pour supprimer les rooms inactives depuis plus de 30 minutes
     * S'exécute toutes les 5 minutes (300000 ms)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes en millisecondes
    public void cleanupInactiveRooms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyMinutesAgo = now.minusMinutes(30);
        
        System.out.println("🧹 [" + now + "] Exécution du nettoyage des rooms inactives (> 30 min)...");
        
        List<Room> allRooms = roomRepository.findAll();
        System.out.println("   📊 Total de rooms dans la base: " + allRooms.size());
        
        int deletedCount = 0;
        for (Room room : allRooms) {
            LocalDateTime lastActivity = room.getLastActivityAt();
            // Si lastActivityAt est null (anciennes rooms), utiliser createdAt
            if (lastActivity == null) {
                lastActivity = room.getCreatedAt();
            }
            
            if (lastActivity != null && lastActivity.isBefore(thirtyMinutesAgo)) {
                long minutesInactive = java.time.Duration.between(lastActivity, now).toMinutes();
                System.out.println("   🗑️ Suppression de la room " + room.getId() + 
                    " (inactive depuis " + minutesInactive + " minutes - dernière activité: " + lastActivity + ")");
                roomRepository.delete(room);
                deletedCount++;
            }
        }
        
        if (deletedCount > 0) {
            System.out.println("   ✅ " + deletedCount + " room(s) inactive(s) supprimée(s)");
        } else {
            System.out.println("   ✅ Aucune room inactive à supprimer");
        }
        
        // Nettoyer aussi les games actifs en mémoire si GameService est disponible
        if (gameService != null) {
            try {
                gameService.cleanupInactiveGames();
            } catch (Exception e) {
                System.out.println("   ⚠️ Erreur lors du nettoyage des games en mémoire: " + e.getMessage());
            }
        }
    }
}
