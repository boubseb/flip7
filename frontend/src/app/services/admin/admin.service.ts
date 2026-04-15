import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserAdminDTO {
  id: string;
  pseudo: string;
  email: string;
  role: string;
}

export interface RoomAdminDTO {
  id: string;
  adminId: string;
  status: string;
  playerCount: number;
  createdAt: string;
  lastActivityAt: string;
}

export interface GameHistoryAdminDTO {
  id: string;
  roomId: string;
  status: string;
  winnerId: string;
  startedAt: string;
  endedAt: string;
  totalRounds: number;
}

export interface AdminStatsDTO {
  totalUsers: number;
  totalRooms: number;
  activeRooms: number;
  totalGames: number;
  gamesLast7Days: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private url = environment.apiUrl + '/admin';

  getStats(): Observable<AdminStatsDTO> {
    return this.http.get<AdminStatsDTO>(`${this.url}/stats`);
  }

  // Users
  getUsers(): Observable<UserAdminDTO[]> {
    return this.http.get<UserAdminDTO[]>(`${this.url}/users`);
  }

  updateUserRole(userId: string, role: string): Observable<any> {
    return this.http.put(`${this.url}/users/${userId}/role`, { role });
  }

  resetUserPassword(userId: string, newPassword: string): Observable<any> {
    return this.http.put(`${this.url}/users/${userId}/password`, { newPassword });
  }

  deleteUser(userId: string): Observable<any> {
    return this.http.delete(`${this.url}/users/${userId}`);
  }

  // Rooms
  getRooms(): Observable<RoomAdminDTO[]> {
    return this.http.get<RoomAdminDTO[]>(`${this.url}/rooms`);
  }

  deleteRoom(roomId: string): Observable<any> {
    return this.http.delete(`${this.url}/rooms/${roomId}`);
  }

  // History
  getHistory(): Observable<GameHistoryAdminDTO[]> {
    return this.http.get<GameHistoryAdminDTO[]>(`${this.url}/history`);
  }

  deleteHistory(gameId: string): Observable<any> {
    return this.http.delete(`${this.url}/history/${gameId}`);
  }

  deleteHistoryByRoom(roomId: string): Observable<any> {
    return this.http.delete(`${this.url}/history/room/${roomId}`);
  }
}
