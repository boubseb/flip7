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
    private List<RoundData> rounds;        // Liste de tous les rounds (le dernier = actuel)

    public GamePlayer(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.hand = new ArrayList<>();
        this.status = PlayerStatus.WAITING;
        this.roundScore = 0;
        this.totalScore = 0;
        this.hasUsedLife = false;
        this.rounds = new ArrayList<>();
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
        // Recalculer le score en temps réel
        calculateRoundScore();
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

        System.out.println("      🧮 calculateRoundScore for " + username + " (hand size: " + hand.size() + ")");
        
        // Somme des cartes numérotées et des bonus
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                int value = ((NumberCard) card).getValue();
                score += value;
                System.out.println("         + NumberCard: " + value + " → score = " + score);
            } else if (card instanceof OperatorCard) {
                OperatorCard opCard = (OperatorCard) card;
                if (opCard.getOperatorType().isMultiply()) {
                    hasMultiply = true;
                    System.out.println("         + OperatorCard: ×2 (will multiply at end)");
                } else {
                    int value = opCard.getOperatorType().getValue();
                    score += value;
                    System.out.println("         + OperatorCard: " + opCard.getOperatorType() + " (" + value + ") → score = " + score);
                }
            }
        }

        // Application du ×2 à la fin
        if (hasMultiply) {
            score *= 2;
            System.out.println("         × 2 → final score = " + score);
        }

        this.roundScore = score;
        System.out.println("      ✅ Final roundScore = " + this.roundScore);
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

    public List<RoundData> getRounds() {
        return new ArrayList<>(rounds);
    }

    /**
     * Sauvegarde l'état du round actuel dans la liste des rounds
     */
    public void saveRoundHistory(int roundNumber, List<Card> handSnapshot) {
        RoundData roundData = new RoundData(
            roundNumber,
            this.roundScore,
            this.totalScore,
            this.status,
            handSnapshot
        );
        rounds.add(roundData);
    }

    /**
     * Classe interne pour stocker les données d'un round
     */
    public static class RoundData {
        private int roundNumber;
        private int roundScore;
        private int totalScore;
        private int theoreticalTotal;
        private PlayerStatus status;
        private List<Card> hand;

        public RoundData(int roundNumber, int roundScore, int totalScore, PlayerStatus status, List<Card> hand) {
            this.roundNumber = roundNumber;
            this.roundScore = roundScore;
            this.totalScore = totalScore;
            this.theoreticalTotal = totalScore + roundScore;
            this.status = status;
            this.hand = hand;
        }

        public int getRoundNumber() { return roundNumber; }
        public int getRoundScore() { return roundScore; }
        public int getTotalScore() { return totalScore; }
        public int getTheoreticalTotal() { return theoreticalTotal; }
        public PlayerStatus getStatus() { return status; }
        public List<Card> getHand() { return hand; }
    }
}
