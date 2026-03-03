import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerStatus } from '../../models/game/game.model';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-player-selector',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './player-selector.component.html',
  styleUrls: ['./player-selector.component.scss']
})
export class PlayerSelectorComponent implements OnChanges {
  @Input() title: string = 'Choisissez un joueur';
  @Input() description: string = 'Sélectionnez le joueur qui recevra cette carte';
  @Input() players: any[] = [];
  @Input() currentUserId: string = '';
  @Input() cardType: 'STOP' | 'DRAW_THREE' = 'STOP';
  @Input() show: boolean = false;
  @Input() statisticsEnabled: boolean = false;
  @Input() remainingCards: number = 0;
  
  @Output() onPlayerSelected = new EventEmitter<string>();
  @Output() onCancel = new EventEmitter<void>();

  // Pourcentage cubé pour +3
  getCubedProbability(prob: number | null): string {
    if (prob === null || prob === undefined) return 'N/A';
    const cubed = Math.pow(prob / 100, 3) * 100;
    return '≈ ' + cubed.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 }) + ' %';
  }

  // Calcule la proba d'élimination pour un joueur donné
  getPlayerEliminationProb(player: any): number | null {
    if (!player.hand) return null;
    const handValues = player.hand.filter((card: any) => card.cardType === 'NUMBER' && typeof card.value === 'number').map((card: any) => card.value);
    if (handValues.length === 0) return null;
    const valueCounts: { [value: number]: number } = {};
    for (const v of handValues) valueCounts[v] = (valueCounts[v] || 0) + 1;
    if (Object.values(valueCounts).some(count => count >= 2)) return 100;
    const initialDeck: { [value: number]: number } = {};
    for (let v = 0; v <= 12; v++) initialDeck[v] = (v === 0 || v === 1) ? 1 : v;
    const allHands = this.players.flatMap((pl: any) => Array.isArray(pl.hand) ? pl.hand : []);
    const usedCount: { [value: number]: number } = {};
    allHands.forEach((card: any) => {
      if (card.cardType === 'NUMBER' && typeof card.value === 'number') {
        usedCount[card.value] = (usedCount[card.value] || 0) + 1;
      }
    });
    const totalRemaining = this.remainingCards || 0;
    if (totalRemaining === 0) return null;
    let eliminationNumerator = 0;
    for (const v of new Set(handValues)) {
      const value = Number(v);
      const remaining = Math.max(0, initialDeck[value] - (usedCount[value] || 0));
      eliminationNumerator += remaining;
    }
    const prob = eliminationNumerator / totalRemaining;
    return Math.min((1 - prob) * 100, 100);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['show'] && changes['show'].currentValue === true) {
      console.log('🎯 Player Selector - Players:', this.players);
      console.log('🎯 Player Selector - Card Type:', this.cardType);
      console.log('🎯 Player Selector - Eligible count:', this.getEligiblePlayersCount());
      this.players.forEach(p => {
        console.log(`   - ${p.username}: status=${p.status}, eligible=${this.isPlayerEligible(p)}`);
      });
    }
  }

  /**
   * Vérifie si un joueur est éligible
   * - Carte STOP: Joueurs en jeu uniquement (pas stoppés, pas éliminés)
   * - Carte DRAW_THREE: Joueurs en jeu uniquement (pas stoppés, pas éliminés)
   */
  isPlayerEligible(player: any): boolean {
    // Toujours exclure les éliminés
    if (player.status === PlayerStatus.ELIMINATED) {
      return false;
    }
    
    // Exclure les joueurs stoppés pour STOP et +3
    if (player.status === PlayerStatus.STOPPED || 
        player.status === PlayerStatus.FORCED_STOP ||
        player.status === PlayerStatus.FLIP7_STOP) {
      return false;
    }
    
    // Seuls les joueurs PLAYING sont éligibles
    return player.status === PlayerStatus.PLAYING;
  }

  /**
   * Retourne le nombre de joueurs éligibles
   */
  getEligiblePlayersCount(): number {
    return this.players.filter(p => this.isPlayerEligible(p)).length;
  }

  /**
   * Sélectionne un joueur
   */
  selectPlayer(playerId: string): void {
    const player = this.players.find(p => p.userId === playerId);
    if (player && this.isPlayerEligible(player)) {
      this.onPlayerSelected.emit(playerId);
    }
  }

  /**
   * Annule la sélection
   */
  cancel(): void {
    this.onCancel.emit();
  }

  /**
   * Retourne la clé de traduction du statut
   */
  getStatusKey(status: string): string {
    switch (status) {
      case PlayerStatus.PLAYING:
        return 'game.status.playing';
      case PlayerStatus.STOPPED:
      case PlayerStatus.FORCED_STOP:
      case PlayerStatus.FLIP7_STOP:
        return 'game.status.stopped';
      case PlayerStatus.ELIMINATED:
        return 'game.status.eliminated';
      case PlayerStatus.WAITING:
        return 'game.status.waiting';
      default:
        // Si le statut n'est pas reconnu, retourner la clé "playing" par défaut
        console.warn('Unknown player status:', status);
        return 'game.status.playing';
    }
  }

  /**
   * Retourne l'icône de la carte
   */
  getCardIcon(): string {
    return this.cardType === 'STOP' ? '🛑' : '➕3️⃣';
  }

  /**
   * Retourne la couleur de la carte
   */
  getCardColor(): string {
    return this.cardType === 'STOP' ? '#ef4444' : '#f97316';
  }
}
