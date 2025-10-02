import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Room, RoomCreateRequest, RoomCreateResponse, RoomJoinRequest } from '../../models/room/room.model';

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private apiUrl = 'http://localhost:3200/api/rooms';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  // Helper to add currentPlayers to Room objects
  private enrichRoom(room: any): Room {
    return {
      ...room,
      currentPlayers: room.players?.length || 0
    };
  }

  private getHeaders(): HttpHeaders {
    let token = '';
    if (isPlatformBrowser(this.platformId)) {
      token = localStorage.getItem('access_token') || '';
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  createRoom(request: RoomCreateRequest): Observable<Room> {
    return this.http.post<any>(
      `${this.apiUrl}/create`,
      request,
      { headers: this.getHeaders() }
    ).pipe(map(room => this.enrichRoom(room)));
  }

  joinRoom(request: RoomJoinRequest): Observable<Room> {
    return this.http.post<any>(
      `${this.apiUrl}/join`,
      request,
      { headers: this.getHeaders() }
    ).pipe(map(room => this.enrichRoom(room)));
  }

  getRoom(roomId: string): Observable<Room> {
    return this.http.get<any>(
      `${this.apiUrl}/${roomId}`,
      { headers: this.getHeaders() }
    ).pipe(map(room => this.enrichRoom(room)));
  }

  getAvailableRooms(): Observable<Room[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/available`,
      { headers: this.getHeaders() }
    ).pipe(map(rooms => rooms.map(room => this.enrichRoom(room))));
  }

  startGame(roomId: string): Observable<Room> {
    return this.http.post<any>(
      `${this.apiUrl}/${roomId}/start`,
      {},
      { headers: this.getHeaders() }
    ).pipe(map(room => this.enrichRoom(room)));
  }

  playTurn(roomId: string, move: string): Observable<Room> {
    return this.http.post<any>(
      `${this.apiUrl}/${roomId}/play`,
      { move },
      { headers: this.getHeaders() }
    ).pipe(map(room => this.enrichRoom(room)));
  }
}
