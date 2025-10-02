import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-score-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './score-board.component.html',
  styleUrl: './score-board.component.scss'
})
export class ScoreBoardComponent {
  @Input() gameState: any;
  @Input() players: string[] = [];
  @Input() playerInfos: any[] = [];

  getPlayerPseudo(playerId: string): string {
    if (!this.playerInfos) {
      return playerId.substring(0, 8) + '...';
    }
    const playerInfo = this.playerInfos.find(p => p.id === playerId);
    return playerInfo ? playerInfo.pseudo : playerId.substring(0, 8) + '...';
  }

  getPlayerScore(playerId: string): number {
    if (!this.gameState || !this.gameState.players || !this.gameState.players[playerId]) {
      return 0;
    }
    return this.gameState.players[playerId].score || 0;
  }

  getPlayerTricks(playerId: string): number {
    if (!this.gameState || !this.gameState.players || !this.gameState.players[playerId]) {
      return 0;
    }
    return this.gameState.players[playerId].tricks || 0;
  }

  getSortedPlayers(): string[] {
    return [...this.players].sort((a, b) => {
      return this.getPlayerScore(b) - this.getPlayerScore(a);
    });
  }

  getRank(playerId: string): number {
    const sorted = this.getSortedPlayers();
    return sorted.indexOf(playerId) + 1;
  }
}
