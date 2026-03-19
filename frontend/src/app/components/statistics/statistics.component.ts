import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { UserService } from '../../services/user/user.service';
import { StatisticsService } from '../../services/statistics/statistics.service';
import { PlayerStatistics } from '../../models/statistics/player-statistics.model';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss'
})
export class StatisticsComponent implements OnInit {
  statistics: PlayerStatistics | null = null;
  loading: boolean = true;
  error: string | null = null;
  currentUserId: string | null = null;

  constructor(
    private statisticsService: StatisticsService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    // Récupérer l'ID de l'utilisateur via UserService
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        if (profile && profile.id) {
          this.currentUserId = profile.id;
          this.loadStatistics();
        } else {
          this.error = 'Utilisateur non connecté';
          this.loading = false;
        }
      },
      error: (err) => {
        this.error = 'Utilisateur non connecté';
        this.loading = false;
      }
    });
  }

  loadStatistics(): void {
    if (!this.currentUserId) return;

    this.loading = true;
    this.error = null;

    this.statisticsService.getPlayerStatistics(this.currentUserId).subscribe({
      next: (stats) => {
        this.statistics = stats;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des statistiques:', err);
        this.error = 'Impossible de charger les statistiques';
        this.loading = false;
      }
    });
  }



  formatNumber(num: number): string {
    return num.toFixed(2);
  }
}
