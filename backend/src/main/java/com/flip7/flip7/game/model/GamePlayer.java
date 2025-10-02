package com.flip7.flip7.game.model;

import com.flip7.flip7.game.card.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente un joueur dans le contexte du jeu
 */
public class GamePlayer {
    private String userId;
    private String username;
    private List<Card> hand;              // Main du joueur
    private PlayerStatus status;           // Statut actuel
    private int roundScore;                // Score du round en cours
    private int totalScore;                // Score total sur tous les rounds
    private boolean hasUsedLife;           // A utilisé une carte Vie ce round
    private int lifeCardsInHand;          // Nombre de cartes Vie disponibles

    public GamePlayer(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.hand = new ArrayList<>();
        this.status = PlayerStatus.WAITING;
        this.roundScore = 0;
        this.totalScore = 0;
        this.hasUsedLife = false;
        this.lifeCardsInHand = 0;
    }

    /**
     * Ajoute une carte à la main du joueur
     */
    public void addCard(Card card) {
        hand.add(card);
        if (card instanceof SpecialCard && ((SpecialCard) card).getSpecialType() == SpecialType.LIFE) {
            lifeCardsInHand++;
        }
    }

    /**
     * Retire une carte de la main
     */
    public void removeCard(Card card) {
        hand.remove(card);
        if (card instanceof SpecialCard && ((SpecialCard) card).getSpecialType() == SpecialType.LIFE) {
            lifeCardsInHand--;
        }
    }

    /**
     * Vérifie si le joueur a un double (2 cartes numérotées identiques)
     */
    public boolean hasDouble() {
        Map<Integer, Integer> numberCount = new HashMap<>();
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                int value = ((NumberCard) card).getValue();
                numberCount.put(value, numberCount.getOrDefault(value, 0) + 1);
                if (numberCount.get(value) >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Vérifie si le joueur a 7 cartes numérotées différentes
     */
    public boolean hasSevenDifferentNumbers() {
        List<Integer> uniqueNumbers = new ArrayList<>();
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                int value = ((NumberCard) card).getValue();
                if (!uniqueNumbers.contains(value)) {
                    uniqueNumbers.add(value);
                }
            }
        }
        return uniqueNumbers.size() >= 7;
    }

    /**
     * Calcule le score du round
     */
    public int calculateRoundScore() {
        int score = 0;
        boolean hasMultiply = false;

        // Somme des cartes numérotées et des bonus
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                score += ((NumberCard) card).getValue();
            } else if (card instanceof OperatorCard) {
                OperatorCard opCard = (OperatorCard) card;
                if (opCard.getOperatorType().isMultiply()) {
                    hasMultiply = true;
                } else {
                    score += opCard.getOperatorType().getValue();
                }
            }
        }

        // Application du ×2 à la fin
        if (hasMultiply) {
            score *= 2;
        }

        this.roundScore = score;
        return score;
    }

    /**
     * Utilise une carte Vie pour survivre à un double
     */
    public boolean useLifeCard() {
        for (Card card : hand) {
            if (card instanceof SpecialCard && ((SpecialCard) card).getSpecialType() == SpecialType.LIFE) {
                removeCard(card);
                hasUsedLife = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Réinitialise pour un nouveau round
     */
    public void resetForNewRound() {
        hand.clear();
        status = PlayerStatus.WAITING;
        roundScore = 0;
        hasUsedLife = false;
        lifeCardsInHand = 0;
    }

    /**
     * Ajoute le score du round au score total
     */
    public void addRoundScoreToTotal() {
        totalScore += roundScore;
    }

    // Getters et Setters
    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public boolean hasUsedLife() {
        return hasUsedLife;
    }

    public int getLifeCardsInHand() {
        return lifeCardsInHand;
    }

    public int getHandSize() {
        return hand.size();
    }
}
