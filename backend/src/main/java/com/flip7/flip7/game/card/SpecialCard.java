package com.flip7.flip7.game.card;

/**
 * Carte spéciale (Stop, +3 cartes, Vie)
 */
public class SpecialCard extends Card {
    private SpecialType specialType;

    public SpecialCard() {
        super(CardType.SPECIAL);
    }

    public SpecialCard(SpecialType specialType) {
        super(CardType.SPECIAL);
        this.specialType = specialType;
    }

    public SpecialType getSpecialType() {
        return specialType;
    }

    public void setSpecialType(SpecialType specialType) {
        this.specialType = specialType;
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
