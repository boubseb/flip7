import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RoomService } from '../../services/room/room.service';
import { WebSocketService } from '../../services/websocket/websocket.service';
import { Room, RoomStatus } from '../../models/room/room.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-game-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './game-page.component.html',
  styleUrl: './game-page.component.scss'
})
export class GamePageComponent implements OnInit, OnDestroy {
  // View states
  currentView: 'room' | 'game' = 'room';
  
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
            this.wsService.subscribeToRoom(this.roomId);
          }
        }
      })
    );
    
    // Subscribe to room updates
    this.subscriptions.push(
      this.wsService.roomUpdates$.subscribe(room => {
        console.log('Room update received:', room);
        this.currentRoom = room;
        this.updateRoomState();
      })
    );
    
    // Subscribe to game starts
    this.subscriptions.push(
      this.wsService.gameStarts$.subscribe(room => {
        console.log('Game started:', room);
        this.currentRoom = room;
        this.currentView = 'game';
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
    this.roomService.getRoom(this.roomId).subscribe({
      next: (room) => {
        this.currentRoom = room;
        this.updateRoomState();
        
        // Determine view based on room status
        if (room.status === RoomStatus.IN_GAME || room.status === RoomStatus.FINISHED) {
          this.currentView = 'game';
        } else {
          this.currentView = 'room';
        }
      },
      error: (error) => {
        console.error('Error loading room:', error);
        this.errorMessage = 'Room introuvable';
        setTimeout(() => this.router.navigate(['/room']), 2000);
      }
    });
  }

  startGame(): void {
    if (!this.currentRoom || !this.isAdmin) return;
    
    this.roomService.startGame(this.currentRoom.id).subscribe({
      next: (room) => {
        console.log('Game started');
        // The game start will be received via WebSocket
      },
      error: (error) => {
        console.error('Error starting game:', error);
        this.errorMessage = error.error?.message || 'Erreur lors du démarrage de la partie';
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
        this.errorMessage = 'Erreur lors du jeu';
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
    if (!this.currentRoom) return;
    
    this.isAdmin = this.currentRoom.adminId === this.currentUserId;
    this.isMyTurn = this.currentRoom.currentPlayerId === this.currentUserId;
  }

  clearError(): void {
    this.errorMessage = '';
  }

  getRoomStatusLabel(status: RoomStatus): string {
    const labels = {
      [RoomStatus.WAITING]: 'En attente',
      [RoomStatus.IN_GAME]: 'En cours',
      [RoomStatus.FINISHED]: 'Terminée'
    };
    return labels[status];
  }
}
