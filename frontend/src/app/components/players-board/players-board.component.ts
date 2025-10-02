import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-players-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './players-board.component.html',
  styleUrl: './players-board.component.scss'
})
export class PlayersBoardComponent {
  @Input() gameState: any;
  @Input() players: string[] = [];
  @Input() playerInfos: any[] = [];
  @Input() currentUserId: string = '';
  @Input() currentPlayerId: string = '';

  getPlayerPseudo(playerId: string): string {
    if (!this.playerInfos) {
      return playerId.substring(0, 8) + '...';
    }
    const playerInfo = this.playerInfos.find(p => p.id === playerId);
    return playerInfo ? playerInfo.pseudo : playerId.substring(0, 8) + '...';
  }

  getPlayerHandCount(playerId: string): number {
    if (!this.gameState || !this.gameState.players || !this.gameState.players[playerId]) {
      return 0;
    }
    return this.gameState.players[playerId].hand?.length || 0;
  }

  getPlayerCards(playerId: string): any[] {
    if (!this.gameState || !this.gameState.players || !this.gameState.players[playerId]) {
      return [];
    }
    return this.gameState.players[playerId].hand || [];
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
}
