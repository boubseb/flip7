import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RoomService } from '../../services/room/room.service';
import { WebSocketService } from '../../services/websocket/websocket.service';
import { GameService } from '../../services/game/game.service';
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
export class GamePageComponent implements OnInit, OnDestroy {
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
    // Cartes numérotées dans la main du joueur
    const handValues = myPlayer.hand
      .filter((card: any) => card.cardType === 'NUMBER' && typeof card.value === 'number')
      .map((card: any) => card.value);
    if (handValues.length === 0) {
      return null;
    }
    // Si déjà un doublon, proba = 100
    const valueCounts: { [value: number]: number } = {};
    for (const v of handValues) valueCounts[v] = (valueCounts[v] || 0) + 1;
    if (Object.values(valueCounts).some(count => count >= 2)) {
      return 100;
    }
    // Deck initial : 1x0, 1x1, 2x2, ..., 12x12
    const initialDeck: { [value: number]: number } = {};
    for (let v = 0; v <= 12; v++) {
      initialDeck[v] = (v === 0 || v === 1) ? 1 : v;
    }
    // Compte les cartes numérotées visibles dans toutes les mains
    const allHands = this.gameState.players.flatMap((p: any) => Array.isArray(p.hand) ? p.hand : []);
    const usedCount: { [value: number]: number } = {};
    allHands.forEach((card: any) => {
      if (card.cardType === 'NUMBER' && typeof card.value === 'number') {
        usedCount[card.value] = (usedCount[card.value] || 0) + 1;
      }
    });
    // Calcul de la proba d'élimination :
    // Pour chaque valeur unique de la main, on regarde combien il en reste dans le deck
    // Le dénominateur est le nombre total de cartes restantes à piocher (NUMBER + spéciales/bonus)
    const totalRemaining = typeof this.gameState.remainingCards === 'number' ? this.gameState.remainingCards : 0;
    if (totalRemaining === 0) {
      return null;
    }
    let eliminationNumerator = 0;
    for (const v of new Set(handValues)) {
      const value = Number(v);
      const remaining = Math.max(0, initialDeck[value] - (usedCount[value] || 0));
      eliminationNumerator += remaining;
    }
    const prob = eliminationNumerator / totalRemaining;
    return Math.min((1-prob) * 100, 100);
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
      card.cardType === 'SPECIAL' && card.specialType === 'LIFE' && !card.cancelled
    );
  }

  /**
   * Retourne la probabilité de survie en pourcentage (0-100), en tenant compte
   * de la carte vie. Toujours un nombre (jamais null) pour l'affichage.
   */
  getSurvivalProbability(): number {
    if (this.hasLifeCard()) return 100;
    const prob = this.calculateEliminationProbability();
    return prob !== null ? prob : 100;
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

  // Start Round Popup
  showStartRoundPopup: boolean = false;

  // Game Over Popup
  showGameOverPopup: boolean = false;
  gameOverRankings: PlayerRanking[] = [];
  gameOverWinner: string = '';
  
  private subscriptions: Subscription[] = [];

  constructor(
    private roomService: RoomService,
    private wsService: WebSocketService,
    private gameService: GameService,
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
        console.log('🔄 Route params changed - roomId:', newRoomId, '- previous:', this.roomId);
        
        if (!newRoomId) {
          this.router.navigate(['/room']);
          return;
        }
        
        // Si le roomId change vraiment (pas juste un refresh)
        if (newRoomId !== this.roomId) {
          // Désabonner de l'ancienne room si différente
          if (this.roomId && this.roomId !== newRoomId) {
            console.log('🔄 Unsubscribing from old room:', this.roomId);
            this.wsService.unsubscribeFromRoom(this.roomId);
          }
          
          this.roomId = newRoomId;
          this.gameState = null;
          this.currentRoom = null;
          
          console.log('📡 Loading room data for:', this.roomId);
          this.loadRoom();
        } else {
          console.log('⏭️ Same roomId, skipping reload');
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
          console.log('✅ WebSocket connected / reconnected');
          // The onConnect handler clears the subscriptions map, so we always
          // need to re-subscribe (handles both first connection and reconnection).
          if (this.roomId) {
            console.log('📡 Subscribing to room after (re)connection:', this.roomId);
            this.wsService.subscribeToRoom(this.roomId);
            // Re-fetch game state to recover any updates missed during disconnection
            if (this.gameState) {
              console.log('🔄 Re-fetching game state after reconnection...');
              this.gameService.getGameState(this.roomId).subscribe({
                next: (state) => {
                  console.log('✅ Game state refreshed after reconnection');
                  this.gameState = state;
                  if (state.players && state.currentPlayerIndex >= 0) {
                    const cp = state.players[state.currentPlayerIndex];
                    this.currentPlayerId = cp.userId;
                    this.isMyTurn = this.currentPlayerId === this.currentUserId;
                  }
                },
                error: (err) => console.warn('⚠️ Could not re-fetch game state:', err)
              });
            }
          }
        } else if (!connected) {
          console.warn('⚠️ WebSocket disconnected');
        }
      })
    );
    
    // Subscribe to room updates (uniquement pour les changements de room, pas de gameState)
    this.subscriptions.push(
      this.wsService.roomUpdates$.subscribe(room => {
        console.log('🔄 Room update received (not game state)');
        this.currentRoom = room;
        // Ne pas appeler updateRoomState() ici car ça peut causer des refresh
      })
    );
    
    // Subscribe to game starts
    this.subscriptions.push(
      this.wsService.gameStarts$.subscribe(room => {
        console.log('🎮 Game started event received:', room);
        this.currentRoom = room;
        
        // Récupérer immédiatement l'état du jeu pour avoir les cartes initiales
        console.log('📡 Fetching initial game state...');
        this.gameService.getGameState(this.roomId).subscribe({
          next: (gameState) => {
            console.log('✅ Initial game state received:', gameState);
            console.log('🔎 statisticsEnabled:', gameState.statisticsEnabled);
            this.gameState = gameState;
            if (gameState.players && gameState.players.length > 0) {
              gameState.players.forEach((p: any) => {
                console.log(`   - ${p.username}: ${p.hand?.length || 0} cards`);
              });
              if (gameState.currentPlayerIndex >= 0) {
                const currentPlayer = gameState.players[gameState.currentPlayerIndex];
                this.currentPlayerId = currentPlayer.userId;
                this.isMyTurn = this.currentPlayerId === this.currentUserId;
              }
            }
          },
          error: (err) => console.error('❌ Error fetching game state:', err)
        });
      })
    );

    // Subscribe to game over
    this.subscriptions.push(
      this.wsService.gameOver$.subscribe((data: any) => {
        console.log('🏁 Game over event received:', data);
        this.prepareGameOverData(data);
      })
    );

    // Subscribe to game restarted
    this.subscriptions.push(
      this.wsService.gameRestarted$.subscribe(data => {
        console.log('🔄 Game restarted event received:', data);
        this.showGameOverPopup = false;

        // Pour les non-admins : rejoindre automatiquement la room remise à zéro
        // puis naviguer vers la salle d'attente.
        if (!this.isAdmin) {
          this.roomService.rejoinAfterRestart(this.roomId).subscribe({
            next: () => {
              console.log('✅ Rejoint la room après restart');
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
        console.log('🔄 Turn update received (ignored, using gameStateUpdates$ instead)');
        // Ne rien faire, on attend le gameStateUpdate
      })
    );
    
    // Subscribe to game state updates
    this.subscriptions.push(
      this.wsService.gameStateUpdates$.subscribe(response => {
        console.log('🎮 Game state update received:', response);
        console.log('   - Game State:', response.gameState);
        console.log('   - Players:', response.players);
        if (response.players && response.players.length > 0) {
          response.players.forEach((p: any) => {
            console.log(`   - ${p.username}: ${p.hand?.length || 0} cards, score: ${p.roundScore}`);
          });
        }
        
        this.gameState = response; // Le DTO complet contient players, pas response.gameState
        
        // Déterminer le joueur actuel à partir de l'index
        if (response.players && response.players.length > 0 && response.currentPlayerIndex >= 0) {
          const currentPlayer = response.players[response.currentPlayerIndex];
          const previousPlayerId = this.currentPlayerId;
          this.currentPlayerId = currentPlayer.userId;
          this.isMyTurn = this.currentPlayerId === this.currentUserId;
          console.log('🎯 Current player:', this.currentPlayerId, '- My turn:', this.isMyTurn);
          
          // Afficher une alerte quand le tour change et que ce n'est pas notre tour (pendant un round actif)
          if (response.gameState === 'PLAYING' && previousPlayerId && previousPlayerId !== this.currentPlayerId && !this.isMyTurn) {
            this.showSuccess(this.translate.instant('game.status.your_turn', { player: currentPlayer.username }), 1000);
          }
        }

        // Détecter l'état WAITING_NEXT_ROUND
        // On ne montre PLUS le popup, le bouton est directement dans le game footer
        if (response.gameState === 'WAITING_NEXT_ROUND') {
          if (this.isMyTurn) {
            console.log('⏳ Waiting for next round - YOUR turn to start');
          } else {
            console.log('⏳ Waiting for next round - waiting for', this.getCurrentPlayerUsername());
          }
          // Ne plus afficher le popup
          this.showStartRoundPopup = false;
        } else {
          this.showStartRoundPopup = false;
        }

        // Détecter l'état GAME_OVER pour afficher le popup de fin de partie
        if (response.gameState === 'GAME_OVER') {
          console.log('🏆 Game Over detected - preparing rankings');
          this.prepareGameOverData(response);
        }

        // DEBUG: Afficher la réponse complète
        console.log('📦 Full response received:', {
          pendingSpecialCards: response.pendingSpecialCards,
          currentUserId: this.currentUserId,
          gameState: response.gameState
        });

        // Détecter si le joueur actuel a des cartes spéciales en attente d'assignation
        // Utiliser la queue pendingSpecialCards au lieu de chercher dans la main
        if (response.pendingSpecialCards && response.pendingSpecialCards.length > 0) {
          console.log('🔍 Checking pending special cards queue:', response.pendingSpecialCards);
          console.log('🔍 Current modals state - Stop:', this.showStopCardModal, 'DrawThree:', this.showDrawThreeModal);
          console.log('🔍 Current user ID:', this.currentUserId);
          
          // DEBUG: Afficher chaque carte en attente
          response.pendingSpecialCards.forEach((pending: any, index: number) => {
            console.log(`   📋 Card ${index}:`, {
              type: pending.specialType,
              sourcePlayerId: pending.sourcePlayerId,
              targetPlayerId: pending.targetPlayerId,
              remainingForcedDraws: pending.remainingForcedDraws,
              isForMe: pending.sourcePlayerId === this.currentUserId
            });
          });
          
          // Chercher une carte STOP en attente pour le joueur actuel
          const pendingStopCard = response.pendingSpecialCards.find((pending: any) => 
            pending.sourcePlayerId === this.currentUserId &&
            !pending.targetPlayerId &&
            pending.specialType === 'STOP'
          );
          
          if (pendingStopCard && !this.showStopCardModal) {
            console.log('🛑 Pending Stop card detected in queue - showing player selection');
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
          
          console.log('🔍 Pending DrawThree card found in queue?', pendingDrawThreeCard);
          
          if (pendingDrawThreeCard && !this.showDrawThreeModal) {
            console.log('➕3️⃣ Pending DrawThree card detected in queue - showing player selection');
            // Créer un objet carte compatible avec showDrawThreeCardSelection
            const card = { id: pendingDrawThreeCard.cardId, specialType: pendingDrawThreeCard.specialType };
            this.showDrawThreeCardSelection(card);
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.roomId) {
      this.wsService.unsubscribeFromRoom(this.roomId);
    }
    this.wsService.disconnect();
  }

  loadRoom(): void {
    console.log('🔍 Loading room:', this.roomId);
    this.roomService.getRoom(this.roomId).subscribe({
      next: (room) => {
        console.log('✅ Room loaded:', room);
        console.log('Current user:', this.currentUserId);
        console.log('Players in room:', room.players);
        
        // Check if user is part of the room
        if (!room.players.includes(this.currentUserId)) {
          console.warn('❌ User not in room');
          this.showError('game.errors.notInRoom');
          setTimeout(() => this.router.navigate(['/room']), 2000);
          return;
        }

        this.currentRoom = room;
        this.updateRoomState();
        
        // Redirect to room-page if game not started yet
        if (room.status !== RoomStatus.IN_GAME && room.status !== RoomStatus.FINISHED) {
          console.log('⚠️ Game not started yet, redirecting to room-page');
          this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
          return;
        }
        
        console.log('✅ Game in progress, displaying game view');

        // If the WS is already connected (e.g. the connection resolved before this
        // HTTP response came back), ensure the room subscriptions are registered now.
        // Without this guard a page-refresh where WS connects before loadRoom()
        // completes would skip subscribeToRoom because connectionStatus$ fires with
        // this.roomId not yet confirmed as a valid in-game room.
        if (this.wsService.isConnected()) {
          console.log('📡 WS already connected — ensuring room subscriptions are active');
          this.wsService.subscribeToRoom(this.roomId);
        }

        // Si le jeu est déjà en cours, récupérer l'état actuel pour reconnexion (une seule fois)
        if (!this.gameState) {
          console.log('📡 Fetching current game state for reconnection...');
          this.gameService.getGameState(this.roomId).subscribe({
            next: (gameState) => {
              console.log('✅ Game state received for reconnection:', gameState);
              this.gameState = gameState;
              
              if (gameState.players && gameState.players.length > 0) {
                gameState.players.forEach((p: any) => {
                  console.log(`   - ${p.username}: ${p.hand?.length || 0} cards, status: ${p.status}, score: ${p.roundScore}/${p.totalScore}`);
                });
                
                if (gameState.currentPlayerIndex >= 0) {
                  const currentPlayer = gameState.players[gameState.currentPlayerIndex];
                  this.currentPlayerId = currentPlayer.userId;
                  this.isMyTurn = this.currentPlayerId === this.currentUserId;
                  console.log('🎯 Current player:', currentPlayer.username, '- My turn:', this.isMyTurn);
                }
                
                // Vérifier si on a des cartes pending à assigner (Stop/DrawThree)
                // Utiliser la queue pendingSpecialCards au lieu de chercher dans la main
                if (gameState.pendingSpecialCards && gameState.pendingSpecialCards.length > 0) {
                  console.log('🔍 Checking pending special cards on reconnection:', gameState.pendingSpecialCards);
                  console.log('🔍 Current user ID on reconnection:', this.currentUserId);
                  
                  // DEBUG: Afficher chaque carte en attente
                  gameState.pendingSpecialCards.forEach((pending: any, index: number) => {
                    console.log(`   📋 Reconnection Card ${index}:`, {
                      type: pending.specialType,
                      sourcePlayerId: pending.sourcePlayerId,
                      targetPlayerId: pending.targetPlayerId,
                      remainingForcedDraws: pending.remainingForcedDraws,
                      isForMe: pending.sourcePlayerId === this.currentUserId
                    });
                  });
                  
                  const pendingStopCard = gameState.pendingSpecialCards.find((pending: any) => 
                    pending.sourcePlayerId === this.currentUserId &&
                    !pending.targetPlayerId &&
                    pending.specialType === 'STOP'
                  );
                  
                  if (pendingStopCard && !this.showStopCardModal) {
                    console.log('🛑 Pending Stop card detected on reconnection');
                    const card = { id: pendingStopCard.cardId, specialType: pendingStopCard.specialType };
                    this.showStopCardSelection(card);
                  }

                  const pendingDrawThreeCard = gameState.pendingSpecialCards.find((pending: any) => 
                    pending.sourcePlayerId === this.currentUserId &&
                    !pending.targetPlayerId &&
                    pending.specialType === 'DRAW_THREE'
                  );
                  
                  if (pendingDrawThreeCard && !this.showDrawThreeModal) {
                    console.log('➕3️⃣ Pending DrawThree card detected on reconnection');
                    const card = { id: pendingDrawThreeCard.cardId, specialType: pendingDrawThreeCard.specialType };
                    this.showDrawThreeCardSelection(card);
                  }
                }

                // Vérifier si on est en état WAITING_NEXT_ROUND
                if (gameState.gameState === 'WAITING_NEXT_ROUND') {
                  console.log('⏳ Reconnected during WAITING_NEXT_ROUND state');
                  // Le bouton est maintenant dans le game footer, pas besoin de popup
                  this.showStartRoundPopup = false;
                }
              }
            },
            error: (err) => {
              console.error('❌ Error fetching game state:', err);
              
              // Si erreur 400, c'est que la partie n'existe plus (backend redémarré ou partie terminée)
              if (err.status === 400) {
                console.warn('⚠️ Game not found - redirecting to room page');
                this.showError('game.errors.gameNotFoundAfterRestart');
                setTimeout(() => this.router.navigate(['/room']), 3000);
              } else {
                this.showError('game.errors.cannotLoadGameState');
              }
            }
          });
        } else {
          console.log('⏭️ Game state already loaded, skipping fetch');
        }
      },
      error: (error) => {
        console.error('❌ Error loading room:', error);
        this.showError('game.errors.roomNotFound');
        setTimeout(() => this.router.navigate(['/room']), 2000);
      }
    });
  }

  onPlayerAction(action: any): void {
    console.log('🎯 Player action received:', action);
    console.log('   - roomId:', this.roomId);
    console.log('   - isMyTurn:', this.isMyTurn);
    
    if (!this.roomId) {
      console.error('❌ No roomId!');
      return;
    }
    
    // Temporairement: permettre l'action même si ce n'est pas notre tour (pour debug)
    if (!this.isMyTurn) {
      console.warn('⚠️ Not your turn, but allowing action for debugging');
    }
    
    if (action.action === 'REVEAL') {
      // Joueur clique sur "Hit" - Tirer une carte
      console.log('🎲 Calling drawCard API...');
      this.gameService.drawCard(this.roomId).subscribe({
        next: (result) => {
          console.log('🃏 Card drawn:', result);
          console.log('   - needsStopAssignment:', result.needsStopAssignment);
          console.log('   - card:', result.card);
          
          if (result.needsStopAssignment && result.card) {
            // Carte Stop ou DrawThree piochée - afficher la sélection de joueur
            const cardData = result.card as any;
            console.log('🎴 Card needs assignment, cardType:', cardData.cardType);
            
            // Le backend retourne "cardType" au lieu de "type"
            if (cardData.cardType === 'SPECIAL' || cardData.type === 'SPECIAL') {
              console.log('   Special type:', cardData.specialType);
              if (cardData.specialType === 'STOP') {
                console.log('🛑 Stop card drawn - showing player selection');
                this.showStopCardSelection(result.card);
              } else if (cardData.specialType === 'DRAW_THREE') {
                console.log('➕3️⃣ DrawThree card drawn - showing player selection');
                this.showDrawThreeCardSelection(result.card);
              } else {
                console.warn('⚠️ Unknown special type:', cardData.specialType);
              }
            } else {
              console.warn('⚠️ needsStopAssignment but card is not SPECIAL:', cardData);
            }
          } else if (result.eliminated) {
            // Message supprimé - déjà affiché dans le footer avec getPlayerStatusMessage()
          } else if (result.lifeUsed) {
            console.log('⚡ Carte Vie utilisée automatiquement');
          } else if (result.roundEnded) {
            console.log('🎯 FLIP7 ! 7 cartes numérotées différentes ! Le round s\'arrête et vous gagnez +15 points !');
            this.showFlip7CelebrationPopup();
          }
          
          // L'état sera mis à jour via WebSocket
        },
        error: (error) => {
          console.error('❌ Error drawing card:', error);
          this.showError('Erreur lors de la pioche');
        }
      });
    } else if (action.action === 'STOP') {
      // Joueur clique sur "Stop"
      this.gameService.stopDrawing(this.roomId).subscribe({
        next: () => {
          console.log('✋ Stopped drawing');
          // L'état sera mis à jour via WebSocket
        },
        error: (error) => {
          console.error('❌ Error stopping:', error);
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
      console.warn('⚠️ updateRoomState called but currentRoom is null');
      return;
    }
    
    this.isAdmin = this.currentRoom.adminId === this.currentUserId;
    this.isMyTurn = this.currentRoom.currentPlayerId === this.currentUserId;
    
    console.log('🔄 Room state updated:');
    console.log('  - isAdmin:', this.isAdmin);
    console.log('  - isMyTurn:', this.isMyTurn);
    console.log('  - players count:', this.currentRoom.players.length);
    console.log('  - room status:', this.currentRoom.status);
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
    console.log('🎲 drawCard() called');
    console.log('   - roomId:', this.roomId);
    console.log('   - isMyTurn:', this.isMyTurn);
    
    if (!this.roomId) {
      console.error('❌ No roomId!');
      return;
    }
    
    if (!this.isMyTurn) {
      console.warn('⚠️ Not your turn!');
      this.showError('Ce n\'est pas votre tour');
      return;
    }
    
    console.log('🎲 Calling drawCard API...');
    this.gameService.drawCard(this.roomId).subscribe({
      next: (result) => {
        console.log('🃏 Card drawn:', result);
        console.log('   - needsStopAssignment:', result.needsStopAssignment);
        console.log('   - card:', result.card);
        
        if (result.needsStopAssignment && result.card) {
          // Carte Stop ou DrawThree piochée - afficher la sélection de joueur
          const cardData = result.card as any;
          console.log('🎴 Card needs assignment, cardType:', cardData.cardType);
          
          // Le backend retourne "cardType" au lieu de "type"
          if (cardData.cardType === 'SPECIAL' || cardData.type === 'SPECIAL') {
            console.log('   Special type:', cardData.specialType);
            if (cardData.specialType === 'STOP') {
              console.log('🛑 Stop card drawn - showing player selection');
              this.showStopCardSelection(result.card);
            } else if (cardData.specialType === 'DRAW_THREE') {
              console.log('➕3️⃣ DrawThree card drawn - showing player selection');
              this.showDrawThreeCardSelection(result.card);
            } else {
              console.warn('⚠️ Unknown special type:', cardData.specialType);
            }
          } else {
            console.warn('⚠️ needsStopAssignment but card is not SPECIAL:', cardData);
          }
        } else if (result.eliminated) {
          // Message supprimé - déjà affiché dans le footer avec getPlayerStatusMessage()
        } else if (result.lifeUsed) {
          console.log('⚡ Carte Vie utilisée automatiquement');
        } else if (result.roundEnded) {
          console.log('🎯 FLIP7 ! 7 cartes numérotées différentes ! Le round s\'arrête et vous gagnez +15 points !');
          this.showFlip7CelebrationPopup();
        }
        
        // L'état sera mis à jour via WebSocket
      },
      error: (error) => {
        console.error('❌ Error drawing card:', error);
        this.showError('Erreur lors de la pioche');
      }
    });
  }

  /**
   * Arrête de tirer des cartes (Stop)
   */
  stopDrawing(): void {
    console.log('✋ stopDrawing() called');
    
    if (!this.roomId) {
      console.error('❌ No roomId!');
      return;
    }
    
    if (!this.isMyTurn) {
      console.warn('⚠️ Not your turn!');
      this.showError('Ce n\'est pas votre tour');
      return;
    }
    
    this.gameService.stopDrawing(this.roomId).subscribe({
      next: () => {
        console.log('✋ Stopped drawing');
        // L'état sera mis à jour via WebSocket
      },
      error: (error) => {
        console.error('❌ Error stopping:', error);
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
    console.log('🛑 Opening Stop card selection modal');
    console.log('   - Card object:', card);
    console.log('   - Card ID:', card?.id);
    console.log('   - Card type:', card?.cardType || card?.type);
    console.log('   - Card specialType:', card?.specialType);
    
    // Définir la carte AVANT de vérifier l'auto-assignation
    this.stopCardToAssign = card;
    
    // Vérifier combien de joueurs sont assignables (non éliminés, non stopped)
    const assignablePlayers = this.getAssignablePlayers();
    console.log('👥 Joueurs assignables:', assignablePlayers.length);
    
    // Si un seul joueur assignable (le joueur actuel), auto-assigner
    if (assignablePlayers.length === 1) {
      console.log('⚠️ JOUEUR SEUL - Auto-assignation à soi-même');
      this.assignStopToPlayer(this.currentUserId);
      return;
    }
    
    // Sinon, afficher le modal
    this.showStopCardModal = true;
  }

  /**
   * Assigne la carte Stop au joueur sélectionné
   */
  assignStopToPlayer(playerId: string): void {
    console.log('🛑 assignStopToPlayer called');
    console.log('   - stopCardToAssign:', this.stopCardToAssign);
    console.log('   - stopCardToAssign.id:', this.stopCardToAssign?.id);
    console.log('   - playerId:', playerId);
    
    if (!this.stopCardToAssign) {
      this.showError('Aucune carte Stop à assigner');
      return;
    }

    if (!this.stopCardToAssign.id) {
      console.error('❌ Card ID is missing!', this.stopCardToAssign);
      this.showError('Erreur: ID de carte manquant');
      return;
    }

    console.log('🛑 Calling gameService.assignStopCard...');
    
    this.gameService.assignStopCard(
      this.roomId, 
      this.stopCardToAssign.id, 
      playerId
    ).subscribe({
      next: (result) => {
        console.log('✅ Stop card assigned successfully:', result);
        this.closeStopCardModal();
      },
      error: (error) => {
        console.error('❌ Error assigning Stop card:', error);
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
    console.log('➕3️⃣ Opening DrawThree card selection modal', card);
    
    // Définir la carte AVANT de vérifier l'auto-assignation
    this.drawThreeCardToAssign = card;
    
    // Vérifier combien de joueurs sont assignables (non éliminés, non stopped)
    const assignablePlayers = this.getAssignablePlayers();
    console.log('👥 Joueurs assignables:', assignablePlayers.length);
    
    // Si un seul joueur assignable (le joueur actuel), auto-assigner
    if (assignablePlayers.length === 1) {
      console.log('⚠️ JOUEUR SEUL - Auto-assignation à soi-même');
      this.assignDrawThreeToPlayer(this.currentUserId);
      return;
    }
    
    // Sinon, afficher le modal
    this.showDrawThreeModal = true;
  }
  
  /**
   * Retourne la liste des joueurs à qui on peut assigner une carte
   * (non éliminés, non stopped)
   */
  private getAssignablePlayers(): any[] {
    if (!this.gameState || !this.gameState.players) {
      console.log('⚠️ getAssignablePlayers: Pas de gameState ou players');
      return [];
    }
    
    const assignable = this.gameState.players.filter((player: any) => {
      const isAssignable = player.status !== 'ELIMINATED' && 
                           player.status !== 'STOPPED' && 
                           player.status !== 'FORCED_STOP';
      console.log(`   - ${player.username} (${player.status}): ${isAssignable ? '✅ Assignable' : '❌ Non assignable'}`);
      return isAssignable;
    });
    
    console.log(`📊 Total joueurs assignables: ${assignable.length}`);
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

    console.log('➕3️⃣ Assigning DrawThree card to player:', playerId);
    
    this.gameService.assignDrawThreeCard(
      this.roomId, 
      this.drawThreeCardToAssign.id, 
      playerId
    ).subscribe({
      next: (result) => {
        console.log('✅ DrawThree card assigned successfully:', result);
        this.closeDrawThreeModal();
      },
      error: (error) => {
        console.error('❌ Error assigning DrawThree card:', error);
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
   * Démarre le prochain round (appelé par le joueur actif)
   */
  startRound(): void {
    if (!this.isMyTurn) {
      this.showError('Ce n\'est pas à vous de démarrer le round');
      return;
    }

    console.log('🚀 Starting next round...');
    
    this.gameService.startNextRound(this.roomId).subscribe({
      next: (result) => {
        console.log('✅ Round started:', result);
        this.showStartRoundPopup = false;
      },
      error: (error) => {
        console.error('❌ Error starting round:', error);
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

    // Afficher la popup
    this.showGameOverPopup = true;

    console.log('🏆 Game Over Rankings:', this.gameOverRankings);
    console.log('👑 Winner:', this.gameOverWinner);
  }

  /**
   * Gère le clic sur "Rejouer"
   */
    onPlayAgain() {
      console.log('🔄 Play again requested');

      if (!this.roomId) {
        console.error('❌ No room ID available');
        return;
      }

      this.showGameOverPopup = false;

      if (this.isAdmin) {
        // Admin triggers backend reset
        this.gameService.restartGame(this.roomId).subscribe({
          next: () => {
            console.log('✅ Room reset via backend');
            this.router.navigate(['/room'], { queryParams: { mode: 'waiting', roomId: this.roomId } });
          },
          error: (error) => {
            console.error('❌ Error resetting room:', error);
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
    console.log('🚪 Leave room requested');
    this.showGameOverPopup = false;
    this.router.navigate(['/']);
  }
}
