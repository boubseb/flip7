import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevealedCardsComponent } from '../revealed-cards/revealed-cards.component';

@Component({
  selector: 'app-players-board',
  standalone: true,
  imports: [CommonModule, RevealedCardsComponent],
  templateUrl: './players-board.component.html',
  styleUrl: './players-board.component.scss'
})
export class PlayersBoardComponent implements OnInit, OnChanges {
  @Input() gameState: any;
  @Input() players: string[] = [];
  @Input() playerInfos: any[] = [];
  @Input() currentUserId: string = '';
  @Input() currentPlayerId: string = '';

  expandedPlayers: Set<string> = new Set();

  ngOnInit(): void {
    // Initialiser tous les joueurs comme déployés au chargement
    this.players.forEach(playerId => this.expandedPlayers.add(playerId));
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Ajouter les nouveaux joueurs comme déployés
    if (changes['players'] && this.players) {
      this.players.forEach(playerId => this.expandedPlayers.add(playerId));
    }
  }

  isExpanded(playerId: string): boolean {
    return this.expandedPlayers.has(playerId);
  }

  togglePlayer(playerId: string): void {
    if (this.expandedPlayers.has(playerId)) {
      this.expandedPlayers.delete(playerId);
    } else {
      this.expandedPlayers.add(playerId);
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
    if (!this.gameState || !this.gameState.players) {
      return 0;
    }
    // gameState.players est un tableau
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    return player?.roundScore || 0;
  }

  getRevealedCards(playerId: string): any[] {
    if (!this.gameState || !this.gameState.players) {
      return [];
    }
    // gameState.players est un tableau
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    return player?.hand || [];
  }

  getSuitSymbol(suit: string): string {
    const suitMap: { [key: string]: string } = {
      'HEARTS': '♥',
      'DIAMONDS': '♦',
      'CLUBS': '♣',
      'SPADES': '♠'
    };
    return suitMap[suit] || suit;
  }
}
