import { AnyCard } from './card.model';

/**
 * États du jeu
 */
export enum GameState {
  WAITING = 'WAITING',
  DISTRIBUTING = 'DISTRIBUTING',
  PLAYING = 'PLAYING',
  ROUND_ENDED = 'ROUND_ENDED',
  WAITING_NEXT_ROUND = 'WAITING_NEXT_ROUND',
  GAME_OVER = 'GAME_OVER'
}

/**
 * Statuts d'un joueur
 */
export enum PlayerStatus {
  PLAYING = 'PLAYING',
  STOPPED = 'STOPPED',
  FORCED_STOP = 'FORCED_STOP', // Forcé de s'arrêter par une carte Stop
  ELIMINATED = 'ELIMINATED',
  WAITING = 'WAITING',
  FLIP7_STOP = 'FLIP7_STOP' // A réalisé un Flip7 (7 cartes numérotées différentes)
}

/**
 * Données d'un round historique
 */
export interface RoundData {
  roundNumber: number;
  roundScore: number;
  totalScore: number;
  theoreticalTotal: number;
  status: PlayerStatus;
  hand?: AnyCard[];
}

/**
 * Joueur dans le contexte du jeu
 */
export interface GamePlayer {
  userId: string;
  username: string;
  status: PlayerStatus;
  handSize: number;
  roundScore: number;
  totalScore: number;
  theoreticalTotal?: number;
  lifeCardsInHand?: number;
  teamId?: number; // 0 = no team (solo mode)
  hand?: AnyCard[]; // Main actuelle
  rounds?: RoundData[]; // Historique des rounds
}

/**
 * Infos d'une équipe (mode équipe)
 */
export interface TeamInfo {
  teamId: number;
  totalScore: number;
  targetScore: number;
  players: string[]; // userIds
}

/**
 * État complet du jeu
 */
export interface GameStateResponse {
  gameState: GameState;
  roundNumber: number;
  currentPlayerIndex: number;
  remainingCards: number;
  winnerId?: string;
  players: GamePlayer[];
  pendingSpecialCards?: PendingSpecialCard[];
  statisticsEnabled?: boolean;
  teamMode?: boolean;
  winningTeamId?: number;
  teams?: { [teamId: number]: TeamInfo };
}

/**
 * Carte spéciale en attente d'assignation
 */
export interface PendingSpecialCard {
  cardId: string;
  cardType: string;
  specialType: string;
  sourcePlayerId: string;
  targetPlayerId?: string;
  remainingForcedDraws: number;
}

/**
 * Résultat d'une pioche
 */
export interface DrawResult {
  success: boolean;
  message: string;
  card?: AnyCard;
  roundEnded?: boolean;
  lifeUsed?: boolean;
  eliminated?: boolean;
  needsStopAssignment?: boolean; // Carte Stop piochée, nécessite un choix
}

/**
 * Résultat d'une action
 */
export interface ActionResult {
  success: boolean;
  message: string;
}

/**
 * Événement de distribution de carte
 */
export interface CardDistributedEvent {
  playerIndex: number;
  playerId: string;
  playerName: string;
}

/**
 * Scores de fin de round
 */
export interface RoundEndData {
  roundNumber: number;
  playerScores: RoundPlayerScore[];
}

export interface RoundPlayerScore {
  userId: string;
  username: string;
  roundScore: number;
  totalScore: number;
  eliminated: boolean;
}

/**
 * Fin de partie
 */
export interface GameOverData {
  winnerId: string;
  winnerName: string;
  winningScore: number;
  finalScores: FinalPlayerScore[];
  teamMode?: boolean;
  winningTeamId?: number;
  teams?: { [teamId: number]: { teamId: number; totalScore: number; players: string[] } };
}

export interface FinalPlayerScore {
  userId: string;
  username: string;
  totalScore: number;
}

/**
 * Requête pour jouer une carte spéciale
 */
export interface PlaySpecialCardRequest {
  cardId: string;
  targetPlayerId: string;
}
