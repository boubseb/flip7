import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlayerStatistics } from '../../models/statistics/player-statistics.model';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private apiUrl = `${environment.apiUrl}/api/statistics`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère les statistiques d'un joueur
   */
  getPlayerStatistics(userId: string): Observable<PlayerStatistics> {
    return this.http.get<PlayerStatistics>(`${this.apiUrl}/${userId}`);
  }
}
