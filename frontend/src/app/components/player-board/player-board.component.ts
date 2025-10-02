import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-player-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './player-board.component.html',
  styleUrl: './player-board.component.scss'
})
export class PlayerBoardComponent {
  @Input() gameState: any;
  @Input() currentUserId: string = '';
  @Input() isMyTurn: boolean = false;
  @Output() onPlayCard = new EventEmitter<any>();

  cardRevealed: boolean = false;
  isFlipping: boolean = false;

  getMyHand(): any[] {
    if (!this.gameState || !this.gameState.players || !this.gameState.players[this.currentUserId]) {
      return [];
    }
    return this.gameState.players[this.currentUserId].hand || [];
  }

  getMyRevealedCards(): any[] {
    if (!this.gameState || !this.gameState.players || !this.gameState.players[this.currentUserId]) {
      return [];
    }
    return this.gameState.players[this.currentUserId].revealedCards || [];
  }

  getCardSuit(card: string): string {
    if (!card) return '';
    const parts = card.split('_');
    if (parts.length < 2) return '';
    
    const suit = parts[1];
    const suitSymbols: { [key: string]: string } = {
      'HEARTS': '♥',
      'DIAMONDS': '♦',
      'CLUBS': '♣',
      'SPADES': '♠'
    };
    return suitSymbols[suit] || suit;
  }

  getCardValue(card: string): string {
    if (!card) return '';
    const parts = card.split('_');
    return parts[0] || '';
  }

  getCurrentCard(): string | null {
    const hand = this.getMyHand();
    return hand.length > 0 ? hand[0] : null;
  }

  continueRevealing(): void {
    if (this.isFlipping) return;
    
    this.isFlipping = true;
    this.cardRevealed = true;
    
    // Animation de flip (600ms)
    setTimeout(() => {
      this.isFlipping = false;
      // Émettre l'action de révélation
      const card = this.getCurrentCard();
      if (card) {
        this.onPlayCard.emit({ action: 'REVEAL', card });
      }
    }, 600);
  }

  stopRevealing(): void {
    if (this.isFlipping) return;
    
    // Émettre l'action de stop
    this.onPlayCard.emit({ action: 'STOP' });
  }

  playCard(card: any): void {
    this.onPlayCard.emit(card);
  }
}
