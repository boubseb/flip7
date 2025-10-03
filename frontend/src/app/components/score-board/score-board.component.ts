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
    if (this.gameState && this.gameState.roundNumber) {
      this.maxRound = this.gameState.roundNumber;
      this.selectedRound = this.maxRound; // Par défaut sur le round actuel
    }
  }

  isCurrentRound(): boolean {
    return this.selectedRound === this.maxRound;
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

  /**
   * Récupère les données du round sélectionné pour un joueur
   */
  private getPlayerRoundData(playerId: string): any {
    if (!this.gameState || !this.gameState.players) {
      return null;
    }
    
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    if (!player) {
      return null;
    }

    // Si on regarde un round dans l'historique
    if (player.rounds && player.rounds.length > 0 && this.selectedRound <= player.rounds.length) {
      return player.rounds[this.selectedRound - 1];
    }
    
    // Sinon, retourner les données actuelles du joueur
    return player;
  }

  getPlayerScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      return 0;
    }
    return roundData.theoreticalTotal || 0;
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

  /**
   * Score réalisé durant le round sélectionné
   * - Round terminé : roundScore du round
   * - Round en cours : roundScore actuel
   */
  getPlayerRoundScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      return 0;
    }
    return roundData.roundScore || 0;
  }

  /**
   * Score cumulé jusqu'au round sélectionné (incluant ce round)
   * - Round terminé : theoreticalTotal du round (= totalScore à la fin du round)
   * - Round en cours : theoreticalTotal actuel (score théorique si on s'arrête maintenant)
   */
  getPlayerCumulativeScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      return 0;
    }
    return roundData.theoreticalTotal || 0;
  }
}
