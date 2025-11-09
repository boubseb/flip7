package com.flip7.flip7.game.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flip7.flip7.game.card.*;

import java.util.*;

/**
 * Classe principale gérant la logique du jeu Flip7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private Queue<PendingSpecialCard> pendingSpecialCardsQueue = new LinkedList<>();
    private String firstSourcePlayerId; // Joueur qui a pioché la 1ère carte spéciale de la chaîne

    // Constructeur par défaut pour Jackson
    public Game() {
        this.deck = new Deck();
        this.players = new ArrayList<>();
        this.pendingSpecialCardsQueue = new LinkedList<>();
    }

    public Game(
        String roomId, 
        List<String> playerIds, 
        Map<String, String> playerNames
    ) {
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
        
        System.out.println("🚀 ========== STARTING NEW ROUND " + roundNumber + " ==========");
        
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
        System.out.println("🧹 Resetting all players for new round...");
        for (GamePlayer player : players) {
            System.out.println("   - Resetting " + player.getUsername() + " (current hand: " + player.getHand().size() + " cards)");
            player.resetForNewRound();
            System.out.println("     ✅ " + player.getUsername() + " reset complete (hand: " + player.getHand().size() + " cards, status: " + player.getStatus() + ")");
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

        System.out.println("🃏 " + player.getUsername() + " pioche: " + drawnCard.toString());

        // Vérification si c'est une carte spéciale (STOP ou DRAW_THREE)
        if (drawnCard instanceof SpecialCard) {
            SpecialCard specialCard = (SpecialCard) drawnCard;
            SpecialType type = specialCard.getSpecialType();
            
            if (type == SpecialType.STOP || type == SpecialType.DRAW_THREE) {
                System.out.println("   ⚠️ Carte spéciale détectée: " + type);
                
                // Créer PendingSpecialCard avec remaining=0
                PendingSpecialCard pendingCard = new PendingSpecialCard(
                    specialCard,
                    player.getUserId(),
                    null, // Pas encore assignée
                    0     // Pas de pioche forcée en cours
                );
                pendingSpecialCardsQueue.add(pendingCard);
                
                System.out.println("   📝 Ajout à la queue: " + type + " (source: " + player.getUsername() + ", remaining: 0)");
                System.out.println("   ⏸️ ARRÊT de la pioche - Le joueur doit assigner cette carte");
                
                // ARRÊTER la pioche - le joueur doit assigner la carte
                String message = type == SpecialType.STOP ? 
                    "Carte STOP piochée ! Choisissez un joueur." : 
                    "Carte +3 piochée ! Choisissez un joueur.";
                
                return new DrawResult(true, message, drawnCard, false, false, false, true);
            }
        }

        // Vérification du double
        if (player.hasDouble()) {
            return handleDouble(player, drawnCard, true); // true = passer au joueur suivant
        }

        // Vérification du Flip7 (7 cartes numérotées différentes)
        if (player.hasFlip7()) {
            player.setStatus(PlayerStatus.FLIP7_STOP);
            System.out.println("🎯 FLIP7 ! " + player.getUsername() + " a 7 cartes numérotées différentes !");
            System.out.println("   Le round s'arrête pour tous. Bonus de +15 points pour " + player.getUsername());
            System.out.println("   📞 Calling endRound()...");
            endRound();
            System.out.println("   ✅ endRound() completed. GameState is now: " + gameState);
            return new DrawResult(true, "🎯 FLIP7 ! 7 cartes numérotées différentes ! Le round s'arrête et vous gagnez +15 points !", drawnCard, true);
        }

        // Passer au joueur suivant après une pioche réussie
        nextPlayer();
        return new DrawResult(true, "Carte piochée", drawnCard);
    }

    /**
     * Gère le cas où un joueur pioche un double
     * @param skipNextPlayer true si on doit passer au joueur suivant, false si c'est une pioche forcée en cours
     */
    private DrawResult handleDouble(GamePlayer player, Card drawnCard, boolean skipNextPlayer) {
        // Vérifier si le joueur a une carte Vie
        if (player.getLifeCardsInHand() > 0) {
            // Le joueur peut utiliser sa carte Vie
            player.useLifeCard();
            if (skipNextPlayer) {
                nextPlayer(); // Passer au joueur suivant après utilisation de la carte Vie
            }
            return new DrawResult(true, "Double ! Carte Vie utilisée pour survivre.", drawnCard, false, true);
        } else {
            // Le joueur est éliminé - son score de round passe à 0
            player.setStatus(PlayerStatus.ELIMINATED);
            player.resetRoundScore();
            System.out.println("💀 " + player.getUsername() + " éliminé ! Score du round remis à 0.");
            if (skipNextPlayer) {
                nextPlayer();
            }
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
        System.out.println("🛑 assignStopCard appelé:");
        System.out.println("   - playerId: " + playerId);
        System.out.println("   - cardId: " + cardId);
        System.out.println("   - targetPlayerId: " + targetPlayerId);
        
        GamePlayer player = getPlayerById(playerId);
        if (player == null) {
            System.out.println("   ❌ Joueur non trouvé");
            return new ActionResult(false, "Joueur non trouvé");
        }

        GamePlayer currentPlayer = getCurrentPlayer();
        System.out.println("   - Joueur actuel: " + currentPlayer.getUsername());

        // Vérifier que c'est le tour du joueur
        if (!player.getUserId().equals(currentPlayer.getUserId())) {
            System.out.println("   ❌ Ce n'est pas le tour de ce joueur");
            return new ActionResult(false, "Ce n'est pas votre tour");
        }

        // Vérifier que la carte existe dans la queue
        PendingSpecialCard pendingCard = pendingSpecialCardsQueue.stream()
            .filter(p -> p.getCard().getId().equals(cardId))
            .findFirst()
            .orElse(null);
        
        if (pendingCard == null) {
            System.out.println("   ❌ Carte non trouvée dans la queue");
            return new ActionResult(false, "Cette carte n'est pas en attente");
        }
        
        System.out.println("   ✅ Carte trouvée dans queue (remaining: " + pendingCard.getRemainingForcedDraws() + ")");

        GamePlayer targetPlayer = getPlayerById(targetPlayerId);
        if (targetPlayer == null) {
            return new ActionResult(false, "Joueur cible non trouvé");
        }

        System.out.println("   ➡️ Assignation STOP: " + player.getUsername() + " → " + targetPlayer.getUsername());
        
        // ÉTAPE 1: Retirer la carte de la queue
        int remaining = pendingCard.getRemainingForcedDraws();
        pendingSpecialCardsQueue.remove(pendingCard);
        System.out.println("   📝 Carte retirée de la queue");
        
        // ÉTAPE 2: Déplacer la carte de la main de source vers target
        Card card = player.getHand().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElse(null);
        
        if (card != null) {
            player.removeCard(card);
            targetPlayer.addCard(card);
            System.out.println("   ✅ Carte déplacée dans la main de " + targetPlayer.getUsername());
        }
        
        // ÉTAPE 3: Appliquer le FORCED_STOP
        targetPlayer.setStatus(PlayerStatus.FORCED_STOP);
        
        // ÉTAPE 3b: Enregistrer qui a donné la carte Stop
        if (!player.getUserId().equals(targetPlayer.getUserId())) {
            // Carte donnée par un autre joueur
            targetPlayer.setStoppedByUserId(player.getUserId());
            targetPlayer.setStoppedByUsername(player.getUsername());
        } else {
            // Auto-assignation : pas de "donneur"
            targetPlayer.setStoppedByUserId(null);
            targetPlayer.setStoppedByUsername(null);
        }
        System.out.println("   🛑 " + targetPlayer.getUsername() + " est maintenant FORCED_STOP");
        
        // ÉTAPE 4: Vérifier s'il y a une pioche suspendue à reprendre
        // MAIS si le joueur s'est assigné le Stop à lui-même, la pioche est ANNULÉE
        if (remaining > 0) {
            if (player.getUserId().equals(targetPlayer.getUserId())) {
                // Auto-assignation du Stop : la pioche suspendue est annulée
                System.out.println("   ⚠️ Auto-assignation du Stop : pioche suspendue annulée (remaining=" + remaining + ")");
                player.setRemainingForcedDraws(0); // Reset
            } else {
                // Joueur différent : on reprend la pioche suspendue normalement
                System.out.println("   ♻️ Il reste " + remaining + " carte(s) à piocher pour " + player.getUsername());
                boolean completed = processForcedDraws(player, remaining);
                
                if (!completed) {
                    System.out.println("   ⏸️ Pioche reprise interrompue - Attente assignation");
                    return new ActionResult(true, "Carte spéciale piochée - assignation nécessaire");
                }
            }
        } else {
            // Pas de pioche suspendue, vérifier s'il y en a d'autres dans la queue
            if (resumeSuspendedDraws()) {
                if (!pendingSpecialCardsQueue.isEmpty()) {
                    System.out.println("   ⏸️ Pioche reprise interrompue - Attente assignation");
                    return new ActionResult(true, "Carte spéciale piochée - assignation nécessaire");
                }
            }
        }
        
        // ÉTAPE 5: Tout est terminé - passer au joueur suivant
        if (firstSourcePlayerId != null) {
            GamePlayer firstSource = getPlayerById(firstSourcePlayerId);
            int firstSourceIndex = players.indexOf(firstSource);
            
            // Positionner le tour sur firstSource, puis appeler nextPlayer() qui gère les éliminés
            currentPlayerIndex = firstSourceIndex;
            System.out.println("   🔄 Positionnement sur " + firstSource.getUsername() + " puis passage au suivant");
            nextPlayer(); // Gère automatiquement les joueurs éliminés
            
            GamePlayer nextPlayer = getCurrentPlayer();
            System.out.println("   ✅ Retour au flux normal - Tour passe à: " + nextPlayer.getUsername() + " (après " + firstSource.getUsername() + ")");
            
            firstSourcePlayerId = null; // Reset
        } else {
            System.out.println("   ⚠️ firstSourcePlayerId est null - passage au joueur suivant normal");
            nextPlayer();
        }
        
        return new ActionResult(true, targetPlayer.getUsername() + " a reçu la carte Stop et est forcé de s'arrêter !");
    }

    // Assigner une carte DrawThree à un joueur cible (force à piocher 3 cartes)
    // Assigner une carte DrawThree à un joueur cible (force à piocher 3 cartes)
    public ActionResult assignDrawThreeCard(String playerId, String cardId, String targetPlayerId) {
        System.out.println("➕3️⃣ assignDrawThreeCard appelé:");
        System.out.println("   - playerId: " + playerId);
        System.out.println("   - cardId: " + cardId);
        System.out.println("   - targetPlayerId: " + targetPlayerId);
        
        GamePlayer player = getPlayerById(playerId);
        if (player == null) {
            return new ActionResult(false, "Joueur non trouvé");
        }
        
        GamePlayer currentPlayer = getCurrentPlayer();
        System.out.println("   - Joueur actuel: " + currentPlayer.getUsername());
        
        // Vérifier que c'est le tour du joueur
        if (!player.getUserId().equals(currentPlayer.getUserId())) {
            System.out.println("   ❌ Ce n'est pas le tour de ce joueur");
            return new ActionResult(false, "Ce n'est pas votre tour");
        }

        // Vérifier que la carte existe dans la queue
        PendingSpecialCard pendingCard = pendingSpecialCardsQueue.stream()
            .filter(p -> p.getCard().getId().equals(cardId))
            .findFirst()
            .orElse(null);
        
        if (pendingCard == null) {
            System.out.println("   ❌ Carte non trouvée dans la queue");
            return new ActionResult(false, "Cette carte n'est pas en attente");
        }
        
        System.out.println("   ✅ Carte trouvée dans queue (remaining: " + pendingCard.getRemainingForcedDraws() + ")");

        GamePlayer targetPlayer = getPlayerById(targetPlayerId);
        if (targetPlayer == null) {
            return new ActionResult(false, "Joueur cible non trouvé");
        }

        // Vérifier que le joueur cible n'est pas stoppé
        if (targetPlayer.getStatus() == PlayerStatus.STOPPED || 
            targetPlayer.getStatus() == PlayerStatus.FORCED_STOP ||
            targetPlayer.getStatus() == PlayerStatus.FLIP7_STOP) {
            System.out.println("   ❌ Joueur cible est stoppé");
            return new ActionResult(false, "Impossible d'assigner un +3 à un joueur stoppé");
        }

        System.out.println("   ➡️ Assignation: " + player.getUsername() + " → " + targetPlayer.getUsername());
        
        // ÉTAPE 1: Sauvegarder firstSourcePlayerId si c'est la première carte de la chaîne
        if (firstSourcePlayerId == null) {
            firstSourcePlayerId = pendingCard.getSourcePlayerId();
            System.out.println("   💾 Sauvegarde firstSourcePlayerId: " + getPlayerById(firstSourcePlayerId).getUsername());
        }
        
        // ÉTAPE 2: Retirer la carte de la queue (elle est maintenant assignée)
        int remaining = pendingCard.getRemainingForcedDraws();
        pendingSpecialCardsQueue.remove(pendingCard);
        System.out.println("   📝 Carte retirée de la queue (remaining=" + remaining + ")");
        
        // ÉTAPE 3: Déplacer la carte de la main de source vers target
        Card card = player.getHand().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElse(null);
        
        if (card != null) {
            player.removeCard(card);
            targetPlayer.addCard(card);
            System.out.println("   ✅ Carte déplacée dans la main de " + targetPlayer.getUsername());
        }
        
        // ÉTAPE 4: Changer le tour vers le joueur cible
        currentPlayerIndex = players.indexOf(targetPlayer);
        System.out.println("   🔄 Changement tour: " + targetPlayer.getUsername() + " (index " + currentPlayerIndex + ")");
        
        // ÉTAPE 4b: Enregistrer qui a donné le +3 (pour contexte en cas d'élimination)
        targetPlayer.setDrawThreeByUserId(player.getUserId());
        targetPlayer.setDrawThreeByUsername(player.getUsername());
        
        // ÉTAPE 5: Faire piocher 3 cartes au joueur cible
        boolean completed = processForcedDraws(targetPlayer, 3);
        
        // ÉTAPE 6: Vérifier si le round s'est terminé
        if (gameState != GameState.PLAYING) {
            System.out.println("   ⚠️ Round terminé pendant la pioche forcée");
            firstSourcePlayerId = null; // Reset
            return new ActionResult(true, "Le round s'est terminé");
        }
        
        // ÉTAPE 7: Si la pioche a été interrompue, attendre l'assignation
        if (!completed) {
            System.out.println("   ⏸️ Pioche interrompue par carte spéciale - Attente assignation");
            return new ActionResult(true, targetPlayer.getUsername() + " doit assigner la carte spéciale piochée");
        }
        
        // ÉTAPE 8: Pioche complétée - vérifier s'il y a une pioche suspendue à reprendre
        System.out.println("   ✅ Pioche de 3 cartes terminée");
        
        if (resumeSuspendedDraws()) {
            // Une pioche suspendue a été reprise
            // Attendre qu'elle se termine (peut être interrompue à nouveau)
            if (!pendingSpecialCardsQueue.isEmpty()) {
                System.out.println("   ⏸️ Pioche reprise interrompue - Attente assignation");
                return new ActionResult(true, "Carte spéciale piochée - assignation nécessaire");
            }
        }
        
        // ÉTAPE 9: Tout est terminé - passer au joueur suivant firstSourcePlayerId
        if (firstSourcePlayerId != null) {
            GamePlayer firstSource = getPlayerById(firstSourcePlayerId);
            int firstSourceIndex = players.indexOf(firstSource);
            
            // Positionner le tour sur firstSource, puis appeler nextPlayer() qui gère les éliminés
            currentPlayerIndex = firstSourceIndex;
            System.out.println("   🔄 Positionnement sur " + firstSource.getUsername() + " puis passage au suivant");
            nextPlayer(); // Gère automatiquement les joueurs éliminés
            
            GamePlayer nextPlayer = getCurrentPlayer();
            System.out.println("   ✅ Retour au flux normal - Tour passe à: " + nextPlayer.getUsername() + " (après " + firstSource.getUsername() + ")");
            
            firstSourcePlayerId = null; // Reset pour la prochaine chaîne
        } else {
            System.out.println("   ⚠️ firstSourcePlayerId est null - passage au joueur suivant normal");
            nextPlayer();
        }
        
        return new ActionResult(true, targetPlayer.getUsername() + " a pioché 3 cartes");
    }

    // Force un joueur à piocher N cartes (avec arrêt si élimination ou carte spéciale)
    private void forceDrawThreeCards(GamePlayer targetPlayer, int cardsToDraw) {
        System.out.println("➕3️⃣ " + targetPlayer.getUsername() + " doit piocher " + cardsToDraw + " carte(s)");
        
        // Trouver l'index du joueur cible
        int targetPlayerIndex = players.indexOf(targetPlayer);
        
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
                    System.out.println("   🔄 Changement temporaire du tour : " + getCurrentPlayer().getUsername() + " → " + targetPlayer.getUsername());
                    
                    // Ajouter à la queue au lieu de juste marquer pending
                    specialCard.setPending(true);
                    PendingSpecialCard pendingCard = new PendingSpecialCard(
                        specialCard,
                        targetPlayer.getUserId(), // Le joueur qui a pioché la carte
                        null, // Pas encore de cible
                        remaining // Cartes restantes à piocher après résolution
                    );
                    pendingSpecialCardsQueue.add(pendingCard);
                    System.out.println("   📝 Ajout à la queue: " + specialCard.getSpecialType() + " (remaining: " + remaining + ")");
                    
                    // Sauvegarder le nombre de cartes restant à piocher
                    targetPlayer.setRemainingForcedDraws(remaining);
                    // Changer temporairement le tour vers le joueur qui doit assigner la carte
                    currentPlayerIndex = targetPlayerIndex;
                    System.out.println("   ✅ Tour maintenant sur: " + getCurrentPlayer().getUsername() + " (index " + currentPlayerIndex + ")");
                    // Arrêter la pioche forcée, elle reprendra après l'assignation
                    return;
                }
            }
            
            // Vérifier si le joueur a un double après cette carte
            if (targetPlayer.hasDouble()) {
                System.out.println("   ⚠️ DOUBLE détecté avec " + card.getDisplayName() + " !");
                DrawResult doubleResult = handleDouble(targetPlayer, card, false); // false = NE PAS passer au joueur suivant (pioche forcée en cours)
                
                // Si le joueur est éliminé après le double, arrêter la pioche forcée
                if (targetPlayer.getStatus() == PlayerStatus.ELIMINATED) {
                    System.out.println("   💀 Éliminé à la carte " + (i+1) + "/" + cardsToDraw + " - arrêt de la pioche forcée");
                    targetPlayer.setRemainingForcedDraws(0);
                    break;
                } else if (doubleResult.isLifeUsed()) {
                    System.out.println("   ❤️ Carte Vie utilisée ! Le joueur continue la pioche forcée");
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

    public Queue<PendingSpecialCard> getPendingSpecialCards() {
        return pendingSpecialCardsQueue;
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
    
    /**
     * Traite N pioches forcées pour un joueur.
     * Gère l'interruption par carte spéciale (suspension avec remaining).
     * @param player Le joueur qui doit piocher
     * @param numCards Nombre de cartes à piocher
     * @return true si toutes les cartes ont été piochées, false si interrompu par carte spéciale
     */
    private boolean processForcedDraws(GamePlayer player, int numCards) {
        System.out.println("🔄 processForcedDraws: " + player.getUsername() + " doit piocher " + numCards + " carte(s)");
        
        for (int i = 0; i < numCards; i++) {
            // Vérifier si le joueur est éliminé
            if (player.getStatus() == PlayerStatus.ELIMINATED) {
                System.out.println("   ⚠️ Joueur éliminé - arrêt pioche forcée");
                return true; // Pioche terminée (même si interrompue)
            }
            
            // Piocher une carte
            Card card = deck.draw();
            player.addCard(card);
            
            int remaining = numCards - i - 1;
            System.out.println("   📥 Carte " + (i+1) + "/" + numCards + ": " + card.toString() + " (restantes: " + remaining + ")");
            
            // Vérifier si c'est une carte spéciale
            if (card instanceof SpecialCard) {
                SpecialCard specialCard = (SpecialCard) card;
                SpecialType type = specialCard.getSpecialType();
                
                if (type == SpecialType.STOP || type == SpecialType.DRAW_THREE) {
                    System.out.println("   ⚠️ CARTE SPÉCIALE détectée: " + type);
                    System.out.println("   ⏸️ SUSPENSION: " + remaining + " carte(s) restante(s) à piocher");
                    
                    // Créer PendingSpecialCard avec remaining
                    PendingSpecialCard pendingCard = new PendingSpecialCard(
                        specialCard,
                        player.getUserId(),
                        null,
                        remaining
                    );
                    pendingSpecialCardsQueue.add(pendingCard);
                    
                    System.out.println("   📝 Ajout queue: " + type + " (source: " + player.getUsername() + ", remaining: " + remaining + ")");
                    System.out.println("   ⏸️ ARRÊT pioche - Le joueur doit assigner cette carte");
                    
                    return false; // Pioche interrompue
                }
            }
            
            // Vérifier le double
            if (player.hasDouble()) {
                System.out.println("   ⚠️ DOUBLE détecté !");
                handleDouble(player, card, false); // false = ne pas passer au joueur suivant
                
                if (player.getStatus() == PlayerStatus.ELIMINATED) {
                    System.out.println("   💀 Éliminé - arrêt pioche forcée");
                    return true; // Pioche terminée
                }
            }
        }
        
        System.out.println("   ✅ Pioche forcée terminée - " + numCards + " cartes piochées");
        return true; // Toutes les cartes piochées
    }
    
    /**
     * Reprend une pioche suspendue (remaining > 0) en cherchant dans la queue.
     * @return true si une pioche a été reprise, false sinon
     */
    private boolean resumeSuspendedDraws() {
        System.out.println("🔍 resumeSuspendedDraws: Recherche pioche suspendue...");
        
        // Chercher une carte avec remaining > 0
        PendingSpecialCard suspendedCard = null;
        for (PendingSpecialCard card : pendingSpecialCardsQueue) {
            if (card.getRemainingForcedDraws() > 0) {
                suspendedCard = card;
                break;
            }
        }
        
        if (suspendedCard == null) {
            System.out.println("   ✅ Aucune pioche suspendue trouvée");
            return false;
        }
        
        // Retirer de la queue
        pendingSpecialCardsQueue.remove(suspendedCard);
        
        GamePlayer player = getPlayerById(suspendedCard.getSourcePlayerId());
        int remaining = suspendedCard.getRemainingForcedDraws();
        
        System.out.println("   ♻️ Reprise pioche suspendue: " + player.getUsername() + " (" + remaining + " cartes)");
        
        // Changer le tour vers ce joueur
        currentPlayerIndex = players.indexOf(player);
        
        // Reprendre la pioche
        boolean completed = processForcedDraws(player, remaining);
        
        if (completed) {
            System.out.println("   ✅ Pioche suspendue complétée");
        } else {
            System.out.println("   ⏸️ Pioche suspendue à nouveau interrompue");
        }
        
        return true;
    }
    
    /**
     * Traite la prochaine carte spéciale en attente dans la queue.
     * Si la queue est vide et qu'il reste des cartes à piocher, reprend la pioche forcée.
     * Sinon, passe au joueur suivant.
     */
    private void processNextPendingCard() {
        System.out.println("📋 processNextPendingCard() - État de la queue:");
        System.out.println("   - Taille de la queue: " + pendingSpecialCardsQueue.size());
        
        if (pendingSpecialCardsQueue.isEmpty()) {
            System.out.println("   ✅ Queue vide - vérification reprise pioche forcée");
            
            // Vérifier si le joueur actuel a des cartes restantes à piocher
            GamePlayer currentPlayer = getCurrentPlayer();
            int remainingDraws = currentPlayer.getRemainingForcedDraws();
            
            if (remainingDraws > 0) {
                System.out.println("   ▶️  Reprise pioche forcée: " + currentPlayer.getUsername() + " (" + remainingDraws + " cartes)");
                currentPlayer.setRemainingForcedDraws(0);
                forceDrawThreeCards(currentPlayer, remainingDraws);
                
                // Après la reprise, re-vérifier la queue
                if (!pendingSpecialCardsQueue.isEmpty()) {
                    System.out.println("   ⏸️  Nouvelles cartes dans la queue après reprise - on attend");
                    return;
                }
            }
            
            System.out.println("   ➡️  Pas de pioche forcée, passage au joueur suivant");
            nextPlayer();
        } else {
            PendingSpecialCard nextPending = pendingSpecialCardsQueue.peek();
            System.out.println("   ⏸️  Carte en attente: " + nextPending.toString());
            System.out.println("   ⏸️  Le joueur doit d'abord assigner cette carte");
            // On ne fait rien, on attend que le joueur assigne la carte
        }
    }
}
