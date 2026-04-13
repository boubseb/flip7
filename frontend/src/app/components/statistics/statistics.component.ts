import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
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
export class StatisticsComponent implements OnInit, OnDestroy {
  statistics: PlayerStatistics | null = null;
  loading: boolean = true;
  error: string | null = null;
  currentUserId: string | null = null;
  private gameEndedSub: Subscription | null = null;

  constructor(
    private statisticsService: StatisticsService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.userService.getUserProfile().subscribe({
      next: (profile: any) => {
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

    // Se recharger automatiquement quand une partie se termine
    this.gameEndedSub = this.statisticsService.gameEnded$.subscribe(() => {
      if (this.currentUserId) {
        this.loadStatistics();
      }
    });
  }

  ngOnDestroy(): void {
    this.gameEndedSub?.unsubscribe();
  }

  loadStatistics(): void {
    if (!this.currentUserId) return;
    this.loading = true;
    this.error = null;
    this.statisticsService.getPlayerStatistics(this.currentUserId).subscribe({
      next: (stats: any) => {
        this.statistics = stats;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les statistiques';
        this.loading = false;
      }
    });
  }

  formatNumber(num: number): string {
    return num.toFixed(2);
  }
}
