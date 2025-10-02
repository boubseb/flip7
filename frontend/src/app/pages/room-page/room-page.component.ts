import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { FooterComponent } from '../../components/footer/footer.component';
import { RoomService } from '../../services/room/room.service';
import { WebSocketService } from '../../services/websocket/websocket.service';
import { Room, RoomStatus, RoomCreateRequest, RoomJoinRequest } from '../../models/room/room.model';
import { RoomBrowserComponent } from '../../components/room-browser/room-browser.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-room-page',
  imports: [CommonModule, FormsModule, RoomBrowserComponent, FooterComponent],
  templateUrl: './room-page.component.html',
  styleUrl: './room-page.component.scss'
})
export class RoomPageComponent implements OnInit, OnDestroy {
  // View state: 'create' (create form) | 'join' (join form + list) | 'waiting' (waiting room)
  currentView: 'create' | 'join' | 'waiting' = 'create';
  
  // Available rooms
  availableRooms: Room[] = [];
  
  // Current room (when in waiting room)
  currentRoom: Room | null = null;
  currentUserId: string = '';
  isAdmin: boolean = false;
  wsConnected: boolean = false;
  
  // Create room form
  createRoomPassword: string = '';
  createRoomMaxPlayers: number = 4;
  
  // Join room form
  joinRoomId: string = '';
  joinRoomPassword: string = '';
  
  // Error handling
  errorMessage: string = '';
  successMessage: string = '';
  
  private subscriptions: Subscription[] = [];
  private messageTimeout?: any;
  
  constructor(
    private roomService: RoomService,
    private wsService: WebSocketService,
    private router: Router,
    private route: ActivatedRoute,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Skip during SSR
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Check authentication in browser only
    const token = localStorage.getItem('access_token');
    if (!token) {
      this.router.navigate(['/login']);
      return;
    }
    
    this.currentUserId = token;
    
    // Check query params to determine initial view
    this.route.queryParams.subscribe(params => {
      const mode = params['mode'];
      const roomId = params['roomId'];
      
      if (mode === 'create') {
        this.currentView = 'create';
      } else if (mode === 'join') {
        this.currentView = 'join';
        this.loadAvailableRooms();
      } else if (mode === 'waiting' && roomId) {
        // Load the room and go to waiting view
        this.loadRoomById(roomId);
      } else {
        // Default to create if no mode specified
        this.currentView = 'create';
      }
    });
    
    // Connect to WebSocket
    this.wsService.connect();
    
    // Subscribe to connection status
    this.subscriptions.push(
      this.wsService.connectionStatus$.subscribe(connected => {
        this.wsConnected = connected;
        if (connected && this.currentRoom) {
          console.log('📡 Subscribing to room:', this.currentRoom.id);
          this.wsService.subscribeToRoom(this.currentRoom.id);
        }
      })
    );
    
    // Subscribe to room updates
    this.subscriptions.push(
      this.wsService.roomUpdates$.subscribe(room => {
        console.log('🔄 Room update received:', room);
        this.currentRoom = room;
        this.updateRoomState();
      })
    );
    
    // Subscribe to game starts
    this.subscriptions.push(
      this.wsService.gameStarts$.subscribe(room => {
        console.log('🎮 Game started, navigating to game page');
        this.router.navigate(['/game', room.id]);
      })
    );
  }
  
  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.currentRoom) {
      this.wsService.unsubscribeFromRoom(this.currentRoom.id);
    }
    this.wsService.disconnect();
  }

  loadAvailableRooms(): void {
    this.roomService.getAvailableRooms().subscribe({
      next: (rooms) => {
        this.availableRooms = rooms;
      },
      error: (error) => {
        console.error('Error loading rooms:', error);
        this.showErrorMessage('Erreur lors du chargement des rooms');
      }
    });
  }

  loadRoomById(roomId: string): void {
    this.roomService.getRoom(roomId).subscribe({
      next: (room) => {
        console.log('✅ Room loaded:', room);
        
        // Check if user is part of the room
        if (!room.players.includes(this.currentUserId)) {
          console.warn('❌ User not in room');
          this.showErrorMessage('Vous ne faites pas partie de cette room');
          this.currentView = 'join';
          return;
        }

        this.currentRoom = room;
        this.updateRoomState();
        this.currentView = 'waiting';
        
        // Subscribe to WebSocket updates for this room
        if (this.wsConnected) {
          this.wsService.subscribeToRoom(room.id);
        }
      },
      error: (error) => {
        console.error('❌ Error loading room:', error);
        this.showErrorMessage('Room introuvable');
        this.currentView = 'join';
      }
    });
  }

  createRoom(): void {
    // Only run in browser
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (!this.createRoomPassword) {
      this.showErrorMessage('Le mot de passe est requis');
      return;
    }
    
    if (this.createRoomMaxPlayers < 2 || this.createRoomMaxPlayers > 8) {
      this.showErrorMessage('Le nombre de joueurs doit être entre 2 et 8');
      return;
    }
    
    const request: RoomCreateRequest = {
      password: this.createRoomPassword,
      maxPlayers: this.createRoomMaxPlayers
    };
    
    this.roomService.createRoom(request).subscribe({
      next: (room) => {
        console.log('✅ Room created:', room);
        this.showSuccessMessage(`Room créée ! ID: ${room.id}`);
        
        // Set the room and switch to waiting view
        this.currentRoom = room;
        this.updateRoomState();
        this.currentView = 'waiting';
        
        // Subscribe to WebSocket updates for this room
        if (this.wsConnected) {
          this.wsService.subscribeToRoom(room.id);
        } else {
          // If not connected yet, wait for connection
          const connectionSub = this.wsService.connectionStatus$.subscribe(connected => {
            if (connected && this.currentRoom) {
              this.wsService.subscribeToRoom(this.currentRoom.id);
              connectionSub.unsubscribe();
            }
          });
        }
        
        // Clear form
        this.createRoomPassword = '';
        this.createRoomMaxPlayers = 4;
      },
      error: (error) => {
        console.error('❌ Error creating room:', error);
        this.showErrorMessage(error.error?.message || 'Erreur lors de la création de la room');
      }
    });
  }

  joinRoom(roomId?: string, password?: string): void {
    // Only run in browser
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const id = roomId || this.joinRoomId;
    const pwd = password || this.joinRoomPassword;
    
    if (!id || !pwd) {
      this.showErrorMessage('ID et mot de passe requis');
      return;
    }
    
    const request: RoomJoinRequest = {
      roomId: id,
      password: pwd
    };
    
    this.roomService.joinRoom(request).subscribe({
      next: (room) => {
        console.log('✅ Joined room:', room.id);
        this.showSuccessMessage('Room rejointe avec succès !');
        
        // Switch to waiting view
        this.currentRoom = room;
        this.updateRoomState();
        this.currentView = 'waiting';
        
        // Subscribe to WebSocket updates for this room
        if (this.wsConnected) {
          this.wsService.subscribeToRoom(room.id);
        }
        
        // Clear form
        this.joinRoomId = '';
        this.joinRoomPassword = '';
      },
      error: (error) => {
        console.error('Error joining room:', error);
        this.showErrorMessage(error.error?.message || 'Mot de passe incorrect ou room introuvable');
      }
    });
  }

  selectRoomToJoin(roomId: string): void {
    this.joinRoomId = roomId;
    this.errorMessage = '';
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
      this.messageTimeout = undefined;
    }
  }

  private showSuccessMessage(message: string, duration: number = 3000): void {
    this.successMessage = message;
    this.errorMessage = '';
    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
    }
    this.messageTimeout = setTimeout(() => {
      this.successMessage = '';
    }, duration);
  }

  private showErrorMessage(message: string, duration: number = 5000): void {
    this.errorMessage = message;
    this.successMessage = '';
    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
    }
    this.messageTimeout = setTimeout(() => {
      this.errorMessage = '';
    }, duration);
  }

  getRoomStatusLabel(status: RoomStatus): string {
    const labels = {
      [RoomStatus.WAITING]: 'En attente',
      [RoomStatus.IN_GAME]: 'En cours',
      [RoomStatus.FINISHED]: 'Terminée'
    };
    return labels[status];
  }

  copyRoomId(roomId?: string): void {
    const id = roomId || this.currentRoom?.id;
    if (id) {
      navigator.clipboard.writeText(id).then(() => {
        this.showSuccessMessage('✅ ID copié dans le presse-papier !', 2000);
      });
    }
  }
  
  startGame(): void {
    if (!this.currentRoom || !this.isAdmin) return;
    
    if (this.currentRoom.players.length < 2) {
      this.showErrorMessage('Au moins 2 joueurs sont requis pour lancer la partie');
      return;
    }
    
    this.roomService.startGame(this.currentRoom.id).subscribe({
      next: () => {
        console.log('🎮 Game start request sent');
        // Navigation will happen via WebSocket gameStarts$ subscription
      },
      error: (error) => {
        console.error('Error starting game:', error);
        this.showErrorMessage(error.error?.message || 'Erreur lors du démarrage de la partie');
      }
    });
  }

  // Handler for room browser component
  handleJoinById(event: { roomId: string, password: string }): void {
    this.joinRoom(event.roomId, event.password);
  }
  
  refreshCurrentRoom(): void {
    if (!this.currentRoom) return;
    
    console.log('🔄 Refreshing current room...');
    this.roomService.getRoom(this.currentRoom.id).subscribe({
      next: (room: Room) => {
        console.log('✅ Room refreshed:', room);
        this.currentRoom = room;
        this.updateRoomState();
        this.showSuccessMessage('✅ Liste des joueurs mise à jour !', 2000);
      },
      error: (error: any) => {
        console.error('❌ Error refreshing room:', error);
        this.showErrorMessage('Erreur lors du rafraîchissement');
      }
    });
  }

  kickPlayer(playerId: string): void {
    if (!this.currentRoom || !this.isAdmin) return;
    
    if (playerId === this.currentUserId) {
      this.showErrorMessage('Vous ne pouvez pas vous retirer vous-même');
      return;
    }
    
    console.log('👢 Kicking player:', playerId);
    this.roomService.kickPlayer(this.currentRoom.id, playerId).subscribe({
      next: (room: Room) => {
        console.log('✅ Player kicked:', playerId);
        this.currentRoom = room;
        this.updateRoomState();
        this.showSuccessMessage('✅ Joueur retiré de la room !', 2000);
      },
      error: (error: any) => {
        console.error('❌ Error kicking player:', error);
        this.showErrorMessage(error.error?.message || 'Erreur lors du retrait du joueur');
      }
    });
  }
  
  leaveRoom(): void {
    if (this.currentRoom) {
      this.wsService.unsubscribeFromRoom(this.currentRoom.id);
      this.currentRoom = null;
    }
    this.router.navigate(['/home']);
  }
  
  backToHome(): void {
    this.router.navigate(['/home']);
  }
  
  switchToJoin(): void {
    this.currentView = 'join';
    this.loadAvailableRooms();
    this.errorMessage = '';
    this.successMessage = '';
  }
  
  switchToCreate(): void {
    this.currentView = 'create';
    this.errorMessage = '';
    this.successMessage = '';
  }
  
  private updateRoomState(): void {
    if (!this.currentRoom) return;
    
    this.isAdmin = this.currentRoom.adminId === this.currentUserId;
    console.log('🔄 Room state updated:');
    console.log('  - isAdmin:', this.isAdmin);
    console.log('  - players:', this.currentRoom.players.length);
  }

  getPlayerPseudo(playerId: string): string {
    if (!this.currentRoom || !this.currentRoom.playerInfos) {
      return playerId.substring(0, 8) + '...';
    }
    const playerInfo = this.currentRoom.playerInfos.find(p => p.id === playerId);
    return playerInfo ? playerInfo.pseudo : playerId.substring(0, 8) + '...';
  }
}
