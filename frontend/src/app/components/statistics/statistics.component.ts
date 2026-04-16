import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, NgTemplateOutlet } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { UserService } from '../../services/user/user.service';
import { StatisticsService, StatsFilters } from '../../services/statistics/statistics.service';
import { PlayerStatistics } from '../../models/statistics/player-statistics.model';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule, NgTemplateOutlet, TranslateModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss'
})
export class StatisticsComponent implements OnInit, OnDestroy {
  statistics: PlayerStatistics | null = null;
  loading: boolean = true;
  error: string | null = null;
  currentUserId: string | null = null;
  activeTab: 'indiv' | 'team' = 'indiv';

  // Filtres : null = all, true/false = filtre actif
  filterPersistentDeck: boolean | null = null;
  filterStatsEnabled: boolean | null = null;
  filterCompletedOnly: boolean | null = null;

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
    const filters: StatsFilters = {
      persistentDeck: this.filterPersistentDeck,
      statsEnabled:   this.filterStatsEnabled,
      completedOnly:  this.filterCompletedOnly
    };
    this.statisticsService.getPlayerStatistics(this.currentUserId, filters).subscribe({
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

  setFilter(key: 'persistentDeck' | 'statsEnabled' | 'completedOnly', value: boolean | null): void {
    if (key === 'persistentDeck') this.filterPersistentDeck = value;
    else if (key === 'statsEnabled') this.filterStatsEnabled = value;
    else this.filterCompletedOnly = value;
    this.loadStatistics();
  }

  formatNumber(num: number): string {
    return num.toFixed(2);
  }

  readonly CARD_ORDER = [
    '0','1','2','3','4','5','6','7','8','9','10','11','12',
    'PLUS_2','PLUS_4','PLUS_6','PLUS_8','PLUS_10','X2',
    'STOP','DRAW_THREE','LIFE'
  ];

  readonly SCORE_BUCKET_ORDER = ['0','1-10','11-20','21-30','31-40','41+'];

  cardLabel(key: string): string {
    const labels: Record<string, string> = {
      'X2': '×2', 'PLUS_2': '+2', 'PLUS_4': '+4', 'PLUS_6': '+6',
      'PLUS_8': '+8', 'PLUS_10': '+10',
      'STOP': 'Stop', 'DRAW_THREE': '+3', 'LIFE': 'Vie'
    };
    return labels[key] ?? key;
  }

  sortedCardEntries(map: Record<string, number>): [string, number][] {
    if (!map) return [];
    return this.CARD_ORDER
      .filter(k => (map[k] ?? 0) > 0)
      .map(k => [k, map[k] ?? 0] as [string, number]);
  }

  sortedElimEntries(map: Record<string, number>): [string, number][] {
    if (!map) return [];
    return Array.from({ length: 13 }, (_, i) => String(i))
      .filter(k => (map[k] ?? 0) > 0)
      .map(k => [k, map[k] ?? 0] as [string, number]);
  }
}
