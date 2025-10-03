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
import { PlayerBoardComponent } from '../../components/player-board/player-board.component';
import { ScoreBoardComponent } from '../../components/score-board/score-board.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { PlayerSelectorComponent } from '../../components/player-selector/player-selector.component';
import { StartRoundPopupComponent } from '../../components/start-round-popup/start-round-popup.component';
@Component({
  selector: 'app-game-page',
  imports: [CommonModule, FormsModule, PlayersBoardComponent, PlayerBoardComponent, ScoreBoardComponent, FooterComponent, PlayerSelectorComponent, StartRoundPopupComponent],
  templateUrl: './game-page.component.html',
  styleUrl: './game-page.component.scss'
})
export class GamePageComponent implements OnInit, OnDestroy {
  // View state for game
  gameViewMode: 'all' | 'player' | 'score' = 'player';
  
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
  
  // Stop Card Selection
  showStopCardModal: boolean = false;
  stopCardToAssign: any = null;

  // DrawThree Card Selection
  showDrawThreeModal: boolean = false;
  drawThreeCardToAssign: any = null;

  // Start Round Popup
  showStartRoundPopup: boolean = false;
  
  private subscriptions: Subscription[] = [];

  constructor(
    private roomService: RoomService,
    private wsService: WebSocketService,
    private gameService: GameService,
    private router: Router,
    private route: ActivatedRoute,
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

    // Get room ID from route params
    this.roomId = this.route.snapshot.paramMap.get('roomId') || '';
    
    if (!this.roomId) {
      this.router.navigate(['/room']);
      return;
    }
    
    // Load room data
    this.loadRoom();
    
    // Connect to WebSocket
    this.wsService.connect();
    
    // Subscribe to connection status
    this.subscriptions.push(
      this.wsService.connectionStatus$.subscribe(connected => {
        this.wsConnected = connected;
        if (connected) {
          console.log('✅ WebSocket connected');
          // Subscribe to room when connected
          if (this.roomId) {
            console.log('📡 Subscribing to room:', this.roomId);
            this.wsService.subscribeToRoom(this.roomId);
          }
        } else {
          console.warn('⚠️ WebSocket disconnected');
        }
      })
    );
    
    // Subscribe to room updates
    this.subscriptions.push(
      this.wsService.roomUpdates$.subscribe(room => {
        console.log('🔄 Room update received:', room);
        console.log('Players now:', room.players);
        this.currentRoom = room;
        this.updateRoomState();
      })
    );
    
    // Subscribe to game starts
    this.subscriptions.push(
      this.wsService.gameStarts$.subscribe(room => {
        console.log('🎮 Game started event received:', room);
        this.currentRoom = room;
        this.updateRoomState();
        
        // Récupérer immédiatement l'état du jeu pour avoir les cartes initiales
        console.log('📡 Fetching initial game state...');
        this.gameService.getGameState(this.roomId).subscribe({
          next: (gameState) => {
            console.log('✅ Initial game state received:', gameState);
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
    
    // Subscribe to turn updates
    this.subscriptions.push(
      this.wsService.turnUpdates$.subscribe(update => {
        console.log('Turn update:', update);
        this.currentRoom = update.room;
        this.updateRoomState();
        // Handle move data here
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
          this.currentPlayerId = currentPlayer.userId;
          this.isMyTurn = this.currentPlayerId === this.currentUserId;
          console.log('🎯 Current player:', this.currentPlayerId, '- My turn:', this.isMyTurn);
        }

        // Détecter l'état WAITING_NEXT_ROUND pour afficher le popup
        if (response.gameState === 'WAITING_NEXT_ROUND') {
          console.log('⏳ Waiting for next round - showing popup');
          this.showStartRoundPopup = true;
        } else {
          this.showStartRoundPopup = false;
        }

        // Détecter si le joueur actuel a une carte Stop ou DrawThree pending
        // Note: Pas besoin de vérifier isMyTurn car on peut avoir une carte Stop/DrawThree en distribution initiale
        if (response.players && response.players.length > 0) {
          const myPlayer = response.players.find((p: any) => p.userId === this.currentUserId);
          if (myPlayer && myPlayer.hand) {
            // Vérifier carte Stop (backend retourne "cardType" au lieu de "type")
            const pendingStopCard = myPlayer.hand.find((card: any) => 
              (card.type === 'SPECIAL' || card.cardType === 'SPECIAL') && 
              card.specialType === 'STOP' && 
              card.pending === true
            );
            
            if (pendingStopCard && !this.showStopCardModal) {
              console.log('🛑 Pending Stop card detected in hand - showing player selection');
              this.showStopCardSelection(pendingStopCard);
            }

            // Vérifier carte DrawThree (backend retourne "cardType" au lieu de "type")
            const pendingDrawThreeCard = myPlayer.hand.find((card: any) => 
              (card.type === 'SPECIAL' || card.cardType === 'SPECIAL') && 
              card.specialType === 'DRAW_THREE' && 
              card.pending === true
            );
            
            if (pendingDrawThreeCard && !this.showDrawThreeModal) {
              console.log('➕3️⃣ Pending DrawThree card detected in hand - showing player selection');
              this.showDrawThreeCardSelection(pendingDrawThreeCard);
            }
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
          this.showError('Vous ne faites pas partie de cette room');
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
        
        // Si le jeu est déjà en cours, récupérer l'état actuel
        console.log('📡 Fetching current game state...');
        this.gameService.getGameState(this.roomId).subscribe({
          next: (gameState) => {
            console.log('✅ Game state received:', gameState);
            this.gameState = gameState;
            if (gameState.players && gameState.players.length > 0) {
              gameState.players.forEach((p: any) => {
                console.log(`   - ${p.username}: ${p.hand?.length || 0} cards`);
              });
              if (gameState.currentPlayerIndex >= 0) {
                const currentPlayer = gameState.players[gameState.currentPlayerIndex];
                this.currentPlayerId = currentPlayer.userId;
                this.isMyTurn = this.currentPlayerId === this.currentUserId;
                console.log('🎯 Current player:', currentPlayer.username, '- My turn:', this.isMyTurn);
              }
            }
          },
          error: (err) => console.error('❌ Error fetching game state:', err)
        });
      },
      error: (error) => {
        console.error('❌ Error loading room:', error);
        this.showError('Room introuvable');
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
            this.showError('Double ! Vous êtes éliminé !');
          } else if (result.lifeUsed) {
            console.log('⚡ Carte Vie utilisée automatiquement');
          } else if (result.roundEnded) {
            console.log('� FLIP7 ! 7 cartes numérotées différentes ! Le round s\'arrête et vous gagnez +15 points !');
            this.showSuccess('🎯 FLIP7 ! 7 cartes numérotées différentes ! +15 points bonus !');
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
    
    this.errorMessage = message;
    
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

  showSuccess(message: string): void {
    // Effacer le timer précédent si existe
    if (this.successTimeout) {
      clearTimeout(this.successTimeout);
    }
    
    this.successMessage = message;
    
    // Auto-effacer après 5 secondes
    this.successTimeout = setTimeout(() => {
      this.clearSuccess();
    }, 5000);
  }

  clearSuccess(): void {
    if (this.successTimeout) {
      clearTimeout(this.successTimeout);
      this.successTimeout = null;
    }
    this.successMessage = '';
  }

  switchGameView(mode: 'all' | 'player' | 'score'): void {
    this.gameViewMode = mode;
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
    console.log('🛑 Opening Stop card selection modal', card);
    this.stopCardToAssign = card;
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

    console.log('🛑 Assigning Stop card to player:', playerId);
    
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
    this.drawThreeCardToAssign = card;
    this.showDrawThreeModal = true;
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
}
