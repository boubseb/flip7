package com.flip7.flip7.game.model;

import com.flip7.flip7.game.card.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe principale gérant la logique du jeu Flip7
 */
public class Game {
    private String roomId;
    private Deck deck;
    private List<GamePlayer> players;
    private int currentPlayerIndex;
    private GameState gameState;
    private int roundNumber;
    private String winnerId;
    private static final int WINNING_SCORE = 200;
    private static final int DISTRIBUTION_DELAY_MS = 5000; // 5 secondes entre chaque distribution

    public Game(String roomId, List<String> playerIds, Map<String, String> playerNames) {
        this.roomId = roomId;
        this.deck = new Deck();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.gameState = GameState.WAITING;
        this.roundNumber = 0;

        // Initialisation des joueurs
        for (String playerId : playerIds) {
            String username = playerNames.getOrDefault(playerId, "Player");
            players.add(new GamePlayer(playerId, username));
        }
    }

    /**
     * Démarre la partie (round 1)
     */
    public void startGame() {
        startNewRound();
    }

    /**
     * Démarre un nouveau round
     */
    public void startNewRound() {
        roundNumber++;
        gameState = GameState.DISTRIBUTING;
        
        // Déterminer le joueur de départ
        if (roundNumber == 1) {
            // Premier round : joueur aléatoire
            currentPlayerIndex = new java.util.Random().nextInt(players.size());
            System.out.println("🎲 First round - Random starting player: " + getCurrentPlayer().getUsername() + " (index " + currentPlayerIndex + ")");
        } else {
            // Rounds suivants : faire tourner le joueur de départ
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            System.out.println("🔄 Round " + roundNumber + " - Starting player rotated to: " + getCurrentPlayer().getUsername() + " (index " + currentPlayerIndex + ")");
        }

        // Réinitialiser les joueurs
        for (GamePlayer player : players) {
            player.resetForNewRound();
        }

        // Réinitialiser et mélanger le deck
        deck.reset();

        // Distribution initiale : 1 carte par joueur
        dealInitialCards();

        gameState = GameState.PLAYING;
        System.out.println("✅ Round " + roundNumber + " started! Current player: " + getCurrentPlayer().getUsername());
    }

    /**
     * Retourne le joueur actuel
     */
    public GamePlayer getCurrentPlayer() {
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex);
        }
        return null;
    }

    /**
     * Passe au joueur suivant
     */
    public void nextPlayer() {
        int attempts = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            attempts++;
            // Si on a fait le tour complet, c'est que tous les joueurs sont stopped ou eliminated
            if (attempts > players.size()) {
                endRound();
                return;
            }
        } while (getCurrentPlayer().getStatus() != PlayerStatus.PLAYING);
    }

    /**
     * Le joueur actuel pioche une carte
     */
    public DrawResult drawCard(String playerId) {
        GamePlayer player = getPlayerById(playerId);
        if (player == null) {
            return new DrawResult(false, "Joueur non trouvé", null);
        }

        if (!player.getUserId().equals(getCurrentPlayer().getUserId())) {
            return new DrawResult(false, "Ce n'est pas votre tour", null);
        }

        if (player.getStatus() != PlayerStatus.PLAYING) {
            return new DrawResult(false, "Vous ne pouvez pas piocher", null);
        }

        // Pioche la carte
        Card drawnCard = deck.draw();
        player.addCard(drawnCard);

        // Vérification si c'est une carte Stop (nécessite un choix)
        if (drawnCard instanceof SpecialCard) {
            SpecialCard specialCard = (SpecialCard) drawnCard;
            if (specialCard.getSpecialType() == SpecialType.STOP) {
                System.out.println("🛑 " + player.getUsername() + " a pioché une carte STOP (en attente d'assignation)");
                // La carte reste pending, le joueur doit choisir à qui l'assigner
                return new DrawResult(true, "Carte Stop piochée ! Choisissez un joueur.", drawnCard, false, false, false, true);
            }
        }

        // Vérification du double
        if (player.hasDouble()) {
            return handleDouble(player, drawnCard);
        }

        // Vérification des 7 cartes différentes (victoire instantanée)
        if (player.hasSevenDifferentNumbers()) {
            player.setStatus(PlayerStatus.STOPPED);
            endRound();
            return new DrawResult(true, "7 cartes différentes ! Vous remportez le round !", drawnCard, true);
        }

        // Passer au joueur suivant après une pioche réussie
        nextPlayer();
        return new DrawResult(true, "Carte piochée", drawnCard);
    }

    /**
     * Gère le cas où un joueur pioche un double
     */
    private DrawResult handleDouble(GamePlayer player, Card drawnCard) {
        // Vérifier si le joueur a une carte Vie
        if (player.getLifeCardsInHand() > 0) {
            // Le joueur peut utiliser sa carte Vie
            player.useLifeCard();
            return new DrawResult(true, "Double ! Carte Vie utilisée pour survivre.", drawnCard, false, true);
        } else {
            // Le joueur est éliminé - son score de round passe à 0
            player.setStatus(PlayerStatus.ELIMINATED);
            player.resetRoundScore();
            System.out.println("💀 " + player.getUsername() + " éliminé ! Score du round remis à 0.");
            nextPlayer();
            return new DrawResult(true, "Double ! Vous êtes éliminé du round.", drawnCard, false, false, true);
        }
    }

    /**
     * Le joueur actuel décide de s'arrêter
     */
    public ActionResult stopDrawing(String playerId) {
        GamePlayer player = getPlayerById(playerId);
        if (player == null) {
            return new ActionResult(false, "Joueur non trouvé");
        }

        if (!player.getUserId().equals(getCurrentPlayer().getUserId())) {
            return new ActionResult(false, "Ce n'est pas votre tour");
        }

        if (player.getStatus() != PlayerStatus.PLAYING) {
            return new ActionResult(false, "Vous ne pouvez pas vous arrêter");
        }

        player.setStatus(PlayerStatus.STOPPED);
        nextPlayer();

        return new ActionResult(true, "Vous avez décidé de vous arrêter");
    }

    /**
     * Assigner une carte Stop à un joueur (appelé après que le joueur ait fait son choix)
     */
    public ActionResult assignStopCard(String playerId, String cardId, String targetPlayerId) {
        GamePlayer player = getPlayerById(playerId);
        if (player == null) {
            return new ActionResult(false, "Joueur non trouvé");
        }

        if (!player.getUserId().equals(getCurrentPlayer().getUserId())) {
            return new ActionResult(false, "Ce n'est pas votre tour");
        }

        // Trouver la carte Stop dans la main du joueur
        Card card = player.getHand().stream()
            .filter(c -> c.getId().equals(cardId))
            .findFirst()
            .orElse(null);

        if (card == null || !(card instanceof SpecialCard)) {
            return new ActionResult(false, "Carte Stop non trouvée");
        }

        SpecialCard stopCard = (SpecialCard) card;
        if (stopCard.getSpecialType() != SpecialType.STOP) {
            return new ActionResult(false, "Cette carte n'est pas une carte Stop");
        }

        if (!stopCard.isPending()) {
            return new ActionResult(false, "Cette carte Stop a déjà été assignée");
        }

        GamePlayer targetPlayer = getPlayerById(targetPlayerId);
        if (targetPlayer == null) {
            return new ActionResult(false, "Joueur cible non trouvé");
        }

        // Assigner la carte au joueur cible
        stopCard.setPending(false);
        stopCard.setAssignedToPlayerId(targetPlayerId);
        
        // Retirer la carte de la main du joueur qui l'a piochée
        player.removeCard(card);
        
        // Ajouter la carte à la main du joueur cible et le forcer à s'arrêter
        targetPlayer.addCard(card);
        targetPlayer.setStatus(PlayerStatus.FORCED_STOP); // FORCED_STOP au lieu de STOPPED
        
        System.out.println("🛑 " + player.getUsername() + " a assigné la carte STOP à " + targetPlayer.getUsername());
        System.out.println("   Score de " + targetPlayer.getUsername() + " : " + targetPlayer.getRoundScore() + " (incluant la carte Stop)");
        
        // Si le joueur s'est stoppé lui-même ou si c'est le joueur actuel qui est stoppé
        if (targetPlayer.getUserId().equals(getCurrentPlayer().getUserId())) {
            nextPlayer();
        } else {
            // Sinon, passer simplement au joueur suivant
            nextPlayer();
        }
        
        return new ActionResult(true, targetPlayer.getUsername() + " a reçu la carte Stop et est forcé de s'arrêter !");
    }

    /**
     * Joue une carte spéciale
     */
    public ActionResult playSpecialCard(String playerId, String cardId, String targetPlayerId) {
        GamePlayer player = getPlayerById(playerId);
        if (player == null) {
            return new ActionResult(false, "Joueur non trouvé");
        }

        // Trouver la carte dans la main du joueur
        Card card = player.getHand().stream()
            .filter(c -> c.getId().equals(cardId))
            .findFirst()
            .orElse(null);

        if (card == null || !(card instanceof SpecialCard)) {
            return new ActionResult(false, "Carte spéciale non trouvée");
        }

        SpecialCard specialCard = (SpecialCard) card;
        GamePlayer targetPlayer = getPlayerById(targetPlayerId);

        if (targetPlayer == null) {
            return new ActionResult(false, "Joueur cible non trouvé");
        }

        // Application de l'effet de la carte
        switch (specialCard.getSpecialType()) {
            case STOP:
                // Les cartes Stop doivent être assignées via assignStopCard
                return new ActionResult(false, "Utilisez assignStopCard pour assigner une carte Stop");

            case DRAW_THREE:
                for (int i = 0; i < 3; i++) {
                    Card drawnCard = deck.draw();
                    targetPlayer.addCard(drawnCard);
                    // Vérifier le double après chaque carte
                    if (targetPlayer.hasDouble() && targetPlayer.getLifeCardsInHand() == 0) {
                        targetPlayer.setStatus(PlayerStatus.ELIMINATED);
                        if (targetPlayer.getUserId().equals(getCurrentPlayer().getUserId())) {
                            nextPlayer();
                        }
                        player.removeCard(card);
                        deck.discard(card);
                        return new ActionResult(true, targetPlayer.getUsername() + " a pioché 3 cartes et a été éliminé !");
                    }
                }
                player.removeCard(card);
                deck.discard(card);
                return new ActionResult(true, targetPlayer.getUsername() + " a pioché 3 cartes");

            case LIFE:
                // La carte Vie est automatiquement utilisée lors d'un double
                return new ActionResult(false, "La carte Vie s'utilise automatiquement lors d'un double");

            default:
                return new ActionResult(false, "Carte spéciale inconnue");
        }
    }

    /**
     * Termine le round et calcule les scores
     */
    private void endRound() {
        gameState = GameState.ROUND_ENDED;

        // Calcul des scores pour tous les joueurs non éliminés
        for (GamePlayer player : players) {
            if (player.getStatus() != PlayerStatus.ELIMINATED) {
                player.calculateRoundScore();
                player.addRoundScoreToTotal();

                // Vérifier si un joueur a atteint 200 points
                if (player.getTotalScore() >= WINNING_SCORE) {
                    winnerId = player.getUserId();
                    gameState = GameState.GAME_OVER;
                }
            }
        }

        // Sauvegarder l'état du round dans chaque joueur
        saveRoundToPlayers();

        // Si le jeu continue, passer en attente du prochain round
        if (gameState != GameState.GAME_OVER) {
            gameState = GameState.WAITING_NEXT_ROUND;
            System.out.println("⏳ En attente que " + getCurrentPlayer().getUsername() + " démarre le round " + (roundNumber + 1));
        }
    }

    /**
     * Sauvegarde l'état du round actuel dans l'historique de chaque joueur
     */
    private void saveRoundToPlayers() {
        for (GamePlayer player : players) {
            List<Card> handSnapshot = new ArrayList<>(player.getHand());
            System.out.println("   💾 Saving " + player.getUsername() + " round " + roundNumber + " with " + handSnapshot.size() + " cards");
            for (Card card : handSnapshot) {
                System.out.println("      - " + card.getDisplayName());
            }
            player.saveRoundHistory(roundNumber, handSnapshot);
        }
        System.out.println("💾 Round " + roundNumber + " saved to all players");
    }

    /**
     * Vérifie si le round est terminé
     */
    public boolean isRoundOver() {
        long playingCount = players.stream()
            .filter(p -> p.getStatus() == PlayerStatus.PLAYING)
            .count();
        return playingCount == 0;
    }

    /**
     * Vérifie si la partie est terminée
     */
    public boolean isGameOver() {
        return gameState == GameState.GAME_OVER;
    }

    /**
     * Démarre le prochain round (appelé par le joueur actif)
     */
    public String startNextRound(String userId) {
        if (gameState != GameState.WAITING_NEXT_ROUND) {
            throw new IllegalStateException("Le jeu n'est pas en attente du prochain round");
        }

        GamePlayer currentPlayer = getCurrentPlayer();
        if (currentPlayer == null || !currentPlayer.getUserId().equals(userId)) {
            throw new IllegalStateException("Ce n'est pas à vous de démarrer le round");
        }

        // Appeler la méthode commune qui incrémente le round et distribue les cartes
        startNewRound();

        System.out.println("🎮 C'est au tour de " + currentPlayer.getUsername());
        return "Round " + roundNumber + " démarré !";
    }

    /**
     * Distribue les cartes initiales (1 carte par joueur)
     * Si une carte Stop est distribuée, elle reste en pending pour assignation
     */
    private void dealInitialCards() {
        System.out.println("🎴 Distributing initial cards to all players...");
        for (GamePlayer player : players) {
            if (player.getStatus() != PlayerStatus.ELIMINATED) {
                Card card = deck.draw();
                
                // Si c'est une carte Stop, la marquer comme pending
                if (card instanceof SpecialCard && ((SpecialCard) card).getSpecialType() == SpecialType.STOP) {
                    SpecialCard stopCard = (SpecialCard) card;
                    stopCard.setPending(true);
                    System.out.println("   🛑 " + player.getUsername() + " received a STOP card (pending assignment)");
                }
                
                player.addCard(card);
                player.setStatus(PlayerStatus.PLAYING);
                System.out.println("   - " + player.getUsername() + " received: " + card.getDisplayName() + " (hand size: " + player.getHand().size() + ", roundScore: " + player.getRoundScore() + ")");
            }
        }
    }

    /**
     * Retourne le joueur gagnant
     */
    public GamePlayer getWinner() {
        if (winnerId != null) {
            return getPlayerById(winnerId);
        }
        return null;
    }

    /**
     * Récupère un joueur par son ID
     */
    public GamePlayer getPlayerById(String playerId) {
        return players.stream()
            .filter(p -> p.getUserId().equals(playerId))
            .findFirst()
            .orElse(null);
    }

    // Getters
    public String getRoomId() {
        return roomId;
    }

    public List<GamePlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public GameState getGameState() {
        return gameState;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public int getRemainingCards() {
        return deck.getRemainingCards();
    }

    /**
     * Classe résultat pour les actions de pioche
     */
    public static class DrawResult {
        private boolean success;
        private String message;
        private Card drawnCard;
        private boolean roundEnded;
        private boolean lifeUsed;
        private boolean eliminated;
        private boolean needsStopAssignment; // Carte Stop piochée, nécessite un choix

        public DrawResult(boolean success, String message, Card drawnCard) {
            this(success, message, drawnCard, false, false, false, false);
        }

        public DrawResult(boolean success, String message, Card drawnCard, boolean roundEnded) {
            this(success, message, drawnCard, roundEnded, false, false, false);
        }

        public DrawResult(boolean success, String message, Card drawnCard, boolean roundEnded, boolean lifeUsed) {
            this(success, message, drawnCard, roundEnded, lifeUsed, false, false);
        }

        public DrawResult(boolean success, String message, Card drawnCard, boolean roundEnded, boolean lifeUsed, boolean eliminated) {
            this(success, message, drawnCard, roundEnded, lifeUsed, eliminated, false);
        }

        public DrawResult(boolean success, String message, Card drawnCard, boolean roundEnded, boolean lifeUsed, boolean eliminated, boolean needsStopAssignment) {
            this.success = success;
            this.message = message;
            this.drawnCard = drawnCard;
            this.roundEnded = roundEnded;
            this.lifeUsed = lifeUsed;
            this.eliminated = eliminated;
            this.needsStopAssignment = needsStopAssignment;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Card getDrawnCard() { return drawnCard; }
        public boolean isRoundEnded() { return roundEnded; }
        public boolean isLifeUsed() { return lifeUsed; }
        public boolean isEliminated() { return eliminated; }
        public boolean isNeedsStopAssignment() { return needsStopAssignment; }
    }

    /**
     * Classe résultat pour les actions générales
     */
    public static class ActionResult {
        private boolean success;
        private String message;

        public ActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
