/**
 * Statut d'une partie dans l'historique
 */
export enum GameHistoryStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  ABANDONED = 'ABANDONED'
}

/**
 * Données d'un joueur pour un round
 */
export interface PlayerRoundData {
  playerId: string;
  username: string;
  roundScore: number;
  eliminated: boolean;
  stopped: boolean;
  hasSevenDifferent: boolean;
  cardsDrawn: number;
  usedLife: boolean;
}

/**
 * Historique d'un round
 */
export interface RoundHistory {
  roundNumber: number;
  startedAt: string;
  endedAt: string;
  roundWinnerId: string;
  playerData: PlayerRoundData[];
}

/**
 * Score final d'un joueur
 */
export interface PlayerScore {
  playerId: string;
  username: string;
  totalScore: number;
  roundsWon: number;
  roundsPlayed: number;
}

/**
 * Historique complet d'une partie
 */
export interface GameHistory {
  id: string;
  roomId: string;
  playerIds: string[];
  startedAt: string;
  endedAt?: string;
  status: GameHistoryStatus;
  winnerId?: string;
  totalRounds: number;
  rounds: RoundHistory[];
  finalScores: PlayerScore[];
}

/**
 * Statistiques d'un joueur
 */
export interface PlayerStats {
  playerId: string;
  totalGamesPlayed: number;
  totalWins: number;
  totalScore: number;
  averageScore: number;
  winRate: number;
  totalRoundsPlayed: number;
  totalRoundsWon: number;
}
