import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnyCard } from '../../models/game/card.model';
import { CardComponent } from '../card/card.component';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-player-hand',
  standalone: true,
  imports: [CommonModule, CardComponent, TranslateModule],
  templateUrl: './player-hand.component.html',
  styleUrl: './player-hand.component.scss'
})
export class PlayerHandComponent {
  @Input() cards: AnyCard[] = [];
  @Input() selectable: boolean = false;
  @Output() cardSelected = new EventEmitter<AnyCard>();

  selectedCard: AnyCard | null = null;

  onCardClick(card: AnyCard): void {
    if (this.selectable) {
      this.selectedCard = card;
      this.cardSelected.emit(card);
    }
  }

  isSelected(card: AnyCard): boolean {
    return this.selectedCard?.id === card.id;
  }
}
