package com.flip7.flip7.service;

import com.flip7.flip7.dto.PlayerStatistics;
import com.flip7.flip7.entity.GameHistory;
import com.flip7.flip7.repository.GameHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticsService {

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    /**
     * Calcule les statistiques d'un joueur
     */
    public PlayerStatistics getPlayerStatistics(String userId) {
        PlayerStatistics stats = new PlayerStatistics();

        // Récupérer toutes les parties du joueur
        List<GameHistory> playerGames = gameHistoryRepository.findByPlayerIdsContaining(userId);
        stats.setTotalGames(playerGames.size());

        // Statistiques du joueur
        int completedGames = 0;
        int victories = 0;
        int totalFlip7 = 0;
        int totalRounds = 0;
        int roundsEliminatedByDouble = 0;
        int maxScoreInOneRound = 0;
        int totalPoints = 0;
        int scoringRounds = 0; // Rounds avec score > 0
        int stopCardsReceived = 0;
        int lifeCardsObtained = 0;
        int drawThreeReceived = 0;
        int drawThreeCompleted = 0;
        int drawThreeWithElimination = 0;

        for (GameHistory game : playerGames) {
            if (game.getStatus() == GameHistory.GameStatus.COMPLETED) {
                completedGames++;
                
                // Vérifier si le joueur a gagné
                if (userId.equals(game.getWinnerId())) {
                    victories++;
                }
            }

            // Parcourir les rounds pour les statistiques détaillées
            for (GameHistory.RoundHistory round : game.getRounds()) {
                GameHistory.PlayerRoundData playerData = round.getPlayerData().stream()
                        .filter(p -> p.getPlayerId().equals(userId))
                        .findFirst()
                        .orElse(null);

                if (playerData != null) {
                    totalRounds++;

                    // Score du round
                    int roundScore = playerData.getRoundScore();
                    totalPoints += roundScore;
                    if (roundScore > 0) {
                        scoringRounds++;
                    }
                    if (roundScore > maxScoreInOneRound) {
                        maxScoreInOneRound = roundScore;
                    }

                    // Flip7
                    if (playerData.isHasSevenDifferent()) {
                        totalFlip7++;
                    }

                    // Éliminations par double
                    if (playerData.isEliminatedByDouble()) {
                        roundsEliminatedByDouble++;
                    }

                    // Cartes Stop
                    if (playerData.isReceivedStopCard()) {
                        stopCardsReceived++;
                    }

                    // Cartes Vie
                    lifeCardsObtained += playerData.getLifeCardsObtained();

                    // Cartes +3
                    if (playerData.isReceivedDrawThree()) {
                        drawThreeReceived++;
                        
                        if (playerData.isCompletedDrawThree()) {
                            drawThreeCompleted++;
                        }
                        
                        if (playerData.isEliminatedByDrawThree()) {
                            drawThreeWithElimination++;
                        }
                    }
                }
            }
        }

        stats.setCompletedGames(completedGames);
        stats.setVictories(victories);
        stats.setTotalFlip7(totalFlip7);
        stats.setTotalRounds(totalRounds);
        stats.setRoundsEliminatedByDouble(roundsEliminatedByDouble);
        stats.setMaxScoreInOneRound(maxScoreInOneRound);
        stats.setStopCardsReceived(stopCardsReceived);
        stats.setLifeCardsObtained(lifeCardsObtained);
        stats.setDrawThreeReceived(drawThreeReceived);
        stats.setDrawThreeCompleted(drawThreeCompleted);
        stats.setDrawThreeWithElimination(drawThreeWithElimination);

        // Moyennes
        if (totalRounds > 0) {
            stats.setAveragePointsPerRound((double) totalPoints / totalRounds);
        }
        if (scoringRounds > 0) {
            stats.setAveragePointsPerScoringRound((double) totalPoints / scoringRounds);
        }

        // Statistiques globales
        calculateGlobalStatistics(stats);

        return stats;
    }

    /**
     * Calcule les statistiques globales (tous joueurs)
     */
    private void calculateGlobalStatistics(PlayerStatistics stats) {
        // Récupérer toutes les parties terminées
        List<GameHistory> completedGames = gameHistoryRepository.findByStatus(GameHistory.GameStatus.COMPLETED);
        stats.setGlobalCompletedGames(completedGames.size());

        if (completedGames.isEmpty()) {
            return;
        }

        int totalRounds = 0;
        int totalFlip7 = 0;
        int maxScoreInOneRound = 0;

        for (GameHistory game : completedGames) {
            totalRounds += game.getTotalRounds();

            // Parcourir les rounds pour trouver le max score et les Flip7
            for (GameHistory.RoundHistory round : game.getRounds()) {
                for (GameHistory.PlayerRoundData playerData : round.getPlayerData()) {
                    // Score maximum
                    if (playerData.getRoundScore() > maxScoreInOneRound) {
                        maxScoreInOneRound = playerData.getRoundScore();
                    }

                    // Flip7
                    if (playerData.isHasSevenDifferent()) {
                        totalFlip7++;
                    }
                }
            }
        }

        stats.setGlobalTotalFlip7(totalFlip7);
        stats.setGlobalMaxScoreInOneRound(maxScoreInOneRound);
        
        if (completedGames.size() > 0) {
            stats.setGlobalAverageRoundsPerGame((double) totalRounds / completedGames.size());
        }
    }
}
