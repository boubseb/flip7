package com.flip7.flip7.dto;

/**
 * DTO contenant les statistiques d'un joueur
 */
public class PlayerStatistics {
    // Statistiques du joueur
    private int totalGames;                  // Nombre total de parties du joueur
    private int completedGames;              // Nombre de parties finies (avec vainqueur)
    private int victories;                   // Nombre de victoires
    private int totalFlip7;                  // Nombre de Flip7 réalisés
    private int totalRounds;                 // Nombre total de rounds joués
    private int roundsEliminatedByDouble;    // Rounds éliminés par un double
    private int maxScoreInOneRound;          // Plus gros score en 1 round
    private double averagePointsPerRound;    // Moyenne de points par round
    private double averagePointsPerScoringRound; // Moyenne par round avec score > 0
    private int stopCardsReceived;           // Nombre de cartes Stop subies
    private int lifeCardsObtained;           // Nombre de cartes Vie obtenues
    private int drawThreeReceived;           // Nombre de cartes +3 reçues
    private int drawThreeCompleted;          // Nombre de +3 réussis
    private int drawThreeWithElimination;    // Nombre de +3 avec élimination par double
    
    // Statistiques globales (accessibles à tous)
    private int globalCompletedGames;        // Nombre total de parties finies (tous joueurs)
    private double globalAverageRoundsPerGame; // Moyenne de rounds par partie finie
    private int globalTotalFlip7;            // Nombre total de Flip7 (parties finies)
    private int globalMaxScoreInOneRound;    // Plus gros score réalisé en 1 round (tous joueurs)
    private int theoreticalMaxScore;         // Plus gros score théorique possible en 1 round

    public PlayerStatistics() {
        // Score théorique max : +2 +4 +6 +8 +10 + (12+11+10+9+8+7+6) avec x2 = 30 + 126 = 156
        this.theoreticalMaxScore = 156;
    }

    // Getters et Setters
    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getCompletedGames() {
        return completedGames;
    }

    public void setCompletedGames(int completedGames) {
        this.completedGames = completedGames;
    }

    public int getVictories() {
        return victories;
    }

    public void setVictories(int victories) {
        this.victories = victories;
    }

    public int getTotalFlip7() {
        return totalFlip7;
    }

    public void setTotalFlip7(int totalFlip7) {
        this.totalFlip7 = totalFlip7;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }

    public int getRoundsEliminatedByDouble() {
        return roundsEliminatedByDouble;
    }

    public void setRoundsEliminatedByDouble(int roundsEliminatedByDouble) {
        this.roundsEliminatedByDouble = roundsEliminatedByDouble;
    }

    public int getMaxScoreInOneRound() {
        return maxScoreInOneRound;
    }

    public void setMaxScoreInOneRound(int maxScoreInOneRound) {
        this.maxScoreInOneRound = maxScoreInOneRound;
    }

    public double getAveragePointsPerRound() {
        return averagePointsPerRound;
    }

    public void setAveragePointsPerRound(double averagePointsPerRound) {
        this.averagePointsPerRound = averagePointsPerRound;
    }

    public double getAveragePointsPerScoringRound() {
        return averagePointsPerScoringRound;
    }

    public void setAveragePointsPerScoringRound(double averagePointsPerScoringRound) {
        this.averagePointsPerScoringRound = averagePointsPerScoringRound;
    }

    public int getStopCardsReceived() {
        return stopCardsReceived;
    }

    public void setStopCardsReceived(int stopCardsReceived) {
        this.stopCardsReceived = stopCardsReceived;
    }

    public int getLifeCardsObtained() {
        return lifeCardsObtained;
    }

    public void setLifeCardsObtained(int lifeCardsObtained) {
        this.lifeCardsObtained = lifeCardsObtained;
    }

    public int getDrawThreeReceived() {
        return drawThreeReceived;
    }

    public void setDrawThreeReceived(int drawThreeReceived) {
        this.drawThreeReceived = drawThreeReceived;
    }

    public int getDrawThreeCompleted() {
        return drawThreeCompleted;
    }

    public void setDrawThreeCompleted(int drawThreeCompleted) {
        this.drawThreeCompleted = drawThreeCompleted;
    }

    public int getDrawThreeWithElimination() {
        return drawThreeWithElimination;
    }

    public void setDrawThreeWithElimination(int drawThreeWithElimination) {
        this.drawThreeWithElimination = drawThreeWithElimination;
    }

    public int getGlobalCompletedGames() {
        return globalCompletedGames;
    }

    public void setGlobalCompletedGames(int globalCompletedGames) {
        this.globalCompletedGames = globalCompletedGames;
    }

    public double getGlobalAverageRoundsPerGame() {
        return globalAverageRoundsPerGame;
    }

    public void setGlobalAverageRoundsPerGame(double globalAverageRoundsPerGame) {
        this.globalAverageRoundsPerGame = globalAverageRoundsPerGame;
    }

    public int getGlobalTotalFlip7() {
        return globalTotalFlip7;
    }

    public void setGlobalTotalFlip7(int globalTotalFlip7) {
        this.globalTotalFlip7 = globalTotalFlip7;
    }

    public int getGlobalMaxScoreInOneRound() {
        return globalMaxScoreInOneRound;
    }

    public void setGlobalMaxScoreInOneRound(int globalMaxScoreInOneRound) {
        this.globalMaxScoreInOneRound = globalMaxScoreInOneRound;
    }

    public int getTheoreticalMaxScore() {
        return theoreticalMaxScore;
    }

    public void setTheoreticalMaxScore(int theoreticalMaxScore) {
        this.theoreticalMaxScore = theoreticalMaxScore;
    }
}
