import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

export interface PlayerRanking {
  username: string;
  score: number;
  rank: number;
  isWinner: boolean;
}

@Component({
  selector: 'app-game-over-popup',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './game-over-popup.component.html',
  styleUrl: './game-over-popup.component.scss'
})
export class GameOverPopupComponent {
  @Input() rankings: PlayerRanking[] = [];
  @Input() winnerName: string = '';
  @Input() isAdmin: boolean = false; // NOUVEAU : pour vérifier si l'utilisateur est admin

  @Output() playAgain = new EventEmitter<void>();
  @Output() leaveRoom = new EventEmitter<void>();
  @Output() hideTemporarily = new EventEmitter<void>();

  isMinimized: boolean = false;

  onPlayAgain() {
    this.playAgain.emit();
  }

  onLeaveRoom() {
    this.leaveRoom.emit();
  }

  onHideTemporarily() {
    this.isMinimized = true;
    this.hideTemporarily.emit();
  }

  onShowAgain() {
    this.isMinimized = false;
  }

  onOverlayClick(event: MouseEvent) {
    // Empêcher la fermeture accidentelle
    event.stopPropagation();
  }

  getRankIcon(rank: number): string {
    switch (rank) {
      case 1: return '🏆';
      case 2: return '🥈';
      case 3: return '🥉';
      default: return `${rank}°`;
    }
  }

  getRankClass(rank: number): string {
    switch (rank) {
      case 1: return 'gold';
      case 2: return 'silver';
      case 3: return 'bronze';
      default: return '';
    }
  }
}
