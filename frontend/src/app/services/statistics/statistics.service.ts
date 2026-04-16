import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlayerStatistics } from '../../models/statistics/player-statistics.model';

export interface StatsFilters {
  persistentDeck?: boolean | null;  // null = all
  statsEnabled?: boolean | null;
  completedOnly?: boolean | null;
}

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private apiUrl = `${environment.apiUrl}/api/statistics`;

  // Émis quand une partie se termine pour forcer le rechargement des stats
  private gameEndedSource = new Subject<void>();
  public gameEnded$ = this.gameEndedSource.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Récupère les statistiques d'un joueur avec filtres optionnels
   */
  getPlayerStatistics(userId: string, filters?: StatsFilters): Observable<PlayerStatistics> {
    let params = new HttpParams();
    if (filters?.persistentDeck != null) params = params.set('persistentDeck', String(filters.persistentDeck));
    if (filters?.statsEnabled    != null) params = params.set('statsEnabled',    String(filters.statsEnabled));
    if (filters?.completedOnly   != null) params = params.set('completedOnly',   String(filters.completedOnly));
    return this.http.get<PlayerStatistics>(`${this.apiUrl}/${userId}`, { params });
  }

  getMyHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/api/history/me`);
  }

  /**
   * À appeler depuis la game-page quand la partie se termine
   */
  notifyGameEnded(): void {
    this.gameEndedSource.next();
  }
}
