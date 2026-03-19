import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-start-round-popup',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './start-round-popup.component.html',
  styleUrls: ['./start-round-popup.component.scss']
})
export class StartRoundPopupComponent {
  @Input() show: boolean = false;
  @Input() roundNumber: number = 1;
  @Input() playerUsername: string = '';
  @Input() isCurrentPlayer: boolean = false;
  @Input() roundEndReason: 'flip7' | 'eliminated' | 'stopped' | null = null;
  @Input() flip7PlayerName: string = '';
  
  @Output() onStartRound = new EventEmitter<void>();
  @Output() hideTemporarily = new EventEmitter<void>();

  isMinimized: boolean = false;

  /**
   * Démarre le round
   */
  startRound(): void {
    if (this.isCurrentPlayer) {
      this.onStartRound.emit();
    }
  }

  /**
   * Masque temporairement le popup
   */
  onHideTemporarily(): void {
    this.isMinimized = true;
    this.hideTemporarily.emit();
  }

  /**
   * Réaffiche le popup
   */
  onShowAgain(): void {
    this.isMinimized = false;
  }

  /**
   * Empêche la fermeture accidentelle
   */
  onOverlayClick(event: MouseEvent): void {
    event.stopPropagation();
  }

  /**
   * Retourne le message de fin de round selon la raison
   */
  getRoundEndMessage(): string {
    if (!this.roundEndReason || this.roundNumber === 1) {
      return '';
    }
    
    switch (this.roundEndReason) {
      case 'flip7':
        return this.flip7PlayerName 
          ? `🎯 ${this.flip7PlayerName} a réalisé un FLIP7 !` 
          : '🎯 Un joueur a réalisé un FLIP7 !';
      case 'eliminated':
        return '💀 Tous les joueurs ont été éliminés !';
      case 'stopped':
        return '🛑 Tous les joueurs se sont arrêtés !';
      default:
        return '';
    }
  }

  /**
   * Retourne le message approprié selon si c'est le joueur actuel ou non
   */
  getMessage(): string {
    if (this.isCurrentPlayer) {
      return `C'est à vous de commencer le round ${this.roundNumber} !`;
    } else {
      return `En attente de ${this.playerUsername} pour démarrer le round ${this.roundNumber}...`;
    }
  }

  /**
   * Retourne l'icône appropriée
   */
  getIcon(): string {
    return this.isCurrentPlayer ? '🎮' : '⏳';
  }
}
