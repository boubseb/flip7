package com.flip7.flip7.game.model;

import com.flip7.flip7.game.card.SpecialCard;

/**
 * Représente une carte spéciale en attente d'assignation avec tout son contexte.
 * Utilisé pour gérer la queue de cartes spéciales à traiter.
 */
public class PendingSpecialCard {
    private SpecialCard card;
    private String sourcePlayerId; // Joueur qui a pioché/donné la carte
    private String targetPlayerId; // Joueur cible (null si pas encore assigné)
    private int remainingForcedDraws; // Nombre de cartes restantes à piocher après résolution
    private long timestamp; // Pour l'ordre de traitement
    private int priority; // Pour prioriser certaines cartes si nécessaire

    public PendingSpecialCard(SpecialCard card, String sourcePlayerId, String targetPlayerId, int remainingForcedDraws) {
        this.card = card;
        this.sourcePlayerId = sourcePlayerId;
        this.targetPlayerId = targetPlayerId;
        this.remainingForcedDraws = remainingForcedDraws;
        this.timestamp = System.currentTimeMillis();
        this.priority = 0; // Par défaut
    }

    // Getters et setters
    public SpecialCard getCard() {
        return card;
    }

    public void setCard(SpecialCard card) {
        this.card = card;
    }

    public String getSourcePlayerId() {
        return sourcePlayerId;
    }

    public void setSourcePlayerId(String sourcePlayerId) {
        this.sourcePlayerId = sourcePlayerId;
    }

    public String getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(String targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public int getRemainingForcedDraws() {
        return remainingForcedDraws;
    }

    public void setRemainingForcedDraws(int remainingForcedDraws) {
        this.remainingForcedDraws = remainingForcedDraws;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isAssigned() {
        return targetPlayerId != null;
    }
    
    @Override
    public String toString() {
        return "PendingSpecialCard{" +
                "card=" + card.getSpecialType() +
                ", source=" + sourcePlayerId +
                ", target=" + (targetPlayerId != null ? targetPlayerId : "NOT_ASSIGNED") +
                ", remainingDraws=" + remainingForcedDraws +
                '}';
    }
}
