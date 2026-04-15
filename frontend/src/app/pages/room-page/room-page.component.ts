import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RoomService } from '../../services/room/room.service';
import { WebSocketService } from '../../services/websocket/websocket.service';
import { Room, RoomStatus, RoomCreateRequest, RoomJoinRequest } from '../../models/room/room.model';
import { RoomBrowserComponent } from '../../components/room-browser/room-browser.component';
import { Subscription } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-room-page',
  imports: [CommonModule, FormsModule, RoomBrowserComponent, TranslateModule],
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
  createRoomMaxPlayers: number = 12;
  createRoomStatisticsEnabled: boolean = false;
  createRoomTargetScore: number = 200;
  createRoomTeamMode: boolean = false;
  createRoomNumTeams: number = 2;
  
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
    private translate: TranslateService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    // Nettoyer le localStorage/sessionStorage si navigation vers /home ou /room sans intention de reconnexion
    this.router.events.subscribe(event => {
      // Vérifie si c'est une navigation vers /home ou /room sans queryParams
      if (event && (event as any).url) {
        const url = (event as any).url;
        if ((url === '/home' || url === '/room') && !window.location.search) {
          localStorage.removeItem('currentRoomId');
          localStorage.removeItem('currentRoomPassword');
          sessionStorage.removeItem('currentRoomId');
          sessionStorage.removeItem('currentRoomPassword');
        }
      }
    });
  }

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
        // Vérifier s'il y a une room sauvegardée pour reconnexion
        const savedRoomId = localStorage.getItem('currentRoomId');
        const savedPassword = localStorage.getItem('currentRoomPassword');
        
        if (savedRoomId && savedPassword) {
          this.attemptReconnection(savedRoomId, savedPassword);
        } else {
          // Default to create if no mode specified
          this.currentView = 'create';
        }
      }
    });
    
    // Connect to WebSocket
    this.wsService.connect();
    
    // Subscribe to connection status
    this.subscriptions.push(
      this.wsService.connectionStatus$.subscribe(connected => {
        this.wsConnected = connected;
        if (connected && this.currentRoom) {
          this.wsService.subscribeToRoom(this.currentRoom.id);
        }
      })
    );
    
    // Subscribe to room updates
    this.subscriptions.push(
      this.wsService.roomUpdates$.subscribe(room => {
        this.currentRoom = room;
        this.updateRoomState();
      })
    );
    
    // Subscribe to game starts
    this.subscriptions.push(
      this.wsService.gameStarts$.subscribe(room => {
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
    
    // Ne pas nettoyer le localStorage ici car l'utilisateur peut juste rafraîchir la page
    // Le nettoyage se fera seulement en cas d'échec de reconnexion ou de quitter intentionnel
  }

  loadAvailableRooms(): void {
    this.roomService.getAvailableRooms().subscribe({
      next: (rooms) => {
        this.availableRooms = rooms;
      },
      error: (error) => {
        this.showErrorMessage('room.errors.loadingRooms');
      }
    });
  }

  loadRoomById(roomId: string): void {
    this.roomService.getRoom(roomId).subscribe({
      next: (room) => {
        
        // Check if user is part of the room
        if (!room.players.includes(this.currentUserId)) {
          this.showErrorMessage('room.errors.notInRoom');
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
        this.showErrorMessage('room.errors.roomNotFound');
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
      this.showErrorMessage('room.errors.passwordRequired');
      return;
    }
    
    if (this.createRoomMaxPlayers < 2 || this.createRoomMaxPlayers > 12) {
      this.showErrorMessage('room.errors.invalidMaxPlayers');
      return;
    }
    
    const request: RoomCreateRequest = {
      password: this.createRoomPassword,
      maxPlayers: this.createRoomMaxPlayers,
      statisticsEnabled: this.createRoomStatisticsEnabled,
      targetScore: this.createRoomTargetScore,
      teamMode: this.createRoomTeamMode,
      numTeams: this.createRoomTeamMode ? this.createRoomNumTeams : undefined
    };
    
    this.roomService.createRoom(request).subscribe({
      next: (room) => {
        
        // Sauvegarder dans localStorage pour reconnexion
        localStorage.setItem('currentRoomId', room.id);
        localStorage.setItem('currentRoomPassword', request.password);
        
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
        this.createRoomMaxPlayers = 12;
      },
      error: (error) => {
        this.showErrorMessage(error.error?.message || 'room.errors.createError');
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
      this.showErrorMessage('room.errors.credentialsRequired');
      return;
    }
    
    const request: RoomJoinRequest = {
      roomId: id,
      password: pwd
    };
    
    this.roomService.joinRoom(request).subscribe({
      next: (room) => {
        
        // Sauvegarder dans localStorage pour reconnexion
        localStorage.setItem('currentRoomId', room.id);
        localStorage.setItem('currentRoomPassword', pwd);
        
        // Si la partie a déjà commencé, rediriger vers la game page
        if (room.status === RoomStatus.IN_GAME) {
          this.currentRoom = room;
          this.router.navigate(['/game', room.id]);
        } else {
          // Sinon, switch to waiting view
          this.currentRoom = room;
          this.updateRoomState();
          this.currentView = 'waiting';
          
          // Subscribe to WebSocket updates for this room
          if (this.wsConnected) {
            this.wsService.subscribeToRoom(room.id);
          }
        }
        
        // Clear form
        this.joinRoomId = '';
        this.joinRoomPassword = '';
      },
      error: (error) => {
        this.showErrorMessage(error.error?.message || 'room.errors.joinError');
      }
    });
  }

  selectRoomToJoin(roomId: string): void {
    this.joinRoomId = roomId;
    this.errorMessage = '';
  }

  attemptReconnection(roomId: string, password: string): void {
    
    const request: RoomJoinRequest = {
      roomId: roomId,
      password: password
    };
    
    this.roomService.joinRoom(request).subscribe({
      next: (room) => {
        this.showSuccessMessage('room.success.reconnected');
        
        this.currentRoom = room;
        
        // Si la partie a déjà commencé, rediriger directement vers la game page
        if (room.status === RoomStatus.IN_GAME) {
          this.router.navigate(['/game', room.id]);
        } else {
          // Sinon, switch to waiting view
          this.updateRoomState();
          this.currentView = 'waiting';
          
          // Subscribe to WebSocket updates for this room
          if (this.wsConnected) {
            this.wsService.subscribeToRoom(room.id);
          }
        }
      },
      error: (error) => {
        // Nettoyer le localStorage si la reconnexion échoue (room n'existe plus)
        localStorage.removeItem('currentRoomId');
        localStorage.removeItem('currentRoomPassword');
        this.showErrorMessage('room.errors.reconnectionFailed');
        this.currentView = 'create';
      }
    });
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
      this.messageTimeout = undefined;
    }
  }

  private showSuccessMessage(messageKey: string, duration: number = 3000, params?: any): void {
    this.successMessage = this.translate.instant(messageKey, params);
    this.errorMessage = '';
    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
    }
    this.messageTimeout = setTimeout(() => {
      this.successMessage = '';
    }, duration);
  }

  private showErrorMessage(messageKey: string, duration: number = 5000): void {
    this.errorMessage = this.translate.instant(messageKey);
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
        this.showSuccessMessage('room.success.idCopied', 2000);
      });
    }
  }
  
  startGame(): void {
    if (!this.currentRoom || !this.isAdmin) return;
    
    if (this.currentRoom.players.length < 2) {
      this.showErrorMessage('room.errors.minPlayersRequired');
      return;
    }
    
    this.roomService.startGame(this.currentRoom.id).subscribe({
      next: () => {
        // Navigation will happen via WebSocket gameStarts$ subscription
      },
      error: (error) => {
        this.showErrorMessage(error.error?.message || 'room.errors.startGameError');
      }
    });
  }

  // Handler for room browser component
  handleJoinById(event: { roomId: string, password: string }): void {
    this.joinRoom(event.roomId, event.password);
  }
  
  refreshCurrentRoom(): void {
    if (!this.currentRoom) return;
    
    this.roomService.getRoom(this.currentRoom.id).subscribe({
      next: (room: Room) => {
        this.currentRoom = room;
        this.updateRoomState();
        this.showSuccessMessage('room.success.playersRefreshed', 2000);
      },
      error: (error: any) => {
        this.showErrorMessage('room.errors.refreshError');
      }
    });
  }

  kickPlayer(playerId: string): void {
    if (!this.currentRoom || !this.isAdmin) return;
    
    if (playerId === this.currentUserId) {
      this.showErrorMessage('room.errors.cannotKickYourself');
      return;
    }
    
    this.roomService.kickPlayer(this.currentRoom.id, playerId).subscribe({
      next: (room: Room) => {
        this.currentRoom = room;
        this.updateRoomState();
        this.showSuccessMessage('room.success.playerKicked', 2000);
      },
      error: (error: any) => {
        this.showErrorMessage(error.error?.message || 'room.errors.kickPlayerError');
      }
    });
  }
  
  leaveRoom(): void {
    // Nettoyer le localStorage et sessionStorage quand on quitte intentionnellement
    localStorage.removeItem('currentRoomId');
    localStorage.removeItem('currentRoomPassword');
    sessionStorage.removeItem('currentRoomId');
    sessionStorage.removeItem('currentRoomPassword');
    if (this.currentRoom) {
      this.wsService.unsubscribeFromRoom(this.currentRoom.id);
      this.currentRoom = null;
    }
    this.router.navigate(['/home']);
  }
  
  backToHome(): void {
  // Nettoyer le localStorage et sessionStorage pour éviter la reconnexion automatique
  localStorage.removeItem('currentRoomId');
  localStorage.removeItem('currentRoomPassword');
  sessionStorage.removeItem('currentRoomId');
  sessionStorage.removeItem('currentRoomPassword');
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
  }

  getPlayerPseudo(playerId: string): string {
    if (!this.currentRoom || !this.currentRoom.playerInfos) {
      return playerId.substring(0, 8) + '...';
    }
    const playerInfo = this.currentRoom.playerInfos.find(p => p.id === playerId);
    return playerInfo ? playerInfo.pseudo : playerId.substring(0, 8) + '...';
  }

  // ─── Team Mode Helpers ───

  getTeamIds(): number[] {
    if (!this.currentRoom?.numTeams) return [];
    return Array.from({ length: this.currentRoom.numTeams }, (_, i) => i + 1);
  }

  getTeamPlayers(teamId: number): string[] {
    if (!this.currentRoom?.teamAssignments) return [];
    return Object.entries(this.currentRoom.teamAssignments)
      .filter(([_, tid]) => tid === teamId)
      .map(([pid]) => pid);
  }

  getMyTeam(): number {
    return this.currentRoom?.teamAssignments?.[this.currentUserId] ?? 0;
  }

  joinTeam(teamId: number): void {
    if (!this.currentRoom) return;
    this.roomService.joinTeam(this.currentRoom.id, teamId).subscribe({
      next: (room: Room) => {
        this.currentRoom = room;
        this.updateRoomState();
      },
      error: (err: any) => {
        this.showErrorMessage(err.error?.message || 'room.errors.joinTeamError');
      }
    });
  }
}
