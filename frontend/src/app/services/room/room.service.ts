import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  createRoom(request: RoomCreateRequest): Observable<RoomCreateResponse> {
    return this.http.post<RoomCreateResponse>(
      `${this.apiUrl}/create`,
      request,
      { headers: this.getHeaders() }
    );
  }

  joinRoom(request: RoomJoinRequest): Observable<Room> {
    return this.http.post<Room>(
      `${this.apiUrl}/join`,
      request,
      { headers: this.getHeaders() }
    );
  }

  getRoom(roomId: string): Observable<Room> {
    return this.http.get<Room>(
      `${this.apiUrl}/${roomId}`,
      { headers: this.getHeaders() }
    );
  }

  getAvailableRooms(): Observable<Room[]> {
    return this.http.get<Room[]>(
      `${this.apiUrl}/available`,
      { headers: this.getHeaders() }
    );
  }

  startGame(roomId: string): Observable<Room> {
    return this.http.post<Room>(
      `${this.apiUrl}/${roomId}/start`,
      {},
      { headers: this.getHeaders() }
    );
  }

  playTurn(roomId: string, move: string): Observable<Room> {
    return this.http.post<Room>(
      `${this.apiUrl}/${roomId}/play`,
      { move },
      { headers: this.getHeaders() }
    );
  }
}
