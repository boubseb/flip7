package com.flip7.flip7.game.card;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Carte spéciale (Stop, +3 cartes, Vie)
 */
public class SpecialCard extends Card {
    private SpecialType specialType;
    private boolean used; // Carte Vie utilisée (devient noire)
    private boolean pending; // Carte Stop en attente d'assignation
    private String assignedToPlayerId; // ID du joueur qui recevra la carte Stop

    public SpecialCard() {
        super(CardType.SPECIAL);
        this.used = false;
        this.pending = false;
        this.assignedToPlayerId = null;
    }

    public SpecialCard(SpecialType specialType) {
        super(CardType.SPECIAL);
        this.specialType = specialType;
        this.used = false;
        // Les cartes Stop et DrawThree sont pending par défaut (nécessitent une assignation)
        this.pending = (specialType == SpecialType.STOP || specialType == SpecialType.DRAW_THREE);
        this.assignedToPlayerId = null;
    }

    public SpecialType getSpecialType() {
        return specialType;
    }

    public void setSpecialType(SpecialType specialType) {
        this.specialType = specialType;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public String getAssignedToPlayerId() {
        return assignedToPlayerId;
    }

    public void setAssignedToPlayerId(String assignedToPlayerId) {
        this.assignedToPlayerId = assignedToPlayerId;
    }

    @Override
    @JsonIgnore
    public String getDisplayName() {
        switch (specialType) {
            case STOP:
                return "STOP";
            case DRAW_THREE:
                return "+3 Cartes";
            case LIFE:
                return "VIE";
            default:
                return "SPECIAL";
        }
    }

    @Override
    public String toString() {
        return "SpecialCard{type=" + specialType + ", id=" + getId() + "}";
    }
}
