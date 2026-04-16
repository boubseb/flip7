export interface IndivStats {
  totalGames: number;
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
  x2CardsDrawn: number;
  lifeCardsObtained: number;
  lifeCardsDrawn: number;
  drawThreeDrawn: number;
  drawThreeReceived: number;
  drawThreeCompleted: number;
  drawThreeWithElimination: number;
  drawThreeDealtSuccess: number;
  selfAssignedSpecialCards: number;
  x2WithFlip7: number;
  x2RoundsNotEliminated: number;
  avgScoreWithX2: number;
  avgScoreFlip7: number;
}

export interface GlobalStats {
  completedGames: number;
  averageRoundsPerGame: number;
  totalFlip7: number;
  maxScoreInOneRound: number;
  theoreticalMaxScore: number;
  x2WithFlip7: number;
  avgScoreWithX2: number;
  avgScoreFlip7: number;
}

export interface PlayerStatistics {
  indiv: IndivStats;
  team: IndivStats;
  globalIndiv: GlobalStats;
  globalTeam: GlobalStats;
}
