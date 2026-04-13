import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlayerStatistics } from '../../models/statistics/player-statistics.model';

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
   * Récupère les statistiques d'un joueur
   */
  getPlayerStatistics(userId: string): Observable<PlayerStatistics> {
    return this.http.get<PlayerStatistics>(`${this.apiUrl}/${userId}`);
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
