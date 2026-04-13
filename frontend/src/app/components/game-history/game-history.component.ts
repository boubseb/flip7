import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { StatisticsService } from '../../services/statistics/statistics.service';
import { UserService } from '../../services/user/user.service';
import { GameHistory, GameHistoryStatus } from '../../models/game/game-history.model';

@Component({
  selector: 'app-game-history',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './game-history.component.html',
  styleUrl: './game-history.component.scss'
})
export class GameHistoryComponent implements OnInit {
  games: GameHistory[] = [];
  loading = true;
  error: string | null = null;
  currentUserId: string | null = null;
  expandedGameId: string | null = null;

  GameHistoryStatus = GameHistoryStatus;

  constructor(
    private statisticsService: StatisticsService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.userService.getUserProfile().subscribe({
      next: (profile: any) => {
        this.currentUserId = profile?.id ?? null;
        this.loadHistory();
      },
      error: () => {
        this.error = 'history.errorLoading';
        this.loading = false;
      }
    });
  }

  loadHistory(): void {
    this.loading = true;
    this.error = null;
    this.statisticsService.getMyHistory().subscribe({
      next: (games: any[]) => {
        // Sort newest first
        this.games = games.sort((a, b) =>
          new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime()
        );
        this.loading = false;
      },
      error: () => {
        this.error = 'history.errorLoading';
        this.loading = false;
      }
    });
  }

  toggleGame(gameId: string): void {
    this.expandedGameId = this.expandedGameId === gameId ? null : gameId;
  }

  isExpanded(gameId: string): boolean {
    return this.expandedGameId === gameId;
  }

  isMe(playerId: string): boolean {
    return playerId === this.currentUserId;
  }

  getWinnerName(game: GameHistory): string {
    if (!game.winnerId) return '—';
    // Try from finalScores first
    const fs = game.finalScores?.find(s => s.playerId === game.winnerId);
    if (fs) return fs.username;
    // Fallback: from any round
    for (const round of game.rounds ?? []) {
      const pd = round.playerData?.find(p => p.playerId === game.winnerId);
      if (pd) return pd.username;
    }
    return game.winnerId.substring(0, 8);
  }

  getMyScore(game: GameHistory): number | null {
    if (!this.currentUserId) return null;
    const fs = game.finalScores?.find(s => s.playerId === this.currentUserId);
    return fs ? fs.totalScore : null;
  }

  getMyRoundScore(round: any): number | null {
    if (!this.currentUserId) return null;
    const pd = round.playerData?.find((p: any) => p.playerId === this.currentUserId);
    return pd ? pd.roundScore : null;
  }

  getPlayerName(playerId: string, game: GameHistory): string {
    const fs = game.finalScores?.find(s => s.playerId === playerId);
    if (fs) return fs.username;
    for (const round of game.rounds ?? []) {
      const pd = round.playerData?.find(p => p.playerId === playerId);
      if (pd) return pd.username;
    }
    return playerId.substring(0, 8);
  }

  getFinalScore(game: GameHistory, playerId: string): number {
    const fs = game.finalScores?.find(s => s.playerId === playerId);
    return fs ? fs.totalScore : 0;
  }

  getRoundScore(round: any, playerId: string): number | null {
    const pd = round.playerData?.find((p: any) => p.playerId === playerId);
    return pd ? pd.roundScore : null;
  }

  getRoundStatus(round: any, playerId: string): string {
    const pd = round.playerData?.find((p: any) => p.playerId === playerId);
    if (!pd) return '';
    if (pd.hasSevenDifferent) return 'flip7';
    if (pd.eliminated) return 'eliminated';
    if (pd.stopped) return 'stopped';
    return 'active';
  }

  isRoundWinner(round: any, playerId: string): boolean {
    return round.roundWinnerId === playerId;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString(undefined, { day: '2-digit', month: '2-digit', year: 'numeric' })
      + ' ' + d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }

  didIWin(game: GameHistory): boolean {
    return !!this.currentUserId && game.winnerId === this.currentUserId;
  }
}
