import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevealedCardsComponent } from '../revealed-cards/revealed-cards.component';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-players-board',
  standalone: true,
  imports: [CommonModule, RevealedCardsComponent, TranslateModule],
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
  
  // Tri des joueurs
  sortOrder: 'game' | 'current' | 'theoretical' = 'game';
  sortedPlayers: string[] = [];

  ngOnInit(): void {
    // Initialiser tous les joueurs comme déployés au chargement
    this.players.forEach(playerId => this.expandedPlayers.add(playerId));
    this.updateMaxRound();
    this.applySorting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Ajouter les nouveaux joueurs comme déployés
    if (changes['players'] && this.players) {
      this.players.forEach(playerId => this.expandedPlayers.add(playerId));
    }
    
    // Réappliquer le tri si les données changent
    if (changes['gameState'] || changes['players']) {
      this.applySorting();
    }

    // Scroller jusqu'au joueur actuel si c'est son tour
    if (changes['currentPlayerId'] && this.currentPlayerId) {
      setTimeout(() => this.scrollToCurrentPlayer(), 100);
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
      
      // Log pour debug : afficher le nombre de rounds dans l'historique du premier joueur
      if (this.gameState.players && this.gameState.players.length > 0) {
        const firstPlayer = this.gameState.players[0];
        console.log(`   Premier joueur a ${firstPlayer.rounds?.length || 0} rounds dans l'historique`);
      }
    }
  }

  /**
   * Vérifie si on affiche le round en cours (pas un round historique)
   */
  isCurrentRound(): boolean {
    return this.selectedRound === this.maxRound;
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
   * Récupère les données du round sélectionné pour un joueur
   * Si le round n'existe pas encore dans l'historique, retourne les données actuelles
   */
  private getPlayerRoundData(playerId: string): any {
    if (!this.gameState || !this.gameState.players) {
      console.log('⚠️ getPlayerRoundData: pas de gameState');
      return null;
    }
    
    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    if (!player) {
      console.log('⚠️ getPlayerRoundData: joueur non trouvé', playerId);
      return null;
    }

    console.log(`🔍 getPlayerRoundData pour ${player.username}:`);
    console.log(`   - selectedRound: ${this.selectedRound}`);
    console.log(`   - player.rounds length: ${player.rounds?.length || 0}`);
    
    // Si on regarde un round dans l'historique (pas le dernier)
    if (player.rounds && player.rounds.length > 0 && this.selectedRound <= player.rounds.length) {
      const roundData = player.rounds[this.selectedRound - 1];
      console.log(`   ✅ Retourne round historique ${this.selectedRound}:`, {
        roundScore: roundData.roundScore,
        totalScore: roundData.totalScore,
        theoreticalTotal: roundData.theoreticalTotal,
        cardsCount: roundData.hand?.length || 0
      });
      return roundData;
    }
    
    // Sinon, retourner les données actuelles du joueur
    console.log(`   ✅ Retourne données actuelles du joueur:`, {
      roundScore: player.roundScore,
      totalScore: player.totalScore,
      theoreticalTotal: player.theoreticalTotal,
      cardsCount: player.hand?.length || 0
    });
    return player;
  }

  /**
   * Retourne le score sécurisé (totalScore des rounds précédents)
   */
  getPlayerSafetyScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      console.log('⚠️ getPlayerSafetyScore: roundData manquant pour', playerId);
      return 0;
    }
    console.log(`📊 getPlayerSafetyScore pour ${playerId} round ${this.selectedRound}:`, roundData.totalScore);
    return roundData.totalScore || 0;
  }

  /**
   * Retourne le score total théorique (calculé côté backend)
   */
  getPlayerTotalScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      console.log(`⚠️ getPlayerTotalScore: roundData manquant pour ${playerId}`);
      return 0;
    }
    const theoreticalTotal = roundData.theoreticalTotal || 0;
    console.log(`📊 getPlayerTotalScore pour ${playerId} round ${this.selectedRound}: theoreticalTotal = ${theoreticalTotal}`);
    return theoreticalTotal;
  }

  /**
   * Retourne le score du round en cours (pour les cartes révélées)
   */
  getPlayerRoundScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      console.log('⚠️ getPlayerRoundScore: roundData manquant pour', playerId);
      return 0;
    }
    console.log(`📊 getPlayerRoundScore pour ${playerId} round ${this.selectedRound}:`, roundData.roundScore);
    return roundData.roundScore || 0;
  }

  getRevealedCards(playerId: string): any[] {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      console.log('⚠️ getRevealedCards: roundData manquant pour', playerId);
      return [];
    }
    console.log(`🎴 getRevealedCards pour ${playerId} round ${this.selectedRound}:`, roundData.hand?.length || 0, 'cartes');
    return roundData.hand || [];
  }

  /**
   * Retourne le statut du joueur pour le round sélectionné
   */
  getPlayerStatus(playerId: string): string {
    const roundData = this.getPlayerRoundData(playerId);
    if (!roundData) {
      return 'WAITING';
    }
    return roundData.status || 'PLAYING';
  }

  /**
   * Retourne la classe CSS en fonction du statut du joueur
   */
  getPlayerStatusClass(playerId: string): string {
    const status = this.getPlayerStatus(playerId);
    switch (status) {
      case 'PLAYING':
        return 'status-playing';
      case 'STOPPED':
        return 'status-stopped';
      case 'FORCED_STOP':
        return 'status-forced-stop';
      case 'ELIMINATED':
        return 'status-eliminated';
      default:
        return 'status-waiting';
    }
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

  /**
   * Change l'ordre de tri des joueurs
   */
  setSortOrder(order: 'game' | 'current' | 'theoretical'): void {
    this.sortOrder = order;
    this.applySorting();
  }

  /**
   * Applique le tri sur la liste des joueurs
   */
  applySorting(): void {
    if (!this.gameState?.players || this.players.length === 0) {
      this.sortedPlayers = [...this.players];
      return;
    }

    const playersCopy = [...this.players];

    switch (this.sortOrder) {
      case 'game':
        // Ordre de jeu : ordre original (index dans gameState.players)
        this.sortedPlayers = playersCopy;
        break;

      case 'current':
        // Classement actuel : par score sécurisé (vert) décroissant
        this.sortedPlayers = playersCopy.sort((a, b) => {
          const scoreA = this.getPlayerSafetyScore(a);
          const scoreB = this.getPlayerSafetyScore(b);
          return scoreB - scoreA; // Décroissant
        });
        break;

      case 'theoretical':
        // Classement théorique : par score théorique (bleu) décroissant
        this.sortedPlayers = playersCopy.sort((a, b) => {
          const safetyA = this.getPlayerTotalScore(a);
          const safetyB = this.getPlayerTotalScore(b);
          return safetyB - safetyA; // Décroissant
        });
        break;
    }
  }

  /**
   * Scroll automatiquement jusqu'au joueur actuel quand c'est son tour
   */
  scrollToCurrentPlayer(): void {
    if (!this.currentPlayerId) return;

    // Trouver l'élément HTML du joueur actuel
    const playerElements = document.querySelectorAll('.player-item');
    const currentPlayerIndex = this.sortedPlayers.indexOf(this.currentPlayerId);
    
    if (currentPlayerIndex >= 0 && currentPlayerIndex < playerElements.length) {
      const playerElement = playerElements[currentPlayerIndex] as HTMLElement;
      
      // Scroller avec un comportement smooth
      playerElement.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest', // Scroll seulement si nécessaire
        inline: 'nearest'
      });
      
      console.log(`🎯 Auto-scroll vers le joueur actuel: ${this.currentPlayerId} (index: ${currentPlayerIndex})`);
    }
  }
}
