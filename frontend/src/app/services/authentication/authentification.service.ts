import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../../models/user/user';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  constructor() { }

  http = inject(HttpClient);
  url = environment.apiUrl;

  private isLoginSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(
    typeof localStorage !== 'undefined' && localStorage.getItem('access_token') !== null
  );
  isLogin$: Observable<boolean> = this.isLoginSubject.asObservable();

  private roleSubject: BehaviorSubject<string> = new BehaviorSubject<string>(
    typeof localStorage !== 'undefined' ? (localStorage.getItem('user_role') ?? 'USER') : 'USER'
  );
  role$: Observable<string> = this.roleSubject.asObservable();

  login(username: string, password: string): Observable<any> {
    const params = new HttpParams().set('password', password).set('username', username);
    return this.http.post(this.url + '/login', {}, { params });
  }

  setToken(token: string): void {
    localStorage.setItem('access_token', token);
    this.isLoginSubject.next(true);
  }

  setRole(role: string): void {
    localStorage.setItem('user_role', role);
    this.roleSubject.next(role);
  }

  getToken(): string | null {
    return localStorage.getItem('access_token');
  }

  getRole(): string {
    return localStorage.getItem('user_role') ?? 'USER';
  }

  isAdmin(): boolean {
    const r = this.getRole();
    return r === 'ADMIN' || r === 'SUPERADMIN';
  }

  isSuperAdmin(): boolean {
    return this.getRole() === 'SUPERADMIN';
  }

  removeToken(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_role');
    this.isLoginSubject.next(false);
    this.roleSubject.next('USER');
  }

  register(user: User): Observable<any> {
    return this.http.post<User>(this.url + '/register', user);
  }

  changePassword(password: string): Observable<any> {
    return this.http.post(this.url + '/changePassword', { password });
  }

  deleteAccount(password: string): Observable<any> {
    const params = new HttpParams().set('password', password);
    return this.http.put(this.url + '/deleteAccount', {}, { params });
  }
}
