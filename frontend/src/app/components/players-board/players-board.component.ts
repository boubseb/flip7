import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevealedCardsComponent } from '../revealed-cards/revealed-cards.component';

@Component({
  selector: 'app-players-board',
  standalone: true,
  imports: [CommonModule, RevealedCardsComponent],
  templateUrl: './players-board.component.html',
  styleUrl: './players-board.component.scss'
})
export class PlayersBoardComponent implements OnInit, OnChanges {
  @Input() gameState: any;
  @Input() players: string[] = [];
  @Input() playerInfos: any[] = [];
  @Input() currentUserId: string = '';
  @Input() currentPlayerId: string = '';

  expandedPlayers: Set<string> = new Set();
  selectedRound: number = 1;
  maxRound: number = 1;

  ngOnInit(): void {
    // Initialiser tous les joueurs comme déployés au chargement
    this.players.forEach(playerId => this.expandedPlayers.add(playerId));
    this.updateMaxRound();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Ajouter les nouveaux joueurs comme déployés
    if (changes['players'] && this.players) {
      this.players.forEach(playerId => this.expandedPlayers.add(playerId));
    }
    
    // Logs de débogage
    if (changes['gameState']) {
      console.log('\n🔄 ========== PLAYERS-BOARD ngOnChanges ==========');
      console.log('📦 GameState complet:', this.gameState);
      console.log('   - roundNumber:', this.gameState?.roundNumber);
      console.log('   - players count:', this.gameState?.players?.length);
      if (this.gameState?.players?.length > 0) {
        console.log('\n👥 Tous les joueurs:');
        this.gameState.players.forEach((player: any, index: number) => {
          console.log(`   ${index + 1}. ${player.username} (${player.userId}):`, {
            roundScore: player.roundScore,
            totalScore: player.totalScore,
            theoreticalTotal: player.theoreticalTotal,
            status: player.status,
            handSize: player.handSize
          });
        });
      }
      console.log('==================================================\n');
    }
    
    this.updateMaxRound();
    // Ajuster selectedRound si nécessaire
    if (this.selectedRound > this.maxRound) {
      this.selectedRound = this.maxRound;
    }
  }

  updateMaxRound(): void {
    if (this.gameState && this.gameState.roundNumber) {
      this.maxRound = this.gameState.roundNumber;
      this.selectedRound = this.maxRound; // Par défaut sur le round actuel
      console.log(`📅 updateMaxRound: maxRound = ${this.maxRound}, selectedRound = ${this.selectedRound}`);
    }
  }

  previousRound(): void {
    if (this.selectedRound > 1) {
      this.selectedRound--;
    }
  }

  nextRound(): void {
    if (this.selectedRound < this.maxRound) {
      this.selectedRound++;
    }
  }

  isExpanded(playerId: string): boolean {
    return this.expandedPlayers.has(playerId);
  }

  togglePlayer(playerId: string): void {
    if (this.expandedPlayers.has(playerId)) {
      this.expandedPlayers.delete(playerId);
    } else {
      this.expandedPlayers.add(playerId);
    }
  }

  getPlayerPseudo(playerId: string): string {
    if (!this.playerInfos) {
      return playerId.substring(0, 8) + '...';
    }
    const playerInfo = this.playerInfos.find(p => p.id === playerId);
    return playerInfo ? playerInfo.pseudo : playerId.substring(0, 8) + '...';
  }

  /**
   * Retourne le score sécurisé (totalScore des rounds précédents)
   */
  getPlayerSafetyScore(playerId: string): number {
    if (!this.gameState || !this.gameState.players) {
      console.log('⚠️ getPlayerSafetyScore: gameState ou players manquant', this.gameState);
      return 0;
    }
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    console.log(`📊 getPlayerSafetyScore pour ${playerId}:`, player?.totalScore, 'Player:', player);
    return player?.totalScore || 0;
  }

  /**
   * Retourne le score total théorique (calculé côté backend)
   */
  getPlayerTotalScore(playerId: string): number {
    if (!this.gameState || !this.gameState.players) {
      console.log('⚠️ getPlayerTotalScore: gameState ou players manquant');
      return 0;
    }
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    if (!player) {
      console.log(`⚠️ getPlayerTotalScore: Joueur ${playerId} non trouvé`);
      return 0;
    }
    const theoreticalTotal = player.theoreticalTotal || 0;
    console.log(`📊 getPlayerTotalScore pour ${playerId}: theoreticalTotal = ${theoreticalTotal}`);
    return theoreticalTotal;
  }

  /**
   * Retourne le score du round en cours (pour les cartes révélées)
   */
  getPlayerRoundScore(playerId: string): number {
    if (!this.gameState || !this.gameState.players) {
      console.log('⚠️ getPlayerRoundScore: gameState ou players manquant');
      return 0;
    }
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    console.log(`📊 getPlayerRoundScore pour ${playerId}:`, player?.roundScore, 'Player:', player);
    return player?.roundScore || 0;
  }

  getRevealedCards(playerId: string): any[] {
    if (!this.gameState || !this.gameState.players) {
      console.log('⚠️ getRevealedCards: gameState ou players manquant');
      return [];
    }
    // gameState.players est un tableau
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    console.log(`🎴 getRevealedCards pour ${playerId}:`, player?.hand?.length || 0, 'cartes');
    return player?.hand || [];
  }

  getSuitSymbol(suit: string): string {
    const suitMap: { [key: string]: string } = {
      'HEARTS': '♥',
      'DIAMONDS': '♦',
      'CLUBS': '♣',
      'SPADES': '♠'
    };
    return suitMap[suit] || suit;
  }
}
