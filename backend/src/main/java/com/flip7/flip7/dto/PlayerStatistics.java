package com.flip7.flip7.dto;

/**
 * DTO contenant les statistiques d'un joueur, séparées en sections indiv et équipe.
 */
public class PlayerStatistics {

    private IndivStats indiv = new IndivStats();       // Perso — parties solo
    private IndivStats team  = new IndivStats();       // Perso — parties équipe
    private GlobalStats globalIndiv = new GlobalStats(); // Global — parties solo
    private GlobalStats globalTeam  = new GlobalStats(); // Global — parties équipe

    // ── Getters / setters ────────────────────────────────────────────────────
    public IndivStats getIndiv()           { return indiv; }
    public void setIndiv(IndivStats v)     { this.indiv = v; }
    public IndivStats getTeam()            { return team; }
    public void setTeam(IndivStats v)      { this.team = v; }
    public GlobalStats getGlobalIndiv()    { return globalIndiv; }
    public void setGlobalIndiv(GlobalStats v) { this.globalIndiv = v; }
    public GlobalStats getGlobalTeam()     { return globalTeam; }
    public void setGlobalTeam(GlobalStats v)  { this.globalTeam = v; }

    // ── Nested: stats personnelles d'une catégorie de parties ────────────────
    public static class IndivStats {
        private int totalGames;
        private int completedGames;
        private int victories;
        private int totalFlip7;
        private int totalRounds;
        private int maxScoreInOneRound;
        private double averagePointsPerRound;
        private double averagePointsPerScoringRound;
        private int roundsEliminatedByDouble;

        // Cartes Stop / ×2
        private int stopCardsDrawn;    // Piochées (perspective source)
        private int stopCardsReceived; // Reçues (perspective cible)
        private int x2CardsDrawn;      // ×2 piochés

        // Cartes Vie
        private int lifeCardsObtained; // Reçues (toutes méthodes confondues)
        private int lifeCardsDrawn;    // Piochées (comptées en mode équipe)

        // Cartes +3
        private int drawThreeDrawn;         // Piochées (source)
        private int drawThreeReceived;      // Reçues (cible)
        private int drawThreeCompleted;     // Réussies sans élimination (cible)
        private int drawThreeWithElimination; // Éliminé pendant un +3
        private int drawThreeDealtSuccess;  // +3 distribués avec succès (source)

        // Cartes spéciales auto-attribuées
        private int selfAssignedSpecialCards;

        // Combos ×2
        private int x2WithFlip7;           // Rounds avec ×2 ET Flip7
        private int x2RoundsNotEliminated; // Rounds avec ×2 sans élimination
        private double avgScoreWithX2;     // Moyenne des scores dans les rounds avec ×2
        private double avgScoreFlip7;      // Moyenne des scores dans les rounds Flip7

        // Getters et Setters
        public int getTotalGames() { return totalGames; }
        public void setTotalGames(int v) { this.totalGames = v; }
        public int getCompletedGames() { return completedGames; }
        public void setCompletedGames(int v) { this.completedGames = v; }
        public int getVictories() { return victories; }
        public void setVictories(int v) { this.victories = v; }
        public int getTotalFlip7() { return totalFlip7; }
        public void setTotalFlip7(int v) { this.totalFlip7 = v; }
        public int getTotalRounds() { return totalRounds; }
        public void setTotalRounds(int v) { this.totalRounds = v; }
        public int getMaxScoreInOneRound() { return maxScoreInOneRound; }
        public void setMaxScoreInOneRound(int v) { this.maxScoreInOneRound = v; }
        public double getAveragePointsPerRound() { return averagePointsPerRound; }
        public void setAveragePointsPerRound(double v) { this.averagePointsPerRound = v; }
        public double getAveragePointsPerScoringRound() { return averagePointsPerScoringRound; }
        public void setAveragePointsPerScoringRound(double v) { this.averagePointsPerScoringRound = v; }
        public int getRoundsEliminatedByDouble() { return roundsEliminatedByDouble; }
        public void setRoundsEliminatedByDouble(int v) { this.roundsEliminatedByDouble = v; }
        public int getStopCardsDrawn() { return stopCardsDrawn; }
        public void setStopCardsDrawn(int v) { this.stopCardsDrawn = v; }
        public int getStopCardsReceived() { return stopCardsReceived; }
        public void setStopCardsReceived(int v) { this.stopCardsReceived = v; }
        public int getX2CardsDrawn() { return x2CardsDrawn; }
        public void setX2CardsDrawn(int v) { this.x2CardsDrawn = v; }
        public int getLifeCardsObtained() { return lifeCardsObtained; }
        public void setLifeCardsObtained(int v) { this.lifeCardsObtained = v; }
        public int getLifeCardsDrawn() { return lifeCardsDrawn; }
        public void setLifeCardsDrawn(int v) { this.lifeCardsDrawn = v; }
        public int getDrawThreeDrawn() { return drawThreeDrawn; }
        public void setDrawThreeDrawn(int v) { this.drawThreeDrawn = v; }
        public int getDrawThreeReceived() { return drawThreeReceived; }
        public void setDrawThreeReceived(int v) { this.drawThreeReceived = v; }
        public int getDrawThreeCompleted() { return drawThreeCompleted; }
        public void setDrawThreeCompleted(int v) { this.drawThreeCompleted = v; }
        public int getDrawThreeWithElimination() { return drawThreeWithElimination; }
        public void setDrawThreeWithElimination(int v) { this.drawThreeWithElimination = v; }
        public int getDrawThreeDealtSuccess() { return drawThreeDealtSuccess; }
        public void setDrawThreeDealtSuccess(int v) { this.drawThreeDealtSuccess = v; }
        public int getSelfAssignedSpecialCards() { return selfAssignedSpecialCards; }
        public void setSelfAssignedSpecialCards(int v) { this.selfAssignedSpecialCards = v; }
        public int getX2WithFlip7() { return x2WithFlip7; }
        public void setX2WithFlip7(int v) { this.x2WithFlip7 = v; }
        public int getX2RoundsNotEliminated() { return x2RoundsNotEliminated; }
        public void setX2RoundsNotEliminated(int v) { this.x2RoundsNotEliminated = v; }
        public double getAvgScoreWithX2() { return avgScoreWithX2; }
        public void setAvgScoreWithX2(double v) { this.avgScoreWithX2 = v; }
        public double getAvgScoreFlip7() { return avgScoreFlip7; }
        public void setAvgScoreFlip7(double v) { this.avgScoreFlip7 = v; }
    }

    // ── Nested: statistiques globales d'une catégorie de parties ─────────────
    public static class GlobalStats {
        private int completedGames;
        private double averageRoundsPerGame;
        private int totalFlip7;
        private int maxScoreInOneRound;
        private int theoreticalMaxScore = 156; // +2+4+6+8+10 + (12+11+10+9+8+7+6) × 2
        private int x2WithFlip7;
        private double avgScoreWithX2;
        private double avgScoreFlip7;

        public int getCompletedGames() { return completedGames; }
        public void setCompletedGames(int v) { this.completedGames = v; }
        public double getAverageRoundsPerGame() { return averageRoundsPerGame; }
        public void setAverageRoundsPerGame(double v) { this.averageRoundsPerGame = v; }
        public int getTotalFlip7() { return totalFlip7; }
        public void setTotalFlip7(int v) { this.totalFlip7 = v; }
        public int getMaxScoreInOneRound() { return maxScoreInOneRound; }
        public void setMaxScoreInOneRound(int v) { this.maxScoreInOneRound = v; }
        public int getTheoreticalMaxScore() { return theoreticalMaxScore; }
        public void setTheoreticalMaxScore(int v) { this.theoreticalMaxScore = v; }
        public int getX2WithFlip7() { return x2WithFlip7; }
        public void setX2WithFlip7(int v) { this.x2WithFlip7 = v; }
        public double getAvgScoreWithX2() { return avgScoreWithX2; }
        public void setAvgScoreWithX2(double v) { this.avgScoreWithX2 = v; }
        public double getAvgScoreFlip7() { return avgScoreFlip7; }
        public void setAvgScoreFlip7(double v) { this.avgScoreFlip7 = v; }
    }
}
