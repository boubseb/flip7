package com.flip7.flip7.service;

import com.flip7.flip7.dto.PlayerStatistics;
import com.flip7.flip7.entity.GameHistory;
import com.flip7.flip7.repository.GameHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    /**
     * Calcule les statistiques d'un joueur, séparées en indiv / équipe.
     */
    public PlayerStatistics getPlayerStatistics(String userId) {
        PlayerStatistics stats = new PlayerStatistics();

        List<GameHistory> playerGames = gameHistoryRepository.findByPlayerIdsContaining(userId);
        System.out.println("📊 getPlayerStatistics: userId=" + userId + " → " + playerGames.size() + " partie(s)");

        List<GameHistory> indivGames = playerGames.stream().filter(g -> !g.isTeamMode()).collect(Collectors.toList());
        List<GameHistory> teamGames  = playerGames.stream().filter(GameHistory::isTeamMode).collect(Collectors.toList());

        stats.setIndiv(computePersonalStats(indivGames, userId));
        stats.setTeam(computePersonalStats(teamGames, userId));

        calculateGlobalStatistics(stats);

        return stats;
    }

    // ── Personal stats ─────────────────────────────────────────────────────────

    private PlayerStatistics.IndivStats computePersonalStats(List<GameHistory> games, String userId) {
        PlayerStatistics.IndivStats s = new PlayerStatistics.IndivStats();

        int completedGames = 0, victories = 0;
        int totalFlip7 = 0, totalRounds = 0;
        int maxScore = 0, totalPoints = 0, scoringRounds = 0;
        int roundsEliminatedByDouble = 0;
        int stopCardsDrawn = 0, stopCardsReceived = 0;
        int lifeCardsObtained = 0, lifeCardsDrawn = 0;
        int drawThreeDrawn = 0, drawThreeReceived = 0;
        int drawThreeCompleted = 0, drawThreeWithElim = 0, drawThreeDealtSuccess = 0;
        int selfAssigned = 0;

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

                stopCardsDrawn   += pd.getStopCardsDrawn();
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
            }
        }

        s.setCompletedGames(completedGames);
        s.setVictories(victories);
        s.setTotalFlip7(totalFlip7);
        s.setTotalRounds(totalRounds);
        s.setMaxScoreInOneRound(maxScore);
        s.setRoundsEliminatedByDouble(roundsEliminatedByDouble);
        s.setStopCardsDrawn(stopCardsDrawn);
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

        return s;
    }

    // ── Global stats ──────────────────────────────────────────────────────────

    private void calculateGlobalStatistics(PlayerStatistics stats) {
        List<GameHistory> allGames = gameHistoryRepository.findAll();

        int indivCompleted = 0, teamCompleted = 0;
        int indivTotalRounds = 0, teamTotalRounds = 0;
        int indivFlip7 = 0, teamFlip7 = 0;
        int indivMaxScore = 0, teamMaxScore = 0;

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
                    if (pd.isHasSevenDifferent()) {
                        if (isTeam) teamFlip7++; else indivFlip7++;
                    }
                    int score = pd.getRoundScore();
                    if (isTeam) {
                        if (score > teamMaxScore) teamMaxScore = score;
                    } else {
                        if (score > indivMaxScore) indivMaxScore = score;
                    }
                }
            }
        }

        PlayerStatistics.GlobalStats gi = stats.getGlobalIndiv();
        gi.setCompletedGames(indivCompleted);
        gi.setTotalFlip7(indivFlip7);
        gi.setMaxScoreInOneRound(indivMaxScore);
        if (indivCompleted > 0) gi.setAverageRoundsPerGame((double) indivTotalRounds / indivCompleted);

        PlayerStatistics.GlobalStats gt = stats.getGlobalTeam();
        gt.setCompletedGames(teamCompleted);
        gt.setTotalFlip7(teamFlip7);
        gt.setMaxScoreInOneRound(teamMaxScore);
        if (teamCompleted > 0) gt.setAverageRoundsPerGame((double) teamTotalRounds / teamCompleted);
    }
}
