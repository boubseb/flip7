export interface IndivStats {
  completedGames: number;
  victories: number;
  totalFlip7: number;
  totalRounds: number;
  maxScoreInOneRound: number;
  averagePointsPerRound: number;
  averagePointsPerScoringRound: number;
  roundsEliminatedByDouble: number;
  stopCardsDrawn: number;
  stopCardsReceived: number;
  lifeCardsObtained: number;
  lifeCardsDrawn: number;
  drawThreeDrawn: number;
  drawThreeReceived: number;
  drawThreeCompleted: number;
  drawThreeWithElimination: number;
  drawThreeDealtSuccess: number;
  selfAssignedSpecialCards: number;
}

export interface GlobalStats {
  completedGames: number;
  averageRoundsPerGame: number;
  totalFlip7: number;
  maxScoreInOneRound: number;
  theoreticalMaxScore: number;
}

export interface PlayerStatistics {
  indiv: IndivStats;
  team: IndivStats;
  globalIndiv: GlobalStats;
  globalTeam: GlobalStats;
}
