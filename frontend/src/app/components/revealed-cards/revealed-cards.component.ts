import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-revealed-cards',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './revealed-cards.component.html',
  styleUrls: ['./revealed-cards.component.scss']
})
export class RevealedCardsComponent {
  @Input() cards: any[] = [];
  @Input() score: number = 0;

  /**
   * Obtient la valeur d'affichage de la carte
   */
  getCardDisplay(card: any): string {
    if (card.cardType === 'NUMBER') {
      return card.value?.toString() || '?';
    } else if (card.cardType === 'OPERATOR') {
      return this.formatOperator(card.operator);
    } else if (card.cardType === 'SPECIAL') {
      return this.formatSpecialType(card.specialType);
    }
    return '?';
  }

  /**
   * Formate l'affichage d'un opérateur
   */
  private formatOperator(operator: string): string {
    if (!operator) return '?';
    // PLUS_2 -> +2, MULTIPLY_2 -> ×2, etc.
    if (operator.startsWith('PLUS_')) {
      return '+' + operator.substring(5);
    } else if (operator.startsWith('MULTIPLY_')) {
      return '×' + operator.substring(9);
    }
    return operator;
  }

  /**
   * Formate l'affichage d'une carte spéciale
   */
  private formatSpecialType(specialType: string): string {
    if (!specialType) return '⭐';
    const typeMap: { [key: string]: string } = {
      'STOP': 'STOP',
      'DRAW_THREE': '+3',
      'LIFE': '❤️'
    };
    return typeMap[specialType] || specialType;
  }

  /**
   * Obtient la classe CSS pour le type de carte spéciale
   */
  getCardTypeClass(card: any): string {
    if (card.special || card.cardType === 'SPECIAL') {
      return `card-special card-${card.specialType?.toLowerCase() || 'unknown'}`;
    }
    return '';
  }
}
