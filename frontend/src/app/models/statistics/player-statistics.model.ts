export interface PlayerStatistics {
  // Statistiques du joueur
  totalGames: number;                  // Nombre total de parties du joueur
  completedGames: number;              // Nombre de parties finies (avec vainqueur)
  victories: number;                   // Nombre de victoires
  totalFlip7: number;                  // Nombre de Flip7 réalisés
  totalRounds: number;                 // Nombre total de rounds joués
  roundsEliminatedByDouble: number;    // Rounds éliminés par un double
  maxScoreInOneRound: number;          // Plus gros score en 1 round
  averagePointsPerRound: number;       // Moyenne de points par round
  averagePointsPerScoringRound: number; // Moyenne par round avec score > 0
  stopCardsReceived: number;           // Nombre de cartes Stop subies
  lifeCardsObtained: number;           // Nombre de cartes Vie obtenues
  drawThreeReceived: number;           // Nombre de cartes +3 reçues
  drawThreeCompleted: number;          // Nombre de +3 réussis
  drawThreeWithElimination: number;    // Nombre de +3 avec élimination par double
  
  // Statistiques globales (accessibles à tous)
  globalCompletedGames: number;        // Nombre total de parties finies (tous joueurs)
  globalAverageRoundsPerGame: number;  // Moyenne de rounds par partie finie
  globalTotalFlip7: number;            // Nombre total de Flip7 (parties finies)
  globalMaxScoreInOneRound: number;    // Plus gros score réalisé en 1 round (tous joueurs)
  theoreticalMaxScore: number;         // Plus gros score théorique possible en 1 round
}
