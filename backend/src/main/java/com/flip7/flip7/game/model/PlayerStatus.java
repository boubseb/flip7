package com.flip7.flip7.game.model;

/**
 * Statut d'un joueur dans un round
 */
public enum PlayerStatus {
    PLAYING,      // En train de jouer
    STOPPED,      // A décidé de s'arrêter
    ELIMINATED,   // Éliminé (double carte)
    WAITING       // En attente (pas encore son tour ou doit piocher suite à une carte spéciale)
}
