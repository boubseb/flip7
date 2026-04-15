package com.flip7.flip7.repository;

import com.flip7.flip7.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour l'historique des parties
 */
@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, String> {
    
    /**
     * Trouve toutes les parties d'une room
     */
    List<GameHistory> findByRoomId(String roomId);
    
    /**
     * Trouve toutes les parties où un joueur a participé
     */
    List<GameHistory> findByPlayerIdsContaining(String playerId);
    
    /**
     * Trouve les parties terminées d'un joueur
     */
    List<GameHistory> findByPlayerIdsContainingAndStatus(String playerId, GameHistory.GameStatus status);
    
    /**
     * Trouve les parties gagnées par un joueur
     */
    List<GameHistory> findByWinnerId(String playerId);
    
    /**
     * Trouve les parties dans une période
     */
    List<GameHistory> findByStartedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Trouve les parties en cours
     */
    List<GameHistory> findByStatus(GameHistory.GameStatus status);

    /**
     * Vérifie si une room a au moins une partie dans un statut donné
     */
    boolean existsByRoomIdAndStatus(String roomId, GameHistory.GameStatus status);
}
