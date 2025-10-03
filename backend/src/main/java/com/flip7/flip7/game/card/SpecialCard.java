package com.flip7.flip7.game.card;

/**
 * Carte spéciale (Stop, +3 cartes, Vie)
 */
public class SpecialCard extends Card {
    private SpecialType specialType;
    private boolean used; // Carte Vie utilisée (devient noire)

    public SpecialCard() {
        super(CardType.SPECIAL);
        this.used = false;
    }

    public SpecialCard(SpecialType specialType) {
        super(CardType.SPECIAL);
        this.specialType = specialType;
        this.used = false;
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

    @Override
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
