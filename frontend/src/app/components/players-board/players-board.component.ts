import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevealedCardsComponent } from '../revealed-cards/revealed-cards.component';
import { TranslateModule } from '@ngx-translate/core';
import { TeamInfo } from '../../models/game/game.model';

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
  @Input() teamMode: boolean = false;
  @Input() teams: { [teamId: number]: TeamInfo } = {};

  expandedPlayers: Set<string> = new Set();
  selectedRound: number = 1;
  maxRound: number = 1;

  // Tri des joueurs
  sortOrder: 'game' | 'current' | 'theoretical' | 'history' = 'game';
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

    this.updateMaxRound();
    // Ajuster selectedRound si nécessaire
    if (this.selectedRound > this.maxRound) {
      this.selectedRound = this.maxRound;
    }
  }

  updateMaxRound(): void {
    if (this.gameState && this.gameState.roundNumber) {
      this.maxRound = this.gameState.roundNumber;
      this.selectedRound = this.maxRound;
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
    if (!this.gameState || !this.gameState.players) return null;

    const player = this.gameState.players.find((p: any) => p.userId === playerId);
    if (!player) return null;

    // Si on regarde un round dans l'historique (pas le dernier)
    if (player.rounds && player.rounds.length > 0 && this.selectedRound <= player.rounds.length) {
      return player.rounds[this.selectedRound - 1];
    }

    return player;
  }

  /**
   * Retourne le score sécurisé (totalScore des rounds précédents)
   */
  getPlayerSafetyScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    return roundData?.totalScore || 0;
  }

  /**
   * Retourne le score total théorique (calculé côté backend)
   */
  getPlayerTotalScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    return roundData?.theoreticalTotal || 0;
  }

  /**
   * Retourne le score du round en cours (pour les cartes révélées)
   */
  getPlayerRoundScore(playerId: string): number {
    const roundData = this.getPlayerRoundData(playerId);
    return roundData?.roundScore || 0;
  }

  getRevealedCards(playerId: string): any[] {
    const roundData = this.getPlayerRoundData(playerId);
    return roundData?.hand || [];
  }

  /**
   * Retourne le statut du joueur pour le round sélectionné
   */
  getPlayerStatus(playerId: string): string {
    const roundData = this.getPlayerRoundData(playerId);
    return roundData?.status || 'WAITING';
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
  setSortOrder(order: 'game' | 'current' | 'theoretical' | 'history'): void {
    this.sortOrder = order;
    if (order !== 'history') this.applySorting();
  }

  getEventLog(): any[] {
    return this.gameState?.eventLog ?? [];
  }

  getEventIcon(type: string): string {
    if (type === 'STOP') return '🛑';
    if (type === 'DRAW_THREE') return '➕3️⃣';
    if (type === 'LIFE') return '❤️';
    return '🎴';
  }

  formatEventTime(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleTimeString('fr-FR', { timeZone: 'Europe/Paris', hour: '2-digit', minute: '2-digit', second: '2-digit' });
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
        this.sortedPlayers = playersCopy;
        break;

      case 'current':
        this.sortedPlayers = playersCopy.sort((a, b) => {
          return this.getPlayerSafetyScore(b) - this.getPlayerSafetyScore(a);
        });
        break;

      case 'theoretical':
        this.sortedPlayers = playersCopy.sort((a, b) => {
          return this.getPlayerTotalScore(b) - this.getPlayerTotalScore(a);
        });
        break;
    }
  }

  // ─── Team helpers ───
  getTeamIds(): number[] {
    return Object.keys(this.teams).map(Number).sort();
  }

  getTeamPlayers(teamId: number): string[] {
    return this.teams[teamId]?.players ?? [];
  }

  getTeamScore(teamId: number): number {
    return this.teams[teamId]?.totalScore ?? 0;
  }

  getTeamTarget(teamId: number): number {
    return this.teams[teamId]?.targetScore ?? 0;
  }

  /**
   * Retourne les infos de cartes spéciales en attente impliquant ce joueur
   */
  getPendingCardInfo(playerId: string): { icon: string; label: string }[] {
    const pending = this.gameState?.pendingSpecialCards;
    if (!pending || pending.length === 0) return [];
    const infos: { icon: string; label: string }[] = [];
    for (const p of pending) {
      if (p.targetPlayerId === playerId) {
        if (p.specialType === 'STOP') {
          infos.push({ icon: '🛑', label: '' });
        } else if (p.specialType === 'DRAW_THREE') {
          const rem = p.remainingForcedDraws ?? 3;
          infos.push({ icon: '➕3', label: rem > 0 ? `${rem}` : '' });
        } else if (p.specialType === 'LIFE') {
          infos.push({ icon: '❤️', label: '' });
        }
      } else if (p.sourcePlayerId === playerId && !p.targetPlayerId && p.specialType === 'LIFE') {
        infos.push({ icon: '❤️', label: '?' });
      }
    }
    return infos;
  }

  scrollToCurrentPlayer(): void {
    if (!this.currentPlayerId) return;

    const playerElements = document.querySelectorAll('.player-item');
    const currentPlayerIndex = this.sortedPlayers.indexOf(this.currentPlayerId);

    if (currentPlayerIndex >= 0 && currentPlayerIndex < playerElements.length) {
      const playerElement = playerElements[currentPlayerIndex] as HTMLElement;
      playerElement.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
        inline: 'nearest'
      });
    }
  }
}
