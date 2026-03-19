import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { StatisticsService } from '../../services/statistics/statistics.service';
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
    private router: Router
  ) {}

  ngOnInit(): void {
    // Récupérer l'ID de l'utilisateur depuis le localStorage
    this.currentUserId = localStorage.getItem('userId');
    
    if (!this.currentUserId) {
      this.error = 'Utilisateur non connecté';
      this.loading = false;
      return;
    }

    this.loadStatistics();
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
