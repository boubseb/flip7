package com.flip7.flip7.game.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deck de cartes Flip7
 * Composition :
 * - 12 cartes "12", 11 cartes "11", ..., 2 cartes "2", 1 carte "1", 1 carte "0"
 * - 1 ×2, 1 +2, 1 +4, 1 +6, 1 +8, 1 +10
 * - 3 Stop, 3 +3 cartes, 3 Vie
 * Total : 94 cartes
 */
public class Deck {
    private List<Card> cards;
    private List<Card> discardPile;

    public Deck() {
        this.cards = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        initializeDeck();
    }

    /**
     * Initialise le deck avec toutes les cartes
     */
    private void initializeDeck() {
        // Cartes numérotées : 12 cartes "12", 11 cartes "11", ..., 1 carte "1", 1 carte "0"
        for (int value = 12; value >= 2; value--) {
            for (int count = 0; count < value; count++) {
                cards.add(new NumberCard(value));
            }
        }
        // 1 carte "1" et 1 carte "0"
        cards.add(new NumberCard(1));
        cards.add(new NumberCard(0));

        // Cartes opérateurs
        cards.add(new OperatorCard(OperatorType.MULTIPLY_2));
        cards.add(new OperatorCard(OperatorType.PLUS_2));
        cards.add(new OperatorCard(OperatorType.PLUS_4));
        cards.add(new OperatorCard(OperatorType.PLUS_6));
        cards.add(new OperatorCard(OperatorType.PLUS_8));
        cards.add(new OperatorCard(OperatorType.PLUS_10));

        // Cartes spéciales (3 de chaque)
        for (int i = 0; i < 3; i++) {
            cards.add(new SpecialCard(SpecialType.STOP));
            cards.add(new SpecialCard(SpecialType.DRAW_THREE));
            cards.add(new SpecialCard(SpecialType.LIFE));
        }
    }

    /**
     * Mélange le deck
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Pioche une carte du deck
     */
    public Card draw() {
        if (cards.isEmpty()) {
            // Si le deck est vide, on mélange la défausse et on la remet dans le deck
            if (discardPile.isEmpty()) {
                throw new RuntimeException("Plus de cartes disponibles !");
            }
            cards.addAll(discardPile);
            discardPile.clear();
            shuffle();
        }
        return cards.remove(0);
    }

    /**
     * Défausse une carte
     */
    public void discard(Card card) {
        discardPile.add(card);
    }

    /**
     * Défausse plusieurs cartes
     */
    public void discardAll(List<Card> cardsToDiscard) {
        discardPile.addAll(cardsToDiscard);
    }

    /**
     * Réinitialise le deck pour un nouveau round
     */
    public void reset() {
        cards.clear();
        discardPile.clear();
        initializeDeck();
        shuffle();
    }

    public int getRemainingCards() {
        return cards.size();
    }

    public int getDiscardPileSize() {
        return discardPile.size();
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
}
