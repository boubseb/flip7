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

  getMyHand(): any[] {
    if (!this.gameState || !this.gameState.players || !this.gameState.players[this.currentUserId]) {
      return [];
    }
    return this.gameState.players[this.currentUserId].hand || [];
  }

  playCard(card: any): void {
    this.onPlayCard.emit(card);
  }
}
