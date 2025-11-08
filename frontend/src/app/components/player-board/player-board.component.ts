import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevealedCardsComponent } from '../revealed-cards/revealed-cards.component';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-player-board',
  standalone: true,
  imports: [CommonModule, RevealedCardsComponent, TranslateModule],
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
    if (!this.gameState || !this.gameState.players) {
      return [];
    }
    // gameState.players est un tableau, chercher le joueur actuel
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    return myPlayer?.hand || [];
  }

  /**
   * Retourne le score du round en cours
   */
  getMyRoundScore(): number {
    if (!this.gameState || !this.gameState.players) {
      return 0;
    }
    // gameState.players est un tableau, chercher le joueur actuel
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    return myPlayer?.roundScore || 0;
  }

  /**
   * Retourne le score "sécurité" (totalScore des rounds précédents)
   */
  getMySafetyScore(): number {
    if (!this.gameState || !this.gameState.players) {
      return 0;
    }
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    return myPlayer?.totalScore || 0;
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
    console.log('🎲 continueRevealing() called');
    console.log('   - isFlipping:', this.isFlipping);
    
    if (this.isFlipping) {
      console.warn('⚠️ Already flipping, ignoring click');
      return;
    }
    
    this.isFlipping = true;
    this.cardRevealed = true;
    
    // Émettre IMMÉDIATEMENT pour déboguer (pas d'attente d'animation)
    const card = this.getCurrentCard();
    console.log('🎯 Emitting REVEAL action with card:', card);
    this.onPlayCard.emit({ action: 'REVEAL', card: card || 'TEST_CARD' });
    
    // Animation de flip (600ms)
    setTimeout(() => {
      this.isFlipping = false;
      console.log('✅ Animation finished, isFlipping reset');
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

  /**
   * Vérifie si le joueur actuel est éliminé
   */
  isEliminated(): boolean {
    if (!this.gameState || !this.gameState.players) {
      return false;
    }
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    return myPlayer?.status === 'ELIMINATED';
  }

  /**
   * Calcule le score total théorique (score actuel + score du round en cours)
   */
  getMyTheoreticalTotal(): number {
    if (!this.gameState || !this.gameState.players) {
      return 0;
    }
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!myPlayer) {
      return 0;
    }
    return (myPlayer.totalScore || 0) + (myPlayer.roundScore || 0);
  }

  /**
   * Retourne le statut du joueur actuel
   */
  getMyStatus(): string {
    if (!this.gameState || !this.gameState.players) {
      return 'PLAYING';
    }
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    return myPlayer?.status || 'PLAYING';
  }
}
