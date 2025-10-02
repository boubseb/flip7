import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnyCard, CardHelper } from '../../models/game/card.model';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card.component.html',
  styleUrl: './card.component.scss'
})
export class CardComponent {
  @Input() card!: AnyCard;
  @Input() selectable: boolean = false;
  @Input() selected: boolean = false;
  @Output() cardClick = new EventEmitter<AnyCard>();

  getCardColor(): string {
    return CardHelper.getCardColor(this.card);
  }

  getCardIcon(): string {
    return CardHelper.getCardIcon(this.card);
  }

  onCardClick(): void {
    if (this.selectable) {
      this.cardClick.emit(this.card);
    }
  }
}
