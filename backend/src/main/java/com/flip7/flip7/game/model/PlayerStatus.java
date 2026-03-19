package com.flip7.flip7.game.model;

/**
 * Statut d'un joueur dans un round
 */
public enum PlayerStatus {
    PLAYING,      // En train de jouer
    STOPPED,      // A décidé volontairement de s'arrêter
    FORCED_STOP,  // Forcé de s'arrêter par une carte Stop (carte ajoutée à son jeu)
    ELIMINATED,   // Éliminé (double carte)
    WAITING,      // En attente (pas encore son tour ou doit piocher suite à une carte spéciale)
    FLIP7_STOP    // A réussi un Flip7 (7 cartes numérotées différentes) - arrête le round et gagne +15 points
}
