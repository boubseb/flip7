/**
 * Types de cartes
 */
export enum CardType {
  NUMBER = 'NUMBER',
  OPERATOR = 'OPERATOR',
  SPECIAL = 'SPECIAL'
}

/**
 * Types d'opérateurs
 */
export enum OperatorType {
  PLUS_2 = 'PLUS_2',
  PLUS_4 = 'PLUS_4',
  PLUS_6 = 'PLUS_6',
  PLUS_8 = 'PLUS_8',
  PLUS_10 = 'PLUS_10',
  MULTIPLY_2 = 'MULTIPLY_2'
}

/**
 * Types de cartes spéciales
 */
export enum SpecialType {
  STOP = 'STOP',
  DRAW_THREE = 'DRAW_THREE',
  LIFE = 'LIFE'
}

/**
 * Interface de base pour une carte
 */
export interface Card {
  id: string;
  type: CardType;
  displayName: string;
  cancelled?: boolean; // Carte barrée (annulée par une carte Vie)
}

/**
 * Carte numérotée (0-12)
 */
export interface NumberCard extends Card {
  type: CardType.NUMBER;
  value: number;
}

/**
 * Carte opérateur (+2, +4, +6, +8, +10, ×2)
 */
export interface OperatorCard extends Card {
  type: CardType.OPERATOR;
  operatorType: OperatorType;
}

/**
 * Carte spéciale (Stop, +3, Vie)
 */
export interface SpecialCard extends Card {
  type: CardType.SPECIAL;
  specialType: SpecialType;
  used?: boolean; // Carte Vie utilisée (devient noire)
  pending?: boolean; // Carte Stop en attente d'assignation
  assignedToPlayerId?: string; // ID du joueur qui reçoit la carte Stop
}

/**
 * Union type pour toutes les cartes
 */
export type AnyCard = NumberCard | OperatorCard | SpecialCard;

/**
 * Helper pour vérifier le type de carte
 */
export class CardHelper {
  static isNumberCard(card: Card): card is NumberCard {
    return card.type === CardType.NUMBER;
  }

  static isOperatorCard(card: Card): card is OperatorCard {
    return card.type === CardType.OPERATOR;
  }

  static isSpecialCard(card: Card): card is SpecialCard {
    return card.type === CardType.SPECIAL;
  }

  static getCardColor(card: Card): string {
    if (this.isNumberCard(card)) {
      return '#3b82f6'; // Bleu pour les nombres
    } else if (this.isOperatorCard(card)) {
      return '#f97316'; // Orange pour tous les opérateurs (×2, +2, +4, +6, +8, +10)
    } else {
      // Couleurs différentes pour chaque carte spéciale
      const specialCard = card as SpecialCard;
      switch (specialCard.specialType) {
        case SpecialType.STOP:
          return '#ef4444'; // Rouge
        case SpecialType.DRAW_THREE:
          return '#f59e0b'; // Orange
        case SpecialType.LIFE:
          return '#dc2626'; // Rouge vif pour carte Vie
        default:
          return '#6b7280'; // Gris par défaut
      }
    }
  }

  static getCardIcon(card: Card): string {
    if (this.isSpecialCard(card)) {
      const specialCard = card as SpecialCard;
      switch (specialCard.specialType) {
        case SpecialType.STOP:
          return '🛑';
        case SpecialType.DRAW_THREE:
          return '➕3️⃣';
        case SpecialType.LIFE:
          return '❤️';
        default:
          return '🎴';
      }
    }
    return '';
  }
}
