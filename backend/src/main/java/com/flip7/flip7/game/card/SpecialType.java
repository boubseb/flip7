package com.flip7.flip7.game.card;

/**
 * Types de cartes spéciales
 */
public enum SpecialType {
    STOP,          // Force un joueur à arrêter de piocher
    DRAW_THREE,    // Force un joueur à piocher 3 cartes
    LIFE           // Protection contre un double (consommée à l'utilisation)
}
