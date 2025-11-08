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
    private int initialPlayerIndexForRound; // Sauvegarde du joueur de départ du round
    private GameState gameState;
    private int roundNumber;
    private String winnerId;
    private static final int WINNING_SCORE = 200;

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
        
        // Déterminer le joueur de départ
        if (roundNumber == 1) {
            // Premier round : joueur aléatoire
            currentPlayerIndex = new java.util.Random().nextInt(players.size());
            System.out.println("🎲 First round - Random starting player: " + getCurrentPlayer().getUsername() + " (index " + currentPlayerIndex + ")");
        } else {
            // Rounds suivants : currentPlayerIndex a déjà été calculé dans endRound()
            // On le garde tel quel
            System.out.println("🔄 Round " + roundNumber + " - Starting player (already set): " + getCurrentPlayer().getUsername() + " (index " + currentPlayerIndex + ")");
        }

        // Sauvegarder le joueur de départ
        initialPlayerIndexForRound = currentPlayerIndex;

        // Réinitialiser les joueurs
        for (GamePlayer player : players) {
            player.resetForNewRound();
        }

        // Réinitialiser et mélanger le deck
        deck.reset();

        // NOUVEAU: Pas de distribution automatique - commencer directement en mode PLAYING
        // Chaque joueur pioche sa première carte avec HIT
        gameState = GameState.PLAYING;
        System.out.println("✅ Round " + roundNumber + " started! Players can now HIT or STOP. First player: " + getCurrentPlayer().getUsername());
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

        // Vérification si c'est une carte Stop ou DrawThree (nécessite un choix)
        if (drawnCard instanceof SpecialCard) {
            SpecialCard specialCard = (SpecialCard) drawnCard;
            SpecialType type = specialCard.getSpecialType();
            
            if (type == SpecialType.STOP) {
                System.out.println("🛑 " + player.getUsername() + " a pioché une carte STOP (en attente d'assignation)");
                // La carte reste pending, le joueur doit choisir à qui l'assigner
                return new DrawResult(true, "Carte Stop piochée ! Choisissez un joueur.", drawnCard, false, false, false, true);
            } else if (type == SpecialType.DRAW_THREE) {
                System.out.println("➕3️⃣ " + player.getUsername() + " a pioché une carte DRAW_THREE (en attente d'assignation)");
                // La carte reste pending, le joueur doit choisir à qui l'assigner
                return new DrawResult(true, "Carte +3 piochée ! Choisissez un joueur.", drawnCard, false, false, false, true);
            }
        }

        // Vérification du double
        if (player.hasDouble()) {
            return handleDouble(player, drawnCard);
        }

        // Vérification du Flip7 (7 cartes numérotées différentes)
        if (player.hasFlip7()) {
            player.setStatus(PlayerStatus.FLIP7_STOP);
            System.out.println("🎯 FLIP7 ! " + player.getUsername() + " a 7 cartes numérotées différentes !");
            System.out.println("   Le round s'arrête pour tous. Bonus de +15 points pour " + player.getUsername());
            endRound();
            return new DrawResult(true, "🎯 FLIP7 ! 7 cartes numérotées différentes ! Le round s'arrête et vous gagnez +15 points !", drawnCard, true);
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
            nextPlayer(); // Passer au joueur suivant après utilisation de la carte Vie
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

        // Pendant la distribution (DISTRIBUTING), permettre l'assignation sans vérifier le tour
        if (gameState != GameState.DISTRIBUTING && !player.getUserId().equals(getCurrentPlayer().getUserId())) {
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

        System.out.println("🛑 AVANT assignation:");
        System.out.println("   - Assigneur: " + player.getUsername() + " (main: " + player.getHand().size() + " cartes)");
        System.out.println("   - Cible: " + targetPlayer.getUsername() + " (main: " + targetPlayer.getHand().size() + " cartes, status: " + targetPlayer.getStatus() + ")");
        System.out.println("   - Carte Stop ID: " + card.getId());

        // Assigner la carte au joueur cible
        stopCard.setPending(false);
        stopCard.setAssignedToPlayerId(targetPlayerId);
        
        // Retirer la carte de la main du joueur qui l'a piochée
        boolean removed = player.removeCard(card);
        System.out.println("   - Carte retirée de " + player.getUsername() + " ? " + removed);
        
        // Ajouter la carte Stop à la main du joueur cible et le forcer à s'arrêter
        targetPlayer.addCard(card);
        System.out.println("   - Carte ajoutée à " + targetPlayer.getUsername());
        
        System.out.println("🛑 APRÈS assignation:");
        System.out.println("   - Assigneur: " + player.getUsername() + " (main: " + player.getHand().size() + " cartes)");
        System.out.println("   - Cible: " + targetPlayer.getUsername() + " (main: " + targetPlayer.getHand().size() + " cartes)");
        
        // Initialiser le status si besoin (même s'il n'a pas encore de carte initiale)
        if (targetPlayer.getStatus() != PlayerStatus.PLAYING && targetPlayer.getStatus() != PlayerStatus.ELIMINATED) {
            targetPlayer.setStatus(PlayerStatus.PLAYING);
        }
        
        // Le forcer à s'arrêter immédiatement (même avec 0 carte si pas encore distribué)
        targetPlayer.setStatus(PlayerStatus.FORCED_STOP);
        
        System.out.println("🛑 " + player.getUsername() + " a assigné la carte STOP à " + targetPlayer.getUsername());
        
        // Si le joueur n'avait pas encore de carte, il reste avec 0 point + la Stop
        if (gameState == GameState.DISTRIBUTING && targetPlayer.getHand().size() == 1) {
            System.out.println("   ⚠️  " + targetPlayer.getUsername() + " n'avait pas encore de carte - il est stoppé avec 0 point (seulement la carte Stop)");
        } else {
            System.out.println("   Score de " + targetPlayer.getUsername() + " : " + targetPlayer.getRoundScore() + " (incluant la carte Stop)");
        }
        
        // Vérifier s'il y avait une pioche forcée en cours - la Stop l'annule
        if (targetPlayer.getRemainingForcedDraws() > 0) {
            System.out.println("   🛑 La carte Stop annule les " + targetPlayer.getRemainingForcedDraws() + " cartes restantes à piocher");
            targetPlayer.setRemainingForcedDraws(0);
        }
        
        // Si on est en phase de distribution, GameService reprendra la distribution
        if (gameState == GameState.DISTRIBUTING) {
            // Ne rien faire - GameService appellera continueInitialDistribution() après broadcast
        } else {
            // Pendant le jeu normal, appeler nextPlayer()
            if (targetPlayer.getUserId().equals(getCurrentPlayer().getUserId())) {
                nextPlayer();
            } else {
                // Sinon, passer simplement au joueur suivant
                nextPlayer();
            }
        }
        
        return new ActionResult(true, targetPlayer.getUsername() + " a reçu la carte Stop et est forcé de s'arrêter !");
    }

    // Assigner une carte DrawThree à un joueur cible (force à piocher 3 cartes)
    public ActionResult assignDrawThreeCard(String playerId, String cardId, String targetPlayerId) {
        GamePlayer player = getPlayerById(playerId);
        if (player == null) {
            return new ActionResult(false, "Joueur non trouvé");
        }
        
        // Pendant la distribution (DISTRIBUTING), permettre l'assignation sans vérifier le tour
        if (gameState != GameState.DISTRIBUTING && !player.getUserId().equals(getCurrentPlayer().getUserId())) {
            return new ActionResult(false, "Ce n'est pas votre tour");
        }

        Card card = player.getHand().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElse(null);

        if (card == null) {
            return new ActionResult(false, "Carte non trouvée dans la main du joueur");
        }

        if (!(card instanceof SpecialCard)) {
            return new ActionResult(false, "Cette carte n'est pas une carte spéciale");
        }

        SpecialCard drawThreeCard = (SpecialCard) card;
        if (drawThreeCard.getSpecialType() != SpecialType.DRAW_THREE) {
            return new ActionResult(false, "Cette carte n'est pas une carte DrawThree");
        }

        if (!drawThreeCard.isPending()) {
            return new ActionResult(false, "Cette carte DrawThree a déjà été assignée");
        }

        GamePlayer targetPlayer = getPlayerById(targetPlayerId);
        if (targetPlayer == null) {
            return new ActionResult(false, "Joueur cible non trouvé");
        }

        // Assigner la carte au joueur cible
        drawThreeCard.setPending(false);
        drawThreeCard.setAssignedToPlayerId(targetPlayerId);
        
        // Retirer la carte de la main du joueur qui l'a piochée
        player.removeCard(card);
        
        // Ajouter la carte à la main du joueur cible
        targetPlayer.addCard(card);
        
        // Pendant la distribution, initialiser le status si besoin
        if (gameState == GameState.DISTRIBUTING && targetPlayer.getStatus() != PlayerStatus.PLAYING) {
            targetPlayer.setStatus(PlayerStatus.PLAYING);
        }
        
        System.out.println("➕3️⃣ " + player.getUsername() + " a assigné la carte DRAW_THREE à " + targetPlayer.getUsername());
        
        // Vérifier s'il y a une pioche forcée en cours à reprendre
        int remainingDraws = targetPlayer.getRemainingForcedDraws();
        if (remainingDraws > 0) {
            System.out.println("   ▶️  Reprise d'une pioche forcée avec " + remainingDraws + " cartes restantes");
            targetPlayer.setRemainingForcedDraws(0); // Reset
            forceDrawThreeCards(targetPlayer, remainingDraws);
        } else {
            // Nouvelle pioche forcée de 3 cartes
            forceDrawThreeCards(targetPlayer, 3);
        }
        
        // Passer au joueur suivant (seulement si pas de carte spéciale en attente)
        boolean hasPendingSpecial = targetPlayer.getHand().stream()
            .anyMatch(c -> c instanceof SpecialCard && 
                     ((SpecialCard) c).isPending() &&
                     (((SpecialCard) c).getSpecialType() == SpecialType.STOP || 
                      ((SpecialCard) c).getSpecialType() == SpecialType.DRAW_THREE));
        
        // Si on est en phase de distribution, GameService reprendra la distribution
        if (gameState == GameState.DISTRIBUTING && !hasPendingSpecial) {
            // Ne rien faire - GameService appellera continueInitialDistribution() après broadcast
        } else if (!hasPendingSpecial) {
            // Pendant le jeu normal, passer au joueur suivant dans l'ordre
            // Exemple: A donne +3 à C, alors: A → pioche → C pioche 3 → tour de B → tour de C
            nextPlayer();
        }
        
        return new ActionResult(true, targetPlayer.getUsername() + " a reçu la carte +3 et doit piocher 3 cartes !");
    }

    // Force un joueur à piocher N cartes (avec arrêt si élimination ou carte spéciale)
    private void forceDrawThreeCards(GamePlayer targetPlayer, int cardsToDraw) {
        System.out.println("➕3️⃣ " + targetPlayer.getUsername() + " doit piocher " + cardsToDraw + " carte(s)");
        
        for (int i = 0; i < cardsToDraw; i++) {
            // Vérifier si le joueur est déjà éliminé avant de continuer
            if (targetPlayer.getStatus() == PlayerStatus.ELIMINATED) {
                System.out.println("   ⚠️ Joueur déjà éliminé - arrêt de la pioche forcée");
                targetPlayer.setRemainingForcedDraws(0);
                break;
            }
            
            // Piocher une carte
            Card card = deck.draw();
            targetPlayer.addCard(card);
            
            int remaining = cardsToDraw - i - 1;
            System.out.println("   Carte " + (i+1) + "/" + cardsToDraw + ": " + card.getDisplayName() + " (restantes: " + remaining + ")");
            System.out.println("   Score actuel: " + targetPlayer.getRoundScore());
            
            // Si c'est une carte spéciale (Stop ou DrawThree), elle doit être résolue immédiatement
            if (card instanceof SpecialCard) {
                SpecialCard specialCard = (SpecialCard) card;
                if (specialCard.getSpecialType() == SpecialType.STOP || 
                    specialCard.getSpecialType() == SpecialType.DRAW_THREE) {
                    System.out.println("   🎯 Carte spéciale piochée pendant le +3 : " + specialCard.getSpecialType());
                    System.out.println("   ⏸️  La pioche forcée est suspendue - " + remaining + " carte(s) restante(s)");
                    specialCard.setPending(true);
                    // Sauvegarder le nombre de cartes restant à piocher
                    targetPlayer.setRemainingForcedDraws(remaining);
                    // Arrêter la pioche forcée, elle reprendra après l'assignation
                    return;
                }
            }
            
            // Vérifier si le joueur a un double après cette carte
            if (targetPlayer.hasDouble()) {
                System.out.println("   ⚠️ DOUBLE détecté avec " + card.getDisplayName() + " !");
                handleDouble(targetPlayer, card);
                
                // Si le joueur est éliminé après le double, arrêter la pioche forcée
                if (targetPlayer.getStatus() == PlayerStatus.ELIMINATED) {
                    System.out.println("   💀 Éliminé à la carte " + (i+1) + "/" + cardsToDraw + " - arrêt de la pioche forcée");
                    targetPlayer.setRemainingForcedDraws(0);
                    break;
                }
            }
        }
        
        // Pioche forcée terminée normalement
        targetPlayer.setRemainingForcedDraws(0);
        System.out.println("   ✅ Pioche forcée terminée. Score final: " + targetPlayer.getRoundScore());
    }

    public ActionResult applyLifeCard(String playerId, String cardId) {
        // TODO: Implémenter l'utilisation manuelle de la carte Vie
        // Pour l'instant, la carte Vie est automatiquement utilisée lors d'un double
        return new ActionResult(false, "La carte Vie est automatiquement utilisée lors d'un double");
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

        // Identifier le joueur qui a fait un Flip7 (s'il y en a un)
        GamePlayer flip7Player = players.stream()
            .filter(p -> p.getStatus() == PlayerStatus.FLIP7_STOP)
            .findFirst()
            .orElse(null);

        // Calcul des scores pour tous les joueurs non éliminés
        for (GamePlayer player : players) {
            if (player.getStatus() != PlayerStatus.ELIMINATED) {
                player.calculateRoundScore();
                
                // Bonus de +15 points pour le joueur qui a fait un Flip7
                if (player.equals(flip7Player)) {
                    int bonusScore = 15;
                    player.setRoundScore(player.getRoundScore() + bonusScore);
                    System.out.println("   🎯 Bonus Flip7 : +" + bonusScore + " points → Score total du round = " + player.getRoundScore());
                }
                
                player.addRoundScoreToTotal();
            }
        }

        // Sauvegarder l'état du round dans chaque joueur
        saveRoundToPlayers();

        // Vérifier si la partie est terminée (au moins un joueur >= 200 points)
        checkGameOver();

        // Si le jeu continue, passer en attente du prochain round
        if (gameState != GameState.GAME_OVER) {
            // Calculer qui va commencer le prochain round AVANT de passer à WAITING_NEXT_ROUND
            // pour que le frontend affiche la bonne personne
            int nextRoundStarterIndex;
            if (roundNumber == 0) {
                // Cas impossible normalement, mais on gère par sécurité
                nextRoundStarterIndex = new java.util.Random().nextInt(players.size());
            } else {
                // Faire tourner à partir du joueur qui a commencé ce round
                nextRoundStarterIndex = (initialPlayerIndexForRound + 1) % players.size();
            }
            
            currentPlayerIndex = nextRoundStarterIndex;
            gameState = GameState.WAITING_NEXT_ROUND;
            System.out.println("⏳ En attente que " + getCurrentPlayer().getUsername() + " démarre le round " + (roundNumber + 1) + " (index " + currentPlayerIndex + ")");
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
     * Vérifie si la partie est terminée et détermine le gagnant
     * La partie est terminée si au moins un joueur a >= 200 points
     * Si plusieurs joueurs ont >= 200, le gagnant est celui avec le plus haut score
     * En cas d'égalité, le gagnant est choisi aléatoirement parmi les ex-aequo
     */
    private void checkGameOver() {
        // Trouver tous les joueurs qui ont atteint ou dépassé 200 points
        List<GamePlayer> qualifiedPlayers = players.stream()
            .filter(p -> p.getTotalScore() >= WINNING_SCORE)
            .collect(java.util.stream.Collectors.toList());

        if (qualifiedPlayers.isEmpty()) {
            // Personne n'a atteint 200 points, le jeu continue
            return;
        }

        System.out.println("🏁 Au moins un joueur a atteint " + WINNING_SCORE + " points !");

        // Trouver le score maximum parmi les joueurs qualifiés
        int maxScore = qualifiedPlayers.stream()
            .mapToInt(GamePlayer::getTotalScore)
            .max()
            .orElse(0);

        // Trouver tous les joueurs avec le score maximum
        List<GamePlayer> winners = qualifiedPlayers.stream()
            .filter(p -> p.getTotalScore() == maxScore)
            .collect(java.util.stream.Collectors.toList());

        if (winners.size() == 1) {
            // Un seul gagnant
            GamePlayer winner = winners.get(0);
            winnerId = winner.getUserId();
            System.out.println("🏆 Gagnant : " + winner.getUsername() + " avec " + winner.getTotalScore() + " points !");
        } else {
            // Plusieurs joueurs à égalité - choix aléatoire
            System.out.println("⚖️ Égalité à " + maxScore + " points entre " + winners.size() + " joueurs :");
            for (GamePlayer player : winners) {
                System.out.println("   - " + player.getUsername() + " : " + player.getTotalScore() + " points");
            }
            
            // Choix aléatoire du gagnant
            java.util.Random random = new java.util.Random();
            GamePlayer winner = winners.get(random.nextInt(winners.size()));
            winnerId = winner.getUserId();
            System.out.println("🎲 Gagnant choisi aléatoirement : " + winner.getUsername() + " !");
        }

        gameState = GameState.GAME_OVER;
        System.out.println("🎮 GAME OVER - Partie terminée !");
    }



    /**
     * Continue la distribution initiale des cartes (1 carte par joueur)
     * Distribue UNE carte au prochain joueur qui n'en a pas encore.
     * Si c'est une carte spéciale (Stop/DrawThree), s'arrête et attend l'assignation.
     * Sinon, continue automatiquement jusqu'à ce que tous aient une carte.
     * 
     * PUBLIC pour permettre à GameService de broadcaster après chaque carte distribuée.
     * Retourne true si la distribution continue, false si elle est terminée ou en pause.
     */
    public boolean continueInitialDistribution() {
        System.out.println("🎴 Continuing initial distribution...");
        
        // Trouver le prochain joueur sans carte ET qui n'est pas déjà stoppé/éliminé
        for (int i = 0; i < players.size(); i++) {
            GamePlayer player = players.get(i);
            
            // Skip les joueurs éliminés, stoppés, ou qui ont déjà une carte
            if (player.getStatus() == PlayerStatus.ELIMINATED) {
                continue;
            }
            if (player.getStatus() == PlayerStatus.FORCED_STOP || player.getStatus() == PlayerStatus.STOPPED) {
                System.out.println("   ⏭️  Skipping " + player.getUsername() + " - already stopped (no initial card)");
                continue;
            }
            if (!player.getHand().isEmpty()) {
                continue;
            }
            
            // Ce joueur a besoin d'une carte
            Card card = deck.draw();
            player.addCard(card);
            player.setStatus(PlayerStatus.PLAYING);
                
                System.out.println("   - " + player.getUsername() + " received: " + card.getDisplayName() + " (hand size: " + player.getHand().size() + ", roundScore: " + player.getRoundScore() + ")");
                
                // Si c'est une carte Stop ou DrawThree, marquer comme pending et ARRÊTER
                if (card instanceof SpecialCard) {
                    SpecialCard specialCard = (SpecialCard) card;
                    SpecialType type = specialCard.getSpecialType();
                    
                    if (type == SpecialType.STOP) {
                        specialCard.setPending(true);
                        // IMPORTANT : Mettre à jour currentPlayerIndex pour que le frontend sache qui doit assigner
                        currentPlayerIndex = i;
                        System.out.println("   🛑 STOP card - distribution paused, waiting for " + player.getUsername() + " to assign (index " + i + ")");
                        return false; // ARRÊT ICI - attente de l'assignation
                    } else if (type == SpecialType.DRAW_THREE) {
                        specialCard.setPending(true);
                        // IMPORTANT : Mettre à jour currentPlayerIndex pour que le frontend sache qui doit assigner
                        currentPlayerIndex = i;
                        System.out.println("   ➕3️⃣ DRAW_THREE card - distribution paused, waiting for " + player.getUsername() + " to assign (index " + i + ")");
                        return false; // ARRÊT ICI - attente de l'assignation
                    }
                }
                
                // Pas de carte spéciale, continuer (GameService rappellera cette méthode)
                return true; // Indique qu'il faut continuer la distribution
        }
        
        // Tous les joueurs ont reçu leur carte initiale
        gameState = GameState.PLAYING;
        
        // Restaurer le joueur de départ initial du round
        // (car currentPlayerIndex a pu changer pendant la distribution pour les assignations)
        currentPlayerIndex = initialPlayerIndexForRound;
        
        System.out.println("✅ Initial distribution complete! Round " + roundNumber + " started! Current player: " + getCurrentPlayer().getUsername());
        return false; // Distribution terminée
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

    public String getWinnerId() {
        return winnerId;
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
