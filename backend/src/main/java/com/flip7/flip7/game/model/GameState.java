package com.flip7.flip7.game.model;

/**
 * État du jeu
 */
public enum GameState {
    WAITING,           // En attente de joueurs
    DISTRIBUTING,      // Distribution initiale (1 carte par joueur avec délai)
    PLAYING,           // Round en cours
    ROUND_ENDED,       // Round terminé, calcul des scores
    WAITING_NEXT_ROUND, // En attente que le prochain joueur démarre le round
    GAME_OVER          // Partie terminée (un joueur a atteint 200 points)
}
