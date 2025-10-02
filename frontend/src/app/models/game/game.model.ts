import { AnyCard } from './card.model';

/**
 * États du jeu
 */
export enum GameState {
  WAITING = 'WAITING',
  DISTRIBUTING = 'DISTRIBUTING',
  PLAYING = 'PLAYING',
  ROUND_ENDED = 'ROUND_ENDED',
  GAME_OVER = 'GAME_OVER'
}

/**
 * Statuts d'un joueur
 */
export enum PlayerStatus {
  PLAYING = 'PLAYING',
  STOPPED = 'STOPPED',
  ELIMINATED = 'ELIMINATED',
  WAITING = 'WAITING'
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
  lifeCardsInHand?: number;
  hand?: AnyCard[]; // Seulement pour le joueur actuel
}

/**
 * État complet du jeu
 */
export interface GameStateResponse {
  gameState: GameState;
  roundNumber: number;
  currentPlayerIndex: number;
  remainingCards: number;
  players: GamePlayer[];
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
