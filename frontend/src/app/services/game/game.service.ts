import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  GameStateResponse,
  DrawResult,
  ActionResult,
  PlaySpecialCardRequest,
  RoundEndData,
  GameOverData,
  CardDistributedEvent
} from '../../models/game/game.model';
import { GameHistory, PlayerStats } from '../../models/game/game-history.model';

/**
 * Service pour gérer la logique du jeu Flip7
 */
@Injectable({
  providedIn: 'root'
})
export class GameService {
  private apiUrl = `${environment.apiUrl}/game`;
  private historyUrl = `${environment.apiUrl}/history`;

  // État du jeu en cache
  private gameStateSubject = new BehaviorSubject<GameStateResponse | null>(null);
  public gameState$ = this.gameStateSubject.asObservable();

  // Événements de round
  private roundEndSubject = new BehaviorSubject<RoundEndData | null>(null);
  public roundEnd$ = this.roundEndSubject.asObservable();

  private gameOverSubject = new BehaviorSubject<GameOverData | null>(null);
  public gameOver$ = this.gameOverSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Headers avec Authorization
   */
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('authToken') || '';
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Démarre un nouveau round
   */
  startNewRound(roomId: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/${roomId}/start-round`,
      {},
      { headers: this.getHeaders() }
    );
  }

  /**
   * Pioche une carte
   */
  drawCard(roomId: string): Observable<DrawResult> {
    return this.http.post<DrawResult>(
      `${this.apiUrl}/${roomId}/draw`,
      {},
      { headers: this.getHeaders() }
    );
  }

  /**
   * Arrête de piocher
   */
  stopDrawing(roomId: string): Observable<ActionResult> {
    return this.http.post<ActionResult>(
      `${this.apiUrl}/${roomId}/stop`,
      {},
      { headers: this.getHeaders() }
    );
  }

  /**
   * Joue une carte spéciale
   */
  playSpecialCard(roomId: string, request: PlaySpecialCardRequest): Observable<ActionResult> {
    return this.http.post<ActionResult>(
      `${this.apiUrl}/${roomId}/play-special`,
      request,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupère l'état actuel du jeu
   */
  getGameState(roomId: string): Observable<GameStateResponse> {
    return this.http.get<GameStateResponse>(
      `${this.apiUrl}/${roomId}/state`,
      { headers: this.getHeaders() }
    ).pipe(
      tap(state => this.gameStateSubject.next(state))
    );
  }

  /**
   * Abandonne la partie
   */
  abandonGame(roomId: string, reason?: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/${roomId}/abandon`,
      { reason },
      { headers: this.getHeaders() }
    );
  }

  /**
   * Quitte la partie
   */
  leaveGame(roomId: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/${roomId}/leave`,
      {},
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupère l'historique d'une room
   */
  getRoomHistory(roomId: string): Observable<GameHistory[]> {
    return this.http.get<GameHistory[]>(
      `${this.historyUrl}/room/${roomId}`
    );
  }

  /**
   * Récupère l'historique d'un joueur
   */
  getPlayerHistory(playerId: string): Observable<GameHistory[]> {
    return this.http.get<GameHistory[]>(
      `${this.historyUrl}/player/${playerId}`
    );
  }

  /**
   * Récupère mon historique
   */
  getMyHistory(): Observable<GameHistory[]> {
    return this.http.get<GameHistory[]>(
      `${this.historyUrl}/me`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupère les statistiques d'un joueur
   */
  getPlayerStats(playerId: string): Observable<PlayerStats> {
    return this.http.get<PlayerStats>(
      `${this.historyUrl}/player/${playerId}/stats`
    );
  }

  /**
   * Récupère mes statistiques
   */
  getMyStats(): Observable<PlayerStats> {
    return this.http.get<PlayerStats>(
      `${this.historyUrl}/me/stats`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Met à jour l'état du jeu localement (appelé par WebSocket)
   */
  updateGameState(state: GameStateResponse): void {
    this.gameStateSubject.next(state);
  }

  /**
   * Notifie la fin d'un round (appelé par WebSocket)
   */
  notifyRoundEnd(data: RoundEndData): void {
    this.roundEndSubject.next(data);
  }

  /**
   * Notifie la fin de la partie (appelé par WebSocket)
   */
  notifyGameOver(data: GameOverData): void {
    this.gameOverSubject.next(data);
  }

  /**
   * Réinitialise l'état local
   */
  clearGameState(): void {
    this.gameStateSubject.next(null);
    this.roundEndSubject.next(null);
    this.gameOverSubject.next(null);
  }
}
