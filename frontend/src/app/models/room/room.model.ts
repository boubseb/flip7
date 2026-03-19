export enum RoomStatus {
  WAITING = 'WAITING',
  IN_GAME = 'IN_GAME',
  FINISHED = 'FINISHED'
}

export interface PlayerInfo {
  id: string;
  pseudo: string;
}

export interface Room {
  id: string;
  adminId: string;
  players: string[]; // kept for backward compatibility
  playerInfos?: PlayerInfo[]; // new field with pseudo
  status: RoomStatus;
  turnIndex: number;
  currentPlayerId: string;
  createdAt: Date;
  maxPlayers: number;
  currentPlayers: number; // Added for room browser
  gameState?: any; // Game state (will be typed properly later)
  statisticsEnabled?: boolean;
  targetScore?: number; // Score cible pour gagner (défaut: 200)
}

export interface RoomCreateRequest {
  password: string;
  maxPlayers?: number;
  statisticsEnabled?: boolean;
  targetScore?: number; // Score cible pour gagner
}

export interface RoomJoinRequest {
  roomId: string;
  password: string;
}

export interface RoomCreateResponse {
  roomId: string;
  message: string;
}

export interface TurnUpdate {
  room: Room;
  moveData: string;
}
