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
  
  @Output() onStartRound = new EventEmitter<void>();

  /**
   * Démarre le round
   */
  startRound(): void {
    if (this.isCurrentPlayer) {
      this.onStartRound.emit();
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
