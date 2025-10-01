export enum RoomStatus {
  WAITING = 'WAITING',
  IN_GAME = 'IN_GAME',
  FINISHED = 'FINISHED'
}

export interface Room {
  id: string;
  adminId: string;
  players: string[];
  status: RoomStatus;
  turnIndex: number;
  currentPlayerId: string;
  createdAt: Date;
  maxPlayers: number;
}

export interface RoomCreateRequest {
  password: string;
  maxPlayers?: number;
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
