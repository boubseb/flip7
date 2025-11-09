package com.flip7.flip7.game.card;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Carte numérotée (0-12)
 */
public class NumberCard extends Card {
    private int value;

    public NumberCard() {
        super(CardType.NUMBER);
    }

    public NumberCard(int value) {
        super(CardType.NUMBER);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    @JsonIgnore
    public String getDisplayName() {
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return "NumberCard{value=" + value + ", id=" + getId() + "}";
    }
}
