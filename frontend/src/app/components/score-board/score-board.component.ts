import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-score-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './score-board.component.html',
  styleUrl: './score-board.component.scss'
})
export class ScoreBoardComponent implements OnInit, OnChanges {
  @Input() gameState: any;
  @Input() players: string[] = [];
  @Input() playerInfos: any[] = [];

  selectedRound: number = 1;
  maxRound: number = 1;

  ngOnInit(): void {
    this.updateMaxRound();
  }

  ngOnChanges(): void {
    this.updateMaxRound();
    // Ajuster selectedRound si nécessaire
    if (this.selectedRound > this.maxRound) {
      this.selectedRound = this.maxRound;
    }
  }

  updateMaxRound(): void {
    if (this.gameState && this.gameState.currentRound) {
      this.maxRound = this.gameState.currentRound;
      this.selectedRound = this.maxRound; // Par défaut sur le round actuel
    }
  }

  previousRound(): void {
    if (this.selectedRound > 1) {
      this.selectedRound--;
    }
  }

  nextRound(): void {
    if (this.selectedRound < this.maxRound) {
      this.selectedRound++;
    }
  }

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

  getPlayerRoundScore(playerId: string): number {
    // Score gagné pendant le round sélectionné
    if (!this.gameState || !this.gameState.players || !this.gameState.players[playerId]) {
      return 0;
    }
    const roundScores = this.gameState.players[playerId].roundScores || [];
    return roundScores[this.selectedRound - 1] || 0;
  }

  getPlayerCumulativeScore(playerId: string): number {
    // Score cumulé jusqu'au round sélectionné
    if (!this.gameState || !this.gameState.players || !this.gameState.players[playerId]) {
      return 0;
    }
    const roundScores = this.gameState.players[playerId].roundScores || [];
    let cumulative = 0;
    for (let i = 0; i < this.selectedRound && i < roundScores.length; i++) {
      cumulative += roundScores[i] || 0;
    }
    return cumulative;
  }
}
