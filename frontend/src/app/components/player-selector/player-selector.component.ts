import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerStatus } from '../../models/game/game.model';

@Component({
  selector: 'app-player-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './player-selector.component.html',
  styleUrls: ['./player-selector.component.scss']
})
export class PlayerSelectorComponent {
  @Input() title: string = 'Choisissez un joueur';
  @Input() description: string = 'Sélectionnez le joueur qui recevra cette carte';
  @Input() players: any[] = [];
  @Input() currentUserId: string = '';
  @Input() cardType: 'STOP' | 'DRAW_THREE' = 'STOP';
  @Input() show: boolean = false;
  
  @Output() onPlayerSelected = new EventEmitter<string>();
  @Output() onCancel = new EventEmitter<void>();

  /**
   * Vérifie si un joueur est éligible (statut PLAYING uniquement)
   */
  isPlayerEligible(player: any): boolean {
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
   * Retourne le libellé du statut
   */
  getStatusLabel(status: string): string {
    switch (status) {
      case PlayerStatus.PLAYING:
        return 'En jeu';
      case PlayerStatus.STOPPED:
        return 'Arrêté';
      case PlayerStatus.FORCED_STOP:
        return 'Stoppé (carte)';
      case PlayerStatus.ELIMINATED:
        return 'Éliminé';
      case PlayerStatus.WAITING:
        return 'En attente';
      default:
        return status;
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
