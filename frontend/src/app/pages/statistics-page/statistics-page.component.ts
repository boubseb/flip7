import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { StatisticsService } from '../../services/statistics/statistics.service';
import { UserService } from '../../services/user/user.service';
import { PlayerStatistics } from '../../models/statistics/player-statistics.model';

@Component({
  selector: 'app-statistics-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './statistics-page.component.html',
  styleUrl: './statistics-page.component.scss'
})
export class StatisticsPageComponent implements OnInit {
  statistics: PlayerStatistics | null = null;
  loading: boolean = true;
  error: string | null = null;
  currentUserId: string | null = null;

  constructor(
    private statisticsService: StatisticsService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
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
      error: () => {
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

  goBack(): void {
    this.router.navigate(['/']);
  }

  formatNumber(num: number): string {
    return num.toFixed(2);
  }
}
