package com.flip7.flip7.service;

import com.flip7.flip7.dto.PlayerStatistics;
import com.flip7.flip7.entity.GameHistory;
import com.flip7.flip7.repository.GameHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    /**
     * Calcule les statistiques d'un joueur, séparées en indiv / équipe.
     * Les filtres sont optionnels (null = pas de filtre sur ce critère).
     */
    public PlayerStatistics getPlayerStatistics(String userId,
                                                Boolean persistentDeckFilter,
                                                Boolean statsEnabledFilter,
                                                Boolean completedOnlyFilter) {
        PlayerStatistics stats = new PlayerStatistics();

        List<GameHistory> playerGames = gameHistoryRepository.findByPlayerIdsContaining(userId);
        System.out.println("📊 getPlayerStatistics: userId=" + userId + " → " + playerGames.size() + " partie(s)");

        // Appliquer les filtres
        List<GameHistory> filtered = playerGames.stream()
            .filter(g -> persistentDeckFilter == null || g.isPersistentDeck() == persistentDeckFilter)
            .filter(g -> statsEnabledFilter  == null || g.isStatisticsEnabled() == statsEnabledFilter)
            .filter(g -> completedOnlyFilter == null
                || (completedOnlyFilter && g.getStatus() == GameHistory.GameStatus.COMPLETED)
                || (!completedOnlyFilter && g.getStatus() != GameHistory.GameStatus.COMPLETED))
            .collect(Collectors.toList());

        List<GameHistory> indivGames = filtered.stream().filter(g -> !g.isTeamMode()).collect(Collectors.toList());
        List<GameHistory> teamGames  = filtered.stream().filter(GameHistory::isTeamMode).collect(Collectors.toList());

        stats.setIndiv(computePersonalStats(indivGames, userId));
        stats.setTeam(computePersonalStats(teamGames, userId));

        calculateGlobalStatistics(stats);

        return stats;
    }

    // ── Personal stats ─────────────────────────────────────────────────────────

    private PlayerStatistics.IndivStats computePersonalStats(List<GameHistory> games, String userId) {
        PlayerStatistics.IndivStats s = new PlayerStatistics.IndivStats();

        s.setTotalGames(games.size());
        int completedGames = 0, victories = 0;
        int totalFlip7 = 0, totalRounds = 0;
        int maxScore = 0, totalPoints = 0, scoringRounds = 0;
        int roundsEliminatedByDouble = 0;
        int stopCardsDrawn = 0, stopCardsReceived = 0;
        int x2CardsDrawn = 0;
        int lifeCardsObtained = 0, lifeCardsDrawn = 0;
        int drawThreeDrawn = 0, drawThreeReceived = 0;
        int drawThreeCompleted = 0, drawThreeWithElim = 0, drawThreeDealtSuccess = 0;
        int selfAssigned = 0;
        int x2WithFlip7 = 0, x2RoundsNotEliminated = 0;
        int x2RoundsCount = 0, x2TotalPoints = 0;
        int flip7TotalPoints = 0;
        Map<String, Integer> cardsDrawnTotal = new HashMap<>();
        Map<String, Integer> eliminationsByCard = new HashMap<>();
        Map<String, Integer> scoreDistribution = new HashMap<>(Map.of(
            "0", 0, "1-10", 0, "11-20", 0, "21-30", 0, "31-40", 0, "41+", 0));

        for (GameHistory game : games) {
            if (game.getStatus() == GameHistory.GameStatus.COMPLETED) {
                completedGames++;
                if (userId.equals(game.getWinnerId())) victories++;
            }

            for (GameHistory.RoundHistory round : game.getRounds()) {
                GameHistory.PlayerRoundData pd = round.getPlayerData().stream()
                        .filter(p -> p.getPlayerId().equals(userId))
                        .findFirst().orElse(null);
                if (pd == null) continue;

                totalRounds++;
                int score = pd.getRoundScore();
                totalPoints += score;
                if (score > 0) scoringRounds++;
                if (score > maxScore) maxScore = score;

                if (pd.isHasSevenDifferent()) totalFlip7++;
                if (pd.isEliminatedByDouble() && !pd.isEliminatedByDrawThree()) roundsEliminatedByDouble++;

                stopCardsDrawn += pd.getStopCardsDrawn();
                x2CardsDrawn   += pd.getX2CardsDrawn();
                if (pd.isReceivedStopCard()) stopCardsReceived++;

                lifeCardsObtained += pd.getLifeCardsObtained();
                lifeCardsDrawn    += pd.getLifeCardsDrawn();

                drawThreeDrawn  += pd.getDrawThreeDrawn();
                if (pd.isReceivedDrawThree()) {
                    drawThreeReceived++;
                    if (pd.isCompletedDrawThree()) drawThreeCompleted++;
                    if (pd.isEliminatedByDrawThree()) drawThreeWithElim++;
                }
                drawThreeDealtSuccess += pd.getDrawThreeDealtSuccess();
                selfAssigned          += pd.getSelfAssignedSpecialCards();

                boolean drewX2 = pd.getX2CardsDrawn() > 0;
                if (drewX2) {
                    x2RoundsCount++;
                    x2TotalPoints += score;
                    if (!pd.isEliminated()) x2RoundsNotEliminated++;
                    if (pd.isHasSevenDifferent()) x2WithFlip7++;
                }
                if (pd.isHasSevenDifferent()) flip7TotalPoints += score;

                // Distribution des cartes piochées
                if (pd.getCardDrawCounts() != null) {
                    pd.getCardDrawCounts().forEach((k, v) -> cardsDrawnTotal.merge(k, v, Integer::sum));
                }
                if (pd.getEliminatingCardValue() >= 0) {
                    eliminationsByCard.merge(String.valueOf(pd.getEliminatingCardValue()), 1, Integer::sum);
                }
                String bucket = score == 0 ? "0" : score <= 10 ? "1-10" : score <= 20 ? "11-20"
                    : score <= 30 ? "21-30" : score <= 40 ? "31-40" : "41+";
                scoreDistribution.merge(bucket, 1, Integer::sum);
            }
        }

        s.setCompletedGames(completedGames);
        s.setVictories(victories);
        s.setTotalFlip7(totalFlip7);
        s.setTotalRounds(totalRounds);
        s.setMaxScoreInOneRound(maxScore);
        s.setRoundsEliminatedByDouble(roundsEliminatedByDouble);
        s.setStopCardsDrawn(stopCardsDrawn);
        s.setX2CardsDrawn(x2CardsDrawn);
        s.setStopCardsReceived(stopCardsReceived);
        s.setLifeCardsObtained(lifeCardsObtained);
        s.setLifeCardsDrawn(lifeCardsDrawn);
        s.setDrawThreeDrawn(drawThreeDrawn);
        s.setDrawThreeReceived(drawThreeReceived);
        s.setDrawThreeCompleted(drawThreeCompleted);
        s.setDrawThreeWithElimination(drawThreeWithElim);
        s.setDrawThreeDealtSuccess(drawThreeDealtSuccess);
        s.setSelfAssignedSpecialCards(selfAssigned);

        if (totalRounds > 0) s.setAveragePointsPerRound((double) totalPoints / totalRounds);
        if (scoringRounds > 0) s.setAveragePointsPerScoringRound((double) totalPoints / scoringRounds);

        s.setX2WithFlip7(x2WithFlip7);
        s.setX2RoundsNotEliminated(x2RoundsNotEliminated);
        if (x2RoundsCount > 0) s.setAvgScoreWithX2((double) x2TotalPoints / x2RoundsCount);
        if (totalFlip7 > 0) s.setAvgScoreFlip7((double) flip7TotalPoints / totalFlip7);

        s.setCardsDrawnTotal(cardsDrawnTotal);
        s.setEliminationsByCard(eliminationsByCard);
        s.setScoreDistribution(scoreDistribution);

        return s;
    }

    // ── Global stats ──────────────────────────────────────────────────────────

    private void calculateGlobalStatistics(PlayerStatistics stats) {
        List<GameHistory> allGames = gameHistoryRepository.findAll();

        int indivCompleted = 0, teamCompleted = 0;
        int indivTotalRounds = 0, teamTotalRounds = 0;
        int indivFlip7 = 0, teamFlip7 = 0;
        int indivMaxScore = 0, teamMaxScore = 0;
        int indivX2Flip7 = 0, teamX2Flip7 = 0;
        int indivX2Rounds = 0, teamX2Rounds = 0;
        int indivX2Points = 0, teamX2Points = 0;
        int indivFlip7Points = 0, teamFlip7Points = 0;
        Map<String, Integer> indivCardsDrawn = new HashMap<>();
        Map<String, Integer> teamCardsDrawn = new HashMap<>();
        Map<String, Integer> indivElimByCard = new HashMap<>();
        Map<String, Integer> teamElimByCard = new HashMap<>();

        for (GameHistory game : allGames) {
            boolean isTeam = game.isTeamMode();
            boolean completed = game.getStatus() == GameHistory.GameStatus.COMPLETED;

            if (completed) {
                if (isTeam) {
                    teamCompleted++;
                    teamTotalRounds += game.getTotalRounds();
                } else {
                    indivCompleted++;
                    indivTotalRounds += game.getTotalRounds();
                }
            }

            for (GameHistory.RoundHistory round : game.getRounds()) {
                for (GameHistory.PlayerRoundData pd : round.getPlayerData()) {
                    boolean flip7 = pd.isHasSevenDifferent();
                    boolean drewX2 = pd.getX2CardsDrawn() > 0;
                    int score = pd.getRoundScore();
                    if (flip7) {
                        if (isTeam) { teamFlip7++; teamFlip7Points += score; }
                        else        { indivFlip7++; indivFlip7Points += score; }
                    }
                    if (drewX2) {
                        if (isTeam) { teamX2Rounds++; teamX2Points += score; if (flip7) teamX2Flip7++; }
                        else        { indivX2Rounds++; indivX2Points += score; if (flip7) indivX2Flip7++; }
                    }
                    if (isTeam) {
                        if (score > teamMaxScore) teamMaxScore = score;
                    } else {
                        if (score > indivMaxScore) indivMaxScore = score;
                    }

                    // Distribution des cartes
                    Map<String, Integer> cardsTarget = isTeam ? teamCardsDrawn : indivCardsDrawn;
                    Map<String, Integer> elimTarget   = isTeam ? teamElimByCard : indivElimByCard;
                    if (pd.getCardDrawCounts() != null) {
                        pd.getCardDrawCounts().forEach((k, v) -> cardsTarget.merge(k, v, Integer::sum));
                    }
                    if (pd.getEliminatingCardValue() >= 0) {
                        elimTarget.merge(String.valueOf(pd.getEliminatingCardValue()), 1, Integer::sum);
                    }
                }
            }
        }

        PlayerStatistics.GlobalStats gi = stats.getGlobalIndiv();
        gi.setCompletedGames(indivCompleted);
        gi.setTotalFlip7(indivFlip7);
        gi.setMaxScoreInOneRound(indivMaxScore);
        gi.setX2WithFlip7(indivX2Flip7);
        if (indivCompleted > 0) gi.setAverageRoundsPerGame((double) indivTotalRounds / indivCompleted);
        if (indivX2Rounds > 0) gi.setAvgScoreWithX2((double) indivX2Points / indivX2Rounds);
        if (indivFlip7 > 0) gi.setAvgScoreFlip7((double) indivFlip7Points / indivFlip7);
        gi.setCardsDrawnTotal(indivCardsDrawn);
        gi.setEliminationsByCard(indivElimByCard);

        PlayerStatistics.GlobalStats gt = stats.getGlobalTeam();
        gt.setCompletedGames(teamCompleted);
        gt.setTotalFlip7(teamFlip7);
        gt.setMaxScoreInOneRound(teamMaxScore);
        gt.setX2WithFlip7(teamX2Flip7);
        if (teamCompleted > 0) gt.setAverageRoundsPerGame((double) teamTotalRounds / teamCompleted);
        if (teamX2Rounds > 0) gt.setAvgScoreWithX2((double) teamX2Points / teamX2Rounds);
        if (teamFlip7 > 0) gt.setAvgScoreFlip7((double) teamFlip7Points / teamFlip7);
        gt.setCardsDrawnTotal(teamCardsDrawn);
        gt.setEliminationsByCard(teamElimByCard);
    }
}
