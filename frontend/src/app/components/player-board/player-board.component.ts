  showStatsPopup: boolean = false;
  eliminationProbability: number | null = null;

  ngOnChanges(): void {
    // Recalcule la proba à chaque changement d'état
    this.eliminationProbability = this.calculateEliminationProbability();
  }

  /**
   * Calcule la probabilité d'être éliminé par un double à la prochaine pioche
   * (Suppose que la main et le deck sont connus)
   */
  calculateEliminationProbability(): number | null {
    if (!this.gameState || !this.gameState.players || !this.gameState.deck) return null;
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!myPlayer || !myPlayer.hand) return null;
    // Compte les occurrences de chaque valeur dans la main
    const valueCounts: { [value: string]: number } = {};
    for (const card of myPlayer.hand) {
      const value = card.split('_')[0];
      valueCounts[value] = (valueCounts[value] || 0) + 1;
    }
    // Cherche les valeurs déjà doublées (on ne compte que les valeurs uniques)
    const hasDouble = Object.values(valueCounts).some(count => count >= 2);
    if (hasDouble) return 100; // Déjà éliminé
    // Compte le nombre de valeurs présentes une fois
    const singles = Object.keys(valueCounts).filter(v => valueCounts[v] === 1);
    // Parcourt le deck restant pour calculer la proba de piocher un double
    let total = 0, danger = 0;
    for (const card of this.gameState.deck) {
      const value = card.split('_')[0];
      total++;
      if (singles.includes(value)) danger++;
    }
    if (total === 0) return null;
    return (danger / total) * 100;
  }
import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
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
export class PlayerBoardComponent implements OnChanges {
  showStatsPopup: boolean = false;
  eliminationProbability: number | null = null;
  ngOnChanges(changes: SimpleChanges): void {
    // Recalcule la proba à chaque changement d'état
    this.eliminationProbability = this.calculateEliminationProbability();
  }

  /**
   * Calcule la probabilité d'être éliminé par un double à la prochaine pioche
   * (Suppose que la main et le deck sont connus)
   */
  calculateEliminationProbability(): number | null {
    if (!this.gameState || !this.gameState.players || !this.gameState.deck) return null;
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!myPlayer || !myPlayer.hand) return null;
    // Compte les occurrences de chaque valeur dans la main
    const valueCounts: { [value: string]: number } = {};
    for (const card of myPlayer.hand) {
      const value = card.split('_')[0];
      valueCounts[value] = (valueCounts[value] || 0) + 1;
    }
    // Cherche les valeurs déjà doublées (on ne compte que les valeurs uniques)
    const hasDouble = Object.values(valueCounts).some(count => count >= 2);
    if (hasDouble) return 100; // Déjà éliminé
    // Compte le nombre de valeurs présentes une fois
    const singles = Object.keys(valueCounts).filter(v => valueCounts[v] === 1);
    // Parcourt le deck restant pour calculer la proba de piocher un double
    let total = 0, danger = 0;
    for (const card of this.gameState.deck) {
      const value = card.split('_')[0];
      total++;
      if (singles.includes(value)) danger++;
    }
    if (total === 0) return null;
    return (danger / total) * 100;
  }
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
