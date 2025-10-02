import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RoomService } from '../../services/room/room.service';
import { WebSocketService } from '../../services/websocket/websocket.service';
import { Room, RoomStatus } from '../../models/room/room.model';
import { Subscription } from 'rxjs';
import { PlayersBoardComponent } from '../../components/players-board/players-board.component';
import { PlayerBoardComponent } from '../../components/player-board/player-board.component';
import { ScoreBoardComponent } from '../../components/score-board/score-board.component';
import { FooterComponent } from '../../components/footer/footer.component';
@Component({
  selector: 'app-game-page',
  imports: [CommonModule, FormsModule, PlayersBoardComponent, PlayerBoardComponent, ScoreBoardComponent, FooterComponent],
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
  
  // WebSocket
  wsConnected: boolean = false;
  
  // Error handling
  errorMessage: string = '';
  private errorTimeout: any = null;
  
  private subscriptions: Subscription[] = [];

  constructor(
    private roomService: RoomService,
    private wsService: WebSocketService,
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
        console.log('Game started:', room);
        this.currentRoom = room;
        this.updateRoomState();
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
      },
      error: (error) => {
        console.error('❌ Error loading room:', error);
        this.showError('Room introuvable');
        setTimeout(() => this.router.navigate(['/room']), 2000);
      }
    });
  }

  playTurn(move: string): void {
    if (!this.currentRoom || !this.isMyTurn) return;
    
    this.roomService.playTurn(this.currentRoom.id, move).subscribe({
      next: (room) => {
        console.log('Turn played');
        // Update will be received via WebSocket
      },
      error: (error) => {
        console.error('Error playing turn:', error);
        this.showError('Erreur lors du jeu');
      }
    });
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

  switchGameView(mode: 'all' | 'player' | 'score'): void {
    this.gameViewMode = mode;
  }
}
