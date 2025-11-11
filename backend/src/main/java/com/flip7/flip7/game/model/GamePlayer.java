package com.flip7.flip7.game.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flip7.flip7.game.card.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private int remainingForcedDraws;      // Nombre de cartes restant à piocher (pour DrawThree interrompu)
    
    // Contexte des actions spéciales
    private String stoppedByUserId;        // ID du joueur qui a donné la carte Stop (null si auto-stop)
    private String stoppedByUsername;      // Nom du joueur qui a donné la carte Stop
    private String drawThreeByUserId;      // ID du joueur qui a donné le +3 (pour élimination pendant +3)
    private String drawThreeByUsername;    // Nom du joueur qui a donné le +3

    // Constructeur par défaut pour Jackson
    public GamePlayer() {
        this.hand = new ArrayList<>();
        this.status = PlayerStatus.PLAYING;
        this.roundScore = 0;
        this.totalScore = 0;
        this.hasUsedLife = false;
        this.lifeCardsInHand = 0;
        this.rounds = new ArrayList<>();
        this.remainingForcedDraws = 0;
    }

    @JsonCreator
    public GamePlayer(
        @JsonProperty("userId") String userId, 
        @JsonProperty("username") String username
    ) {
        this.userId = userId;
        this.username = username;
        this.hand = new ArrayList<>();
        this.status = PlayerStatus.WAITING;
        this.roundScore = 0;
        this.totalScore = 0;
        this.hasUsedLife = false;
        this.rounds = new ArrayList<>();
        this.lifeCardsInHand = 0;
        this.remainingForcedDraws = 0;
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
    public boolean removeCard(Card card) {
        boolean removed = hand.remove(card);
        if (removed && card instanceof SpecialCard && ((SpecialCard) card).getSpecialType() == SpecialType.LIFE) {
            lifeCardsInHand--;
        }
        return removed;
    }

    /**
     * Vérifie si le joueur a un double (2 cartes numérotées identiques)
     */
    public boolean hasDouble() {
        Map<Integer, Integer> numberCount = new HashMap<>();
        for (Card card : hand) {
            if (card instanceof NumberCard && !card.isCancelled()) {
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
     * Vérifie si le joueur a réalisé un Flip7
     * Condition : 7 cartes numérotées (NumberCard) différentes, non barrées
     * Les +2, +10, ×2 et cartes spéciales ne comptent PAS
     */
    public boolean hasFlip7() {
        Set<Integer> uniqueNumbers = new HashSet<>();
        for (Card card : hand) {
            // Seulement les NumberCard non-barrées
            if (card instanceof NumberCard && !card.isCancelled()) {
                int value = ((NumberCard) card).getValue();
                uniqueNumbers.add(value);
            }
        }
        return uniqueNumbers.size() >= 7;
    }

    /**
     * Calcule le score du round
     */
    public int calculateRoundScore() {
        int numberCardsScore = 0;  // Score des cartes numérotées (peut être multiplié par ×2)
        int operatorCardsScore = 0; // Score des cartes opérateurs (+2, +10, etc. - JAMAIS multiplié)
        boolean hasMultiply = false;

        System.out.println("      🧮 calculateRoundScore for " + username + " (hand size: " + hand.size() + ")");
        
        // Somme des cartes numérotées et des bonus (ignorer les cartes barrées)
        for (Card card : hand) {
            if (card.isCancelled()) {
                System.out.println("         ✖️ " + card.getDisplayName() + " (barrée, ignorée)");
                continue;
            }
            
            if (card instanceof NumberCard) {
                int value = ((NumberCard) card).getValue();
                numberCardsScore += value;
                System.out.println("         + NumberCard: " + value + " → numberCardsScore = " + numberCardsScore);
            } else if (card instanceof OperatorCard) {
                OperatorCard opCard = (OperatorCard) card;
                if (opCard.getOperatorType().isMultiply()) {
                    hasMultiply = true;
                    System.out.println("         + OperatorCard: ×2 (will multiply numberCards only)");
                } else {
                    int value = opCard.getOperatorType().getValue();
                    operatorCardsScore += value;
                    System.out.println("         + OperatorCard: " + opCard.getOperatorType() + " (" + value + ") → operatorCardsScore = " + operatorCardsScore);
                }
            }
        }

        // Application du ×2 UNIQUEMENT sur les cartes numérotées
        int finalScore = operatorCardsScore; // Les opérateurs ne sont JAMAIS multipliés
        if (hasMultiply) {
            finalScore += (numberCardsScore * 2);
            System.out.println("         ×2 appliqué uniquement aux NumberCards: " + numberCardsScore + " × 2 = " + (numberCardsScore * 2));
            System.out.println("         Score final = operatorCards(" + operatorCardsScore + ") + numberCards×2(" + (numberCardsScore * 2) + ") = " + finalScore);
        } else {
            finalScore += numberCardsScore;
            System.out.println("         Score final = operatorCards(" + operatorCardsScore + ") + numberCards(" + numberCardsScore + ") = " + finalScore);
        }

        this.roundScore = finalScore;
        System.out.println("      ✅ Final roundScore = " + this.roundScore);
        return finalScore;
    }

    /**
     * Utilise une carte Vie pour survivre à un double
     * Marque la carte Vie comme utilisée et la carte du double comme barrée
     */
    public boolean useLifeCard() {
        SpecialCard lifeCard = null;
        Card doubleCard = null;
        
        // Trouver la carte Vie
        for (Card card : hand) {
            if (card instanceof SpecialCard && ((SpecialCard) card).getSpecialType() == SpecialType.LIFE && !((SpecialCard) card).isUsed()) {
                lifeCard = (SpecialCard) card;
                break;
            }
        }
        
        if (lifeCard == null) {
            return false;
        }
        
        // Trouver la carte qui a causé le double (dernière carte ajoutée qui crée un double)
        Map<Integer, Card> numberCards = new HashMap<>();
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                int value = ((NumberCard) card).getValue();
                if (numberCards.containsKey(value) && !card.isCancelled()) {
                    // C'est la carte du double
                    doubleCard = card;
                } else {
                    numberCards.put(value, card);
                }
            }
        }
        
        // Marquer la carte Vie comme utilisée (ne plus compter dans lifeCardsInHand)
        lifeCard.setUsed(true);
        lifeCardsInHand--;
        hasUsedLife = true;
        
        // Barrer la carte qui a causé le double
        if (doubleCard != null) {
            doubleCard.setCancelled(true);
            System.out.println("      ❤️ Carte Vie utilisée ! Carte " + doubleCard.getDisplayName() + " barrée.");
        }
        
        // Recalculer le score sans la carte barrée
        calculateRoundScore();
        
        return true;
    }

    /**
     * Réinitialise pour un nouveau round
     */
    public void resetForNewRound() {
        System.out.println("      🧹 RESET " + username + ": clearing " + hand.size() + " cards, status was " + status);
        hand.clear();
        status = PlayerStatus.PLAYING; // NOUVEAU: Les joueurs commencent directement en PLAYING (pas de distribution)
        roundScore = 0;
        hasUsedLife = false;
        lifeCardsInHand = 0;
        
        // Réinitialiser le contexte des actions spéciales
        stoppedByUserId = null;
        stoppedByUsername = null;
        drawThreeByUserId = null;
        drawThreeByUsername = null;
        
        System.out.println("      ✅ RESET " + username + ": hand cleared, status=" + status);
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

    public void setRoundScore(int roundScore) {
        this.roundScore = roundScore;
    }

    public void resetRoundScore() {
        this.roundScore = 0;
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

    @JsonIgnore
    public int getHandSize() {
        return hand.size();
    }

    public List<RoundData> getRounds() {
        return new ArrayList<>(rounds);
    }

    public int getRemainingForcedDraws() {
        return remainingForcedDraws;
    }

    public void setRemainingForcedDraws(int remainingForcedDraws) {
        this.remainingForcedDraws = remainingForcedDraws;
    }

    /**
     * Sauvegarde l'état du round actuel dans la liste des rounds
     * À ce stade, totalScore contient déjà le score du round (addRoundScoreToTotal a été appelé)
     */
    public void saveRoundHistory(int roundNumber, List<Card> handSnapshot) {
        // À ce stade, this.totalScore contient déjà this.roundScore (après addRoundScoreToTotal)
        // On veut sauvegarder :
        // - totalScore du round précédent = this.totalScore - this.roundScore
        // - roundScore du round actuel = this.roundScore
        // - theoreticalTotal = totalScore précédent + roundScore actuel = this.totalScore
        
        int previousTotalScore = this.totalScore - this.roundScore;  // Score cumulé AVANT ce round
        int currentRoundScore = this.roundScore;                      // Score de CE round
        int currentTheoreticalTotal = this.totalScore;                // Score cumulé APRÈS ce round
        
        System.out.println("      📝 saveRoundHistory for " + username + " round " + roundNumber + ":");
        System.out.println("         totalScore (précédent): " + previousTotalScore);
        System.out.println("         roundScore (actuel): " + currentRoundScore);
        System.out.println("         theoreticalTotal: " + currentTheoreticalTotal);
        
        RoundData roundData = new RoundData(
            roundNumber,
            currentRoundScore,
            previousTotalScore,
            currentTheoreticalTotal,
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

        // Constructeur par défaut pour Jackson
        public RoundData() {
            this.hand = new ArrayList<>();
        }

        @JsonCreator
        public RoundData(
            @JsonProperty("roundNumber") int roundNumber, 
            @JsonProperty("roundScore") int roundScore, 
            @JsonProperty("totalScore") int totalScore, 
            @JsonProperty("theoreticalTotal") int theoreticalTotal, 
            @JsonProperty("status") PlayerStatus status, 
            @JsonProperty("hand") List<Card> hand
        ) {
            this.roundNumber = roundNumber;
            this.roundScore = roundScore;
            this.totalScore = totalScore;
            this.theoreticalTotal = theoreticalTotal;
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

    // Getters/Setters pour le contexte des actions spéciales
    public String getStoppedByUserId() { return stoppedByUserId; }
    public void setStoppedByUserId(String stoppedByUserId) { this.stoppedByUserId = stoppedByUserId; }
    
    public String getStoppedByUsername() { return stoppedByUsername; }
    public void setStoppedByUsername(String stoppedByUsername) { this.stoppedByUsername = stoppedByUsername; }
    
    public String getDrawThreeByUserId() { return drawThreeByUserId; }
    public void setDrawThreeByUserId(String drawThreeByUserId) { this.drawThreeByUserId = drawThreeByUserId; }
    
    public String getDrawThreeByUsername() { return drawThreeByUsername; }
    public void setDrawThreeByUsername(String drawThreeByUsername) { this.drawThreeByUsername = drawThreeByUsername; }
}
