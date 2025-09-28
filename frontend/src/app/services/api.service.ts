import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = 'http://localhost:8080/api';
  constructor(private http: HttpClient) {}

  register(username: string, password: string, email?: string): Observable<any> {
    return this.http.post(`${this.base}/auth/register`, { username, password, email });
  }
  login(username: string, password: string): Observable<any> {
    return this.http.post(`${this.base}/auth/login`, { username, password });
  }
  listRooms(): Observable<any> { return this.http.get(`${this.base}/rooms`); }
  createRoom(name: string, password?: string) { return this.http.post(`${this.base}/rooms/create`, { name, password }); }
  joinRoom(name: string, password?: string) { return this.http.post(`${this.base}/rooms/join`, { name, password }); }
}
