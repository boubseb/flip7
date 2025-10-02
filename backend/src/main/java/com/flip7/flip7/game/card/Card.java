package com.flip7.flip7.game.card;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Classe abstraite représentant une carte du jeu Flip7
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "cardType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NumberCard.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = OperatorCard.class, name = "OPERATOR"),
    @JsonSubTypes.Type(value = SpecialCard.class, name = "SPECIAL")
})
public abstract class Card {
    private String id; // Identifiant unique de la carte
    private CardType cardType;

    public Card(CardType cardType) {
        this.cardType = cardType;
        this.id = java.util.UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    /**
     * Retourne une représentation textuelle de la carte
     */
    public abstract String getDisplayName();
}
