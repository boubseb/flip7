package com.flip7.flip7.game.card;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Carte opérateur (+2, +4, +6, +8, +10, ×2)
 */
public class OperatorCard extends Card {
    private OperatorType operatorType;

    public OperatorCard() {
        super(CardType.OPERATOR);
    }

    public OperatorCard(OperatorType operatorType) {
        super(CardType.OPERATOR);
        this.operatorType = operatorType;
    }

    public OperatorType getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(OperatorType operatorType) {
        this.operatorType = operatorType;
    }

    @Override
    @JsonIgnore
    public String getDisplayName() {
        if (operatorType.isMultiply()) {
            return "×" + operatorType.getValue();
        } else {
            return "+" + operatorType.getValue();
        }
    }

    @Override
    public String toString() {
        return "OperatorCard{type=" + operatorType + ", id=" + getId() + "}";
    }
}
