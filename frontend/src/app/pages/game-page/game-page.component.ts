import { Component, OnInit, OnDestroy, AfterViewInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RoomService } from '../../services/room/room.service';
import { WebSocketService } from '../../services/websocket/websocket.service';
import { GameService } from '../../services/game/game.service';
import { StatisticsService } from '../../services/statistics/statistics.service';
import { Room, RoomStatus } from '../../models/room/room.model';
import { Subscription } from 'rxjs';
import { PlayersBoardComponent } from '../../components/players-board/players-board.component';
import { PlayerSelectorComponent } from '../../components/player-selector/player-selector.component';
import { GameOverPopupComponent, PlayerRanking } from '../../components/game-over-popup/game-over-popup.component';
import { StatsPopupComponent } from './components/stats-popup/stats-popup.component';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-game-page',
  imports: [CommonModule, FormsModule, PlayersBoardComponent, PlayerSelectorComponent, GameOverPopupComponent, StatsPopupComponent, TranslateModule],
  templateUrl: './game-page.component.html',
  styleUrl: './game-page.component.scss'
})
export class GamePageComponent implements OnInit, AfterViewInit, OnDestroy {
  // Popup statistiques
  showStatsPopup: boolean = false;

  /**
   * Calcule la probabilité d'élimination : somme des probabilités de piocher une carte numérotée déjà présente dans la main.
   * Ne dépend que de la main du joueur et des mains visibles, en utilisant la composition initiale du deck (1x0, 1x1, 2x2, ..., 12x12).
   * Les cartes spéciales sont ignorées.
   */
  calculateEliminationProbability(): number | null {
    if (!this.gameState || !this.gameState.players) {
      return null;
    }
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!myPlayer || !myPlayer.hand) {
      return null;
    }
    // Cartes numérotées dans la main du joueur (exclure les barrées : carte Vie utilisée barre le doublon)
    const handValues = myPlayer.hand
      .filter((card: any) => card.cardType === 'NUMBER' && typeof card.value === 'number' && !card.cancelled)
      .map((card: any) => card.value);
    if (handValues.length === 0) {
      return null;
    }
    // Si déjà un doublon (ne devrait pas arriver sauf bug), proba = 0
    const valueCounts: { [value: number]: number } = {};
    for (const v of handValues) valueCounts[v] = (valueCounts[v] || 0) + 1;
    if (Object.values(valueCounts).some(count => count >= 2)) {
      return 0; // 0% de survie
    }
    // Deck initial : 1x0, 1x1, 2x2, ..., 12x12
    const initialDeck: { [value: number]: number } = {};
    for (let v = 0; v <= 12; v++) {
      initialDeck[v] = (v === 0 || v === 1) ? 1 : v;
    }
    // Compte les cartes numérotées visibles dans toutes les mains
    // Compte TOUTES les cartes NUMBER visibles (y compris les barrées) : elles sont
    // retirées du deck même si elles ne créent plus de doublon actif.
    const allHands = this.gameState.players.flatMap((p: any) => Array.isArray(p.hand) ? p.hand : []);
    const usedCount: { [value: number]: number } = {};
    allHands.forEach((card: any) => {
      if (card.cardType === 'NUMBER' && typeof card.value === 'number') {
        usedCount[card.value] = (usedCount[card.value] || 0) + 1;
      }
    });
    // Nombre de cartes dangereuses restantes dans le deck (celles qui formeraient un doublon)
    let dangerousCards = 0;
    for (const v of new Set(handValues)) {
      const value = Number(v);
      dangerousCards += Math.max(0, initialDeck[value] - (usedCount[value] || 0));
    }
    let totalRemaining = typeof this.gameState.remainingCards === 'number' ? this.gameState.remainingCards : 0;
    if (totalRemaining === 0) {
      return null;
    }
    // Nombre de tirages forcés (1 par défaut, 3 si +3 en cours)
    const pendingDrawThree = (this.gameState.pendingSpecialCards ?? []).find(
      (card: any) => card.specialType === 'DRAW_THREE' && card.targetPlayerId === this.currentUserId
    );
    const forcedDraws = pendingDrawThree ? (pendingDrawThree.remainingForcedDraws ?? 1) : 1;

    // P(survie) = produit sur chaque tirage de (1 - dangereuses_restantes / total_restant)
    // On suppose le pire cas : une carte non-dangereuse piochée ne réduit pas dangerousCards
    let survivalProb = 1;
    for (let i = 0; i < forcedDraws; i++) {
      if (totalRemaining <= 0) break;
      survivalProb *= (1 - dangerousCards / totalRemaining);
      totalRemaining--;
    }
    return Math.min(survivalProb * 100, 100);
  }

  /**
   * Retourne true si le joueur courant a une carte vie dans sa main
   */
  hasLifeCard(): boolean {
    if (!this.gameState?.players || !this.currentUserId) return false;
    const me = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!me || !Array.isArray(me.hand)) return false;
    // Les cartes spéciales ont cardType === 'SPECIAL' et specialType === 'LIFE'
    return me.hand.some((card: any) =>
      card.cardType === 'SPECIAL' && card.specialType === 'LIFE' && !card.used
    );
  }

  /**
   * Retourne la probabilité de survie en pourcentage (0-100), en tenant compte
   * de la carte vie. Toujours un nombre (jamais null) pour l'affichage.
   */
  getSurvivalProbability(): number {
    if (this.hasLifeCard()) return 100;
    const survival = this.calculateEliminationProbability();
    return survival !== null ? survival : 100;
  }
  // Room ID from URL
  roomId: string = '';
  
  // Current room data
  currentRoom: Room | null = null;
  currentUserId: string = '';
  isAdmin: boolean = false;
  isMyTurn: boolean = false;
  currentPlayerId: string = '';
  
  // Game State
  gameState: any = null;
  
  // WebSocket
  wsConnected: boolean = false;
  
  // Error handling
  errorMessage: string = '';
  private errorTimeout: any = null;
  
  // Success messages
  successMessage: string = '';
  private successTimeout: any = null;
  
  // Flip7 Celebration
  showFlip7Celebration: boolean = false;
  private flip7Timeout: any = null;
  
  // Stop Card Selection
  showStopCardModal: boolean = false;
  stopCardToAssign: any = null;

  // DrawThree Card Selection
  showDrawThreeModal: boolean = false;
  drawThreeCardToAssign: any = null;

  // Life Card Selection (team mode)
  showLifeCardModal: boolean = false;
  lifeCardToAssign: any = null;

  // Minimized card selector (user can minimize to see the board)
  selectorMinimized: boolean = false;

  get hasPendingModal(): boolean {
    return this.showStopCardModal || this.showDrawThreeModal || this.showLifeCardModal;
  }

  // Start Round Popup
  showStartRoundPopup: boolean = false;

  // Game Over Popup
  showGameOverPopup: boolean = false;
  gameOverRankings: PlayerRanking[] = [];
  gameOverWinner: string = '';
  gameOverTeamMode: boolean = false;
  gameOverWinningTeamId: number = 0;
  gameOverTeams: any = {};
  gameOverPlayerNames: { [userId: string]: string } = {};
  
  private subscriptions: Subscription[] = [];

  constructor(
    private roomService: RoomService,
    private wsService: WebSocketService,
    private gameService: GameService,
    private statisticsService: StatisticsService,
    private router: Router,
    private route: ActivatedRoute,
    private translate: TranslateService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Only run in browser (not during SSR)
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Get current user ID from token
    this.currentUserId = localStorage.getItem('access_token') || '';
    
    if (!this.currentUserId) {
      this.router.navigate(['/login']);
      return;
    }

    // Get room ID from route params and listen for changes
    this.subscriptions.push(
      this.route.params.subscribe(params => {
        const newRoomId = params['roomId'];
        
        if (!newRoomId) {
          this.router.navigate(['/room']);
          return;
        }
        
        // Si le roomId change vraiment (pas juste un refresh)
        if (newRoomId !== this.roomId) {
          // Désabonner de l'ancienne room si différente
          if (this.roomId && this.roomId !== newRoomId) {
            this.wsService.unsubscribeFromRoom(this.roomId);
          }
          
          this.roomId = newRoomId;
          this.gameState = null;
          this.currentRoom = null;
          
          this.loadRoom();
        } else {
        }
      })
    );
    
    // Connect to WebSocket
    this.wsService.connect();
    
    // Subscribe to connection status ONCE
    this.subscriptions.push(
      this.wsService.connectionStatus$.subscribe(connected => {
        const wasConnected = this.wsConnected;
        this.wsConnected = connected;
        
        if (connected && !wasConnected) {
          // The onConnect handler clears the subscriptions map, so we always
          // need to re-subscribe (handles both first connection and reconnection).
          if (this.roomId) {
            this.wsService.subscribeToRoom(this.roomId);
            // Re-fetch game state to recover any updates missed during disconnection
            if (this.gameState) {
              this.gameService.getGameState(this.roomId).subscribe({
                next: (state) => {
                  this.gameState = state;
                  if (state.players && state.currentPlayerIndex >= 0) {
                    const cp = state.players[state.currentPlayerIndex];
                    this.currentPlayerId = cp.userId;
                    this.isMyTurn = this.currentPlayerId === this.currentUserId;
                  }
                },
                error: () => {}
              });
            }
          }
        } else if (!connected) {
        }
      })
    );
    
    // Subscribe to room updates (uniquement pour les changements de room, pas de gameState)
    this.subscriptions.push(
      this.wsService.roomUpdates$.subscribe(room => {
        this.currentRoom = room;
        // Ne pas appeler updateRoomState() ici car ça peut causer des refresh
      })
    );
    
    // Subscribe to game starts
    this.subscriptions.push(
      this.wsService.gameStarts$.subscribe(room => {
        this.currentRoom = room;
        
        // Récupérer immédiatement l'état du jeu pour avoir les cartes initiales
        this.gameService.getGameState(this.roomId).subscribe({
          next: (gameState) => {
            this.gameState = gameState;
            if (gameState.players && gameState.players.length > 0) {
              gameState.players.forEach((p: any) => {
              });
              if (gameState.currentPlayerIndex >= 0) {
                const currentPlayer = gameState.players[gameState.currentPlayerIndex];
                this.currentPlayerId = currentPlayer.userId;
                this.isMyTurn = this.currentPlayerId === this.currentUserId;
              }
            }
          },
          error: () => {}
        });
      })
    );

    // Subscribe to game over
    this.subscriptions.push(
      this.wsService.gameOver$.subscribe((data: any) => {
        this.prepareGameOverData(data);
        // Notifier le composant statistiques pour qu'il se recharge
        this.statisticsService.notifyGameEnded();
      })
    );

    // Subscribe to game restarted
    this.subscriptions.push(
      this.wsService.gameRestarted$.subscribe(data => {
        this.showGameOverPopup = false;

        // Pour les non-admins : rejoindre automatiquement la room remise à zéro
        // puis naviguer vers la salle d'attente.
        if (!this.isAdmin) {
          this.roomService.rejoinAfterRestart(this.roomId).subscribe({
            next: () => {
              this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
            },
            error: () => {
              // Si le rejoin échoue, naviguer quand même
              this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
            }
          });
        } else {
          // L'admin a déjà navigué via la réponse REST, cette branche ne devrait pas
          // arriver mais en sécurité on navigue quand même
          this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
        }
      })
    );
    
    // Subscribe to turn updates (on ignore, on utilisera gameStateUpdates$ à la place)
    this.subscriptions.push(
      this.wsService.turnUpdates$.subscribe(update => {
        // Ne rien faire, on attend le gameStateUpdate
      })
    );
    
    // Subscribe to game state updates
    this.subscriptions.push(
      this.wsService.gameStateUpdates$.subscribe(response => {
        if (response.players && response.players.length > 0) {
          response.players.forEach((p: any) => {
          });
        }
        
        this.gameState = response; // Le DTO complet contient players, pas response.gameState
        
        // Déterminer le joueur actuel à partir de l'index
        if (response.players && response.players.length > 0 && response.currentPlayerIndex >= 0) {
          const currentPlayer = response.players[response.currentPlayerIndex];
          const previousPlayerId = this.currentPlayerId;
          this.currentPlayerId = currentPlayer.userId;
          this.isMyTurn = this.currentPlayerId === this.currentUserId;
          
          // (no turn-change notification)
        }

        // Détecter l'état WAITING_NEXT_ROUND
        // On ne montre PLUS le popup, le bouton est directement dans le game footer
        if (response.gameState === 'WAITING_NEXT_ROUND') {
          if (this.isMyTurn) {
          } else {
          }
          // Ne plus afficher le popup
          this.showStartRoundPopup = false;
        } else {
          this.showStartRoundPopup = false;
        }

        // Détecter l'état GAME_OVER pour afficher le popup de fin de partie
        if (response.gameState === 'GAME_OVER') {
          this.prepareGameOverData(response);
        }

        // Détecter si le joueur actuel a des cartes spéciales en attente d'assignation
        if (response.pendingSpecialCards && response.pendingSpecialCards.length > 0) {
          
          // Chercher une carte STOP en attente pour le joueur actuel
          const pendingStopCard = response.pendingSpecialCards.find((pending: any) => 
            pending.sourcePlayerId === this.currentUserId &&
            !pending.targetPlayerId &&
            pending.specialType === 'STOP'
          );
          
          if (pendingStopCard && !this.showStopCardModal) {
            // Créer un objet carte compatible avec showStopCardSelection
            const card = { id: pendingStopCard.cardId, specialType: pendingStopCard.specialType };
            this.showStopCardSelection(card);
          }

          // Chercher une carte DRAW_THREE en attente pour le joueur actuel
          const pendingDrawThreeCard = response.pendingSpecialCards.find((pending: any) => 
            pending.sourcePlayerId === this.currentUserId &&
            !pending.targetPlayerId &&
            pending.specialType === 'DRAW_THREE'
          );
          
          
          if (pendingDrawThreeCard && !this.showDrawThreeModal) {
            // Créer un objet carte compatible avec showDrawThreeCardSelection
            const card = { id: pendingDrawThreeCard.cardId, specialType: pendingDrawThreeCard.specialType };
            this.showDrawThreeCardSelection(card);
          }

          // Chercher une carte LIFE en attente pour le joueur actuel (mode équipe uniquement)
          if (this.gameState?.teamMode) {
            const pendingLifeCard = response.pendingSpecialCards.find((pending: any) =>
              pending.sourcePlayerId === this.currentUserId &&
              !pending.targetPlayerId &&
              pending.specialType === 'LIFE'
            );
            if (pendingLifeCard && !this.showLifeCardModal) {
              this.lifeCardToAssign = { id: pendingLifeCard.cardId, specialType: 'LIFE' };
              this.selectorMinimized = false;
              this.showLifeCardModal = true;
            }
          }
        }
      })
    );
  }

  ngAfterViewInit(): void {
    // Force un recalcul du layout sur iOS Safari où le viewport peut être mal calculé au chargement
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => window.dispatchEvent(new Event('resize')), 100);
      setTimeout(() => window.dispatchEvent(new Event('resize')), 500);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.roomId) {
      this.wsService.unsubscribeFromRoom(this.roomId);
    }
    this.wsService.disconnect();
  }

  loadRoom(): void {
    this.roomService.getRoom(this.roomId).subscribe({
      next: (room) => {
        
        // Check if user is part of the room
        if (!room.players.includes(this.currentUserId)) {
          this.showError('game.errors.notInRoom');
          setTimeout(() => this.router.navigate(['/room']), 2000);
          return;
        }

        this.currentRoom = room;
        this.updateRoomState();
        
        // Redirect to room-page if game not started yet
        if (room.status !== RoomStatus.IN_GAME && room.status !== RoomStatus.FINISHED) {
          this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
          return;
        }
        

        // If the WS is already connected (e.g. the connection resolved before this
        // HTTP response came back), ensure the room subscriptions are registered now.
        // Without this guard a page-refresh where WS connects before loadRoom()
        // completes would skip subscribeToRoom because connectionStatus$ fires with
        // this.roomId not yet confirmed as a valid in-game room.
        if (this.wsService.isConnected()) {
          this.wsService.subscribeToRoom(this.roomId);
        }

        // Si le jeu est déjà en cours, récupérer l'état actuel pour reconnexion (une seule fois)
        if (!this.gameState) {
          this.gameService.getGameState(this.roomId).subscribe({
            next: (gameState) => {
              this.gameState = gameState;
              
              if (gameState.players && gameState.players.length > 0) {
                gameState.players.forEach((p: any) => {
                });
                
                if (gameState.currentPlayerIndex >= 0) {
                  const currentPlayer = gameState.players[gameState.currentPlayerIndex];
                  this.currentPlayerId = currentPlayer.userId;
                  this.isMyTurn = this.currentPlayerId === this.currentUserId;
                }
                
                // Vérifier si on a des cartes pending à assigner (Stop/DrawThree)
                // Utiliser la queue pendingSpecialCards au lieu de chercher dans la main
                if (gameState.pendingSpecialCards && gameState.pendingSpecialCards.length > 0) {
                  const pendingStopCard = gameState.pendingSpecialCards.find((pending: any) => 
                    pending.sourcePlayerId === this.currentUserId &&
                    !pending.targetPlayerId &&
                    pending.specialType === 'STOP'
                  );
                  
                  if (pendingStopCard && !this.showStopCardModal) {
                    const card = { id: pendingStopCard.cardId, specialType: pendingStopCard.specialType };
                    this.showStopCardSelection(card);
                  }

                  const pendingDrawThreeCard = gameState.pendingSpecialCards.find((pending: any) => 
                    pending.sourcePlayerId === this.currentUserId &&
                    !pending.targetPlayerId &&
                    pending.specialType === 'DRAW_THREE'
                  );
                  
                  if (pendingDrawThreeCard && !this.showDrawThreeModal) {
                    const card = { id: pendingDrawThreeCard.cardId, specialType: pendingDrawThreeCard.specialType };
                    this.showDrawThreeCardSelection(card);
                  }

                  // Chercher une carte LIFE en attente (mode équipe uniquement)
                  if (this.gameState?.teamMode) {
                    const pendingLifeCard = gameState.pendingSpecialCards.find((pending: any) =>
                      pending.sourcePlayerId === this.currentUserId &&
                      !pending.targetPlayerId &&
                      pending.specialType === 'LIFE'
                    );
                    if (pendingLifeCard && !this.showLifeCardModal) {
                      this.lifeCardToAssign = { id: pendingLifeCard.cardId, specialType: 'LIFE' };
                      this.showLifeCardModal = true;
                    }
                  }
                }

                // Vérifier si on est en état WAITING_NEXT_ROUND
                if (gameState.gameState === 'WAITING_NEXT_ROUND') {
                  // Le bouton est maintenant dans le game footer, pas besoin de popup
                  this.showStartRoundPopup = false;
                }
              }
            },
            error: (err) => {
              
              // Si erreur 400, c'est que la partie n'existe plus (backend redémarré ou partie terminée)
              if (err.status === 400) {
                this.showError('game.errors.gameNotFoundAfterRestart');
                setTimeout(() => this.router.navigate(['/room']), 3000);
              } else {
                this.showError('game.errors.cannotLoadGameState');
              }
            }
          });
        } else {
        }
      },
      error: (error) => {
        this.showError('game.errors.roomNotFound');
        setTimeout(() => this.router.navigate(['/room']), 2000);
      }
    });
  }

  onPlayerAction(action: any): void {
    
    if (!this.roomId) {
      return;
    }
    
    // Temporairement: permettre l'action même si ce n'est pas notre tour (pour debug)
    if (!this.isMyTurn) {
    }
    
    if (action.action === 'REVEAL') {
      // Joueur clique sur "Hit" - Tirer une carte
      this.gameService.drawCard(this.roomId).subscribe({
        next: (result) => {
          
          if (result.needsStopAssignment && result.card) {
            // Carte Stop ou DrawThree piochée - afficher la sélection de joueur
            const cardData = result.card as any;
            
            // Le backend retourne "cardType" au lieu de "type"
            if (cardData.cardType === 'SPECIAL' || cardData.type === 'SPECIAL') {
              if (cardData.specialType === 'STOP') {
                this.showStopCardSelection(result.card);
              } else if (cardData.specialType === 'DRAW_THREE') {
                this.showDrawThreeCardSelection(result.card);
              } else {
              }
            } else {
            }
          } else if (result.eliminated) {
            // Message supprimé - déjà affiché dans le footer avec getPlayerStatusMessage()
          } else if (result.lifeUsed) {
          } else if (result.roundEnded) {
            this.showFlip7CelebrationPopup();
          }
          
          // L'état sera mis à jour via WebSocket
        },
        error: (error) => {
          this.showError('Erreur lors de la pioche');
        }
      });
    } else if (action.action === 'STOP') {
      // Joueur clique sur "Stop"
      this.gameService.stopDrawing(this.roomId).subscribe({
        next: () => {
          // L'état sera mis à jour via WebSocket
        },
        error: (error) => {
          this.showError('Erreur lors de l\'arrêt');
        }
      });
    }
  }

  leaveRoom(): void {
    if (this.roomId) {
      this.wsService.unsubscribeFromRoom(this.roomId);
    }
    this.router.navigate(['/room']);
  }

  private updateRoomState(): void {
    if (!this.currentRoom) {
      return;
    }
    
    this.isAdmin = this.currentRoom.adminId === this.currentUserId;
    this.isMyTurn = this.currentRoom.currentPlayerId === this.currentUserId;
    
  }

  showError(message: string): void {
    // Effacer le timer précédent si existe
    if (this.errorTimeout) {
      clearTimeout(this.errorTimeout);
    }
    
    // Traduire le message si c'est une clé de traduction
    this.errorMessage = this.translate.instant(message);
    
    // Auto-effacer après 5 secondes
    this.errorTimeout = setTimeout(() => {
      this.clearError();
    }, 5000);
  }

  clearError(): void {
    if (this.errorTimeout) {
      clearTimeout(this.errorTimeout);
      this.errorTimeout = null;
    }
    this.errorMessage = '';
  }

  showSuccess(message: string, duration: number = 5000): void {
    // Effacer le timer précédent si existe
    if (this.successTimeout) {
      clearTimeout(this.successTimeout);
    }
    
    this.successMessage = message;
    
    // Auto-effacer après la durée spécifiée
    this.successTimeout = setTimeout(() => {
      this.clearSuccess();
    }, duration);
  }

  clearSuccess(): void {
    if (this.successTimeout) {
      clearTimeout(this.successTimeout);
      this.successTimeout = null;
    }
    this.successMessage = '';
  }

  showFlip7CelebrationPopup(): void {
    // Effacer le timer précédent si existe
    if (this.flip7Timeout) {
      clearTimeout(this.flip7Timeout);
    }
    
    this.showFlip7Celebration = true;
    
    // Auto-fermer après 3 secondes
    this.flip7Timeout = setTimeout(() => {
      this.showFlip7Celebration = false;
    }, 3000);
  }

  /**
   * Vérifie si le joueur actuel est éliminé
   */
  isPlayerEliminated(): boolean {
    if (!this.gameState?.players || !this.currentUserId) {
      return false;
    }
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    return myPlayer?.status === 'ELIMINATED';
  }

  /**
   * Vérifie si le joueur actuel a des cartes en main
   */
  hasCards(): boolean {
    if (!this.gameState?.players || !this.currentUserId) {
      return false;
    }
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!myPlayer) {
      return false;
    }
    const hasCardsInHand = myPlayer?.hand && Array.isArray(myPlayer.hand) && myPlayer.hand.length > 0;
    return hasCardsInHand || false;
  }

  /**
   * Retourne le message de statut du joueur actuel
   * (utilisé pour afficher un message informatif à la place des boutons)
   */
  getPlayerStatusMessage(): { icon: string, message: string } | null {
    if (!this.gameState?.players || !this.currentUserId) {
      return null;
    }

    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!myPlayer) return null;

    // Si c'est le tour du joueur et qu'il peut jouer normalement
    if (this.isMyTurn && myPlayer.status === 'PLAYING') {
      return null; // Afficher les boutons normaux
    }

    // Joueur éliminé
    if (myPlayer.status === 'ELIMINATED') {
      // Vérifier si l'élimination a eu lieu pendant un +3
      if (myPlayer.drawThreeByUsername) {
        return { 
          icon: '💀', 
          message: this.translate.instant('game.status.eliminated_during_draw_message', { 
            player: myPlayer.drawThreeByUsername 
          })
        };
      }
      return { 
        icon: '💀', 
        message: this.translate.instant('game.status.eliminated_message')
      };
    }

    // Joueur stoppé (a choisi de s'arrêter)
    if (myPlayer.status === 'STOPPED') {
      return { 
        icon: '✋', 
        message: this.translate.instant('game.status.stopped_message')
      };
    }

    // Joueur forcé de s'arrêter (carte Stop reçue)
    if (myPlayer.status === 'FORCED_STOP') {
      // Vérifier si c'est une auto-assignation ou reçue d'un autre joueur
      if (myPlayer.stoppedByUsername) {
        return { 
          icon: '🛑', 
          message: this.translate.instant('game.status.forced_stop_message', { 
            player: myPlayer.stoppedByUsername 
          })
        };
      }
      return { 
        icon: '🛑', 
        message: this.translate.instant('game.status.forced_stop_self_message')
      };
    }

    // Joueur stoppé par un Flip7 (score exactement 7)
    if (myPlayer.status === 'FLIP7_STOP') {
      return { 
        icon: '🎯', 
        message: this.translate.instant('game.status.flip7_stop_message')
      };
    }

    // Ce n'est pas le tour du joueur pendant une phase de jeu active :
    // afficher un message inline plutôt que les boutons désactivés.
    if (!this.isMyTurn && myPlayer.status === 'PLAYING' && this.gameState?.gameState === 'PLAYING') {
      const currentPlayerIndex = this.gameState.currentPlayerIndex;
      const currentPlayer = (currentPlayerIndex >= 0)
        ? this.gameState.players[currentPlayerIndex]
        : null;
      return {
        icon: '⏳',
        message: this.translate.instant('game.status.waiting_for_player', {
          player: currentPlayer?.username || '...'
        })
      };
    }

    return null;
  }

  /**
   * Tire une carte (Hit)
   */
  drawCard(): void {
    
    if (!this.roomId) {
      return;
    }
    
    if (!this.isMyTurn) {
      this.showError('Ce n\'est pas votre tour');
      return;
    }
    
    this.gameService.drawCard(this.roomId).subscribe({
      next: (result) => {
        
        if (result.needsStopAssignment && result.card) {
          // Carte Stop ou DrawThree piochée - afficher la sélection de joueur
          const cardData = result.card as any;
          
          // Le backend retourne "cardType" au lieu de "type"
          if (cardData.cardType === 'SPECIAL' || cardData.type === 'SPECIAL') {
            if (cardData.specialType === 'STOP') {
              this.showStopCardSelection(result.card);
            } else if (cardData.specialType === 'DRAW_THREE') {
              this.showDrawThreeCardSelection(result.card);
            } else {
            }
          } else {
          }
        } else if (result.eliminated) {
          // Message supprimé - déjà affiché dans le footer avec getPlayerStatusMessage()
        } else if (result.lifeUsed) {
        } else if (result.roundEnded) {
          this.showFlip7CelebrationPopup();
        }
        
        // L'état sera mis à jour via WebSocket.
        // Fallback : si le WebSocket est silencieux (coupure mobile), on re-fetch via HTTP.
        if (!this.wsService.isConnected()) {
          this.gameService.getGameState(this.roomId).subscribe({
            next: (state) => {
              this.gameState = state;
              if (state.players && state.currentPlayerIndex >= 0) {
                const cp = state.players[state.currentPlayerIndex];
                this.currentPlayerId = cp.userId;
                this.isMyTurn = this.currentPlayerId === this.currentUserId;
              }
            },
            error: () => {}
          });
        }
      },
      error: (error) => {
        this.showError('Erreur lors de la pioche');
      }
    });
  }

  /**
   * Arrête de tirer des cartes (Stop)
   */
  stopDrawing(): void {
    
    if (!this.roomId) {
      return;
    }
    
    if (!this.isMyTurn) {
      this.showError('Ce n\'est pas votre tour');
      return;
    }
    
    this.gameService.stopDrawing(this.roomId).subscribe({
      next: () => {
        if (!this.wsService.isConnected()) {
          this.gameService.getGameState(this.roomId).subscribe({
            next: (state) => {
              this.gameState = state;
              if (state.players && state.currentPlayerIndex >= 0) {
                const cp = state.players[state.currentPlayerIndex];
                this.currentPlayerId = cp.userId;
                this.isMyTurn = this.currentPlayerId === this.currentUserId;
              }
            },
            error: () => {}
          });
        }
      },
      error: (error) => {
        this.showError('Erreur lors de l\'arrêt');
      }
    });
  }

  /**
   * Retourne le username du joueur actuel
   */
  getCurrentPlayerUsername(): string {
    if (this.gameState?.players && this.gameState.currentPlayerIndex >= 0) {
      const currentPlayer = this.gameState.players[this.gameState.currentPlayerIndex];
      return currentPlayer?.username || '';
    }
    return '';
  }

  /**
   * Ouvre le modal de sélection de joueur pour la carte Stop
   */
  showStopCardSelection(card: any): void {
    
    // Définir la carte AVANT de vérifier l'auto-assignation
    this.stopCardToAssign = card;
    
    // Vérifier combien de joueurs sont assignables (non éliminés, non stopped)
    const assignablePlayers = this.getAssignablePlayers();
    
    // Si un seul joueur assignable (le joueur actuel), auto-assigner
    if (assignablePlayers.length === 1) {
      this.assignStopToPlayer(this.currentUserId);
      return;
    }
    
    // Sinon, afficher le modal
    this.selectorMinimized = false;
    this.showStopCardModal = true;
  }

  /**
   * Assigne la carte Stop au joueur sélectionné
   */
  assignStopToPlayer(playerId: string): void {
    
    if (!this.stopCardToAssign) {
      this.showError('Aucune carte Stop à assigner');
      return;
    }

    if (!this.stopCardToAssign.id) {
      this.showError('Erreur: ID de carte manquant');
      return;
    }

    
    this.gameService.assignStopCard(
      this.roomId, 
      this.stopCardToAssign.id, 
      playerId
    ).subscribe({
      next: (result) => {
        this.closeStopCardModal();
        if (!this.wsService.isConnected()) {
          this.gameService.getGameState(this.roomId).subscribe({
            next: (state) => {
              this.gameState = state;
              if (state.players && state.currentPlayerIndex >= 0) {
                const cp = state.players[state.currentPlayerIndex];
                this.currentPlayerId = cp.userId;
                this.isMyTurn = this.currentPlayerId === this.currentUserId;
              }
            },
            error: () => {}
          });
        }
      },
      error: (error) => {
        this.showError('Erreur lors de l\'assignation de la carte Stop');
      }
    });
  }

  /**
   * Ferme le modal de sélection de joueur (Stop card)
   */
  closeStopCardModal(): void {
    this.showStopCardModal = false;
    this.stopCardToAssign = null;
  }

  /**
   * Affiche le modal de sélection de joueur pour la carte DrawThree
   */
  showDrawThreeCardSelection(card: any): void {
    
    // Définir la carte AVANT de vérifier l'auto-assignation
    this.drawThreeCardToAssign = card;
    
    // Vérifier combien de joueurs sont assignables (non éliminés, non stopped)
    const assignablePlayers = this.getAssignablePlayers();
    
    // Si un seul joueur assignable (le joueur actuel), auto-assigner
    if (assignablePlayers.length === 1) {
      this.assignDrawThreeToPlayer(this.currentUserId);
      return;
    }
    
    // Sinon, afficher le modal
    this.selectorMinimized = false;
    this.showDrawThreeModal = true;
  }
  
  /**
   * Retourne la liste des joueurs à qui on peut assigner une carte
   * (non éliminés, non stopped)
   */
  private getAssignablePlayers(): any[] {
    if (!this.gameState || !this.gameState.players) {
      return [];
    }
    
    const assignable = this.gameState.players.filter((player: any) => {
      const isAssignable = player.status !== 'ELIMINATED' && 
                           player.status !== 'STOPPED' && 
                           player.status !== 'FORCED_STOP';
      return isAssignable;
    });
    
    return assignable;
  }

  /**
   * Assigne la carte DrawThree au joueur sélectionné
   */
  assignDrawThreeToPlayer(playerId: string): void {
    if (!this.drawThreeCardToAssign) {
      this.showError('Aucune carte DrawThree à assigner');
      return;
    }

    
    this.gameService.assignDrawThreeCard(
      this.roomId, 
      this.drawThreeCardToAssign.id, 
      playerId
    ).subscribe({
      next: (result) => {
        this.closeDrawThreeModal();
      },
      error: (error) => {
        this.showError('Erreur lors de l\'assignation de la carte DrawThree');
      }
    });
  }

  /**
   * Ferme le modal de sélection de joueur (DrawThree card)
   */
  closeDrawThreeModal(): void {
    this.showDrawThreeModal = false;
    this.drawThreeCardToAssign = null;
  }

  /**
   * Retourne les coéquipiers éligibles à recevoir la carte Vie (mode équipe).
   * Inclut le joueur lui-même (il peut garder la carte Vie).
   */
  get eligibleLifeTargets(): any[] {
    if (!this.lifeCardToAssign || !this.gameState?.players) return [];
    const myPlayer = this.gameState.players.find((p: any) => p.userId === this.currentUserId);
    if (!myPlayer) return [];
    const myTeamId = myPlayer.teamId;
    return this.gameState.players.filter((p: any) =>
      p.teamId === myTeamId &&
      (p.status === 'PLAYING' || p.status === 'STOPPED')
    );
  }

  /**
   * Assigne la carte Vie à un coéquipier (mode équipe)
   */
  assignLifeCardToTeammate(targetPlayerId: string): void {
    if (!this.lifeCardToAssign?.id) {
      this.showError('Erreur: ID de carte Vie manquant');
      return;
    }
    this.gameService.assignLifeCard(this.roomId, this.lifeCardToAssign.id, targetPlayerId).subscribe({
      next: (result) => {
        this.showLifeCardModal = false;
        this.lifeCardToAssign = null;
      },
      error: (error) => {
        this.showError('Erreur lors du transfert de la carte Vie');
      }
    });
  }

  /**
   * Démarre le prochain round (appelé par le joueur actif)
   */
  startRound(): void {
    if (!this.isMyTurn) {
      this.showError('Ce n\'est pas à vous de démarrer le round');
      return;
    }

    
    this.gameService.startNextRound(this.roomId).subscribe({
      next: (result) => {
        this.showStartRoundPopup = false;
      },
      error: (error) => {
        this.showError('Erreur lors du démarrage du round');
      }
    });
  }

  /**
   * Prépare les données pour la popup de fin de partie
   */
  prepareGameOverData(gameState: any): void {
    if (!gameState.finalScores || gameState.finalScores.length === 0) {
      return;
    }

    // Trier les joueurs par score total décroissant
    const sortedPlayers = [...gameState.finalScores].sort((a, b) => b.totalScore - a.totalScore);
    
    // Créer le classement
    this.gameOverRankings = sortedPlayers.map((player, index) => ({
      username: player.username,
      score: player.totalScore,
      rank: index + 1,
      isWinner: index === 0
    }));

    // Le gagnant est le premier du classement
    this.gameOverWinner = sortedPlayers[0].username;

    // Données team mode
    this.gameOverTeamMode = gameState.teamMode || false;
    this.gameOverWinningTeamId = gameState.winningTeamId || 0;
    this.gameOverTeams = gameState.teams || {};
    // Map userId → username from finalScores
    this.gameOverPlayerNames = {};
    gameState.finalScores?.forEach((p: any) => {
      if (p.userId) this.gameOverPlayerNames[p.userId] = p.username;
    });

    // Afficher la popup
    this.showGameOverPopup = true;

  }

  /**
   * Gère le clic sur "Rejouer"
   */
    onPlayAgain() {

      if (!this.roomId) {
        return;
      }

      this.showGameOverPopup = false;

      if (this.isAdmin) {
        // Admin triggers backend reset
        this.gameService.restartGame(this.roomId).subscribe({
          next: () => {
            this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
          },
          error: (error) => {
            this.showError('Erreur lors de la réinitialisation de la salle');
          }
        });
      } else {
        // Non-admin: just redirect
        this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
      }
    }

  /**
   * Calcule la proba d'élimination pour chaque joueur (pour +3)
   */
  getAllPlayersProbabilities(): { name: string, probability: number | null }[] {
    if (!this.gameState?.players) return [];
    return this.gameState.players.map((p: any) => {
      if (!p.hand) return { name: p.username || p.userId, probability: null };
      const handValues = p.hand.filter((card: any) => card.cardType === 'NUMBER' && typeof card.value === 'number').map((card: any) => card.value);
      if (handValues.length === 0) return { name: p.username || p.userId, probability: null };
      const valueCounts: { [value: number]: number } = {};
      for (const v of handValues) valueCounts[v] = (valueCounts[v] || 0) + 1;
      if (Object.values(valueCounts).some(count => count >= 2)) return { name: p.username || p.userId, probability: 100 };
      const initialDeck: { [value: number]: number } = {};
      for (let v = 0; v <= 12; v++) initialDeck[v] = (v === 0 || v === 1) ? 1 : v;
      const allHands = this.gameState.players.flatMap((pl: any) => Array.isArray(pl.hand) ? pl.hand : []);
      const usedCount: { [value: number]: number } = {};
      allHands.forEach((card: any) => {
        if (card.cardType === 'NUMBER' && typeof card.value === 'number') {
          usedCount[card.value] = (usedCount[card.value] || 0) + 1;
        }
      });
      const totalRemaining = typeof this.gameState.remainingCards === 'number' ? this.gameState.remainingCards : 0;
      if (totalRemaining === 0) return { name: p.username || p.userId, probability: null };
      let eliminationNumerator = 0;
      for (const v of new Set(handValues)) {
        const value = Number(v);
        const remaining = Math.max(0, initialDeck[value] - (usedCount[value] || 0));
        eliminationNumerator += remaining;
      }
      const prob = eliminationNumerator / totalRemaining;
      return { name: p.username || p.userId, probability: Math.min((1-prob) * 100, 100) };
    });
  }

  /**
   * Gère le clic sur "Quitter la salle"
   */
  onLeaveRoom(): void {
    this.showGameOverPopup = false;
    this.router.navigate(['/']);
  }
}
