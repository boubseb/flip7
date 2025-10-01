import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RoomService } from '../../services/room/room.service';
import { Room, RoomStatus, RoomCreateRequest, RoomJoinRequest } from '../../models/room/room.model';

@Component({
  selector: 'app-room-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './room-page.component.html',
  styleUrl: './room-page.component.scss'
})
export class RoomPageComponent implements OnInit {
  // Available rooms
  availableRooms: Room[] = [];
  
  // Create room form
  createRoomPassword: string = '';
  createRoomMaxPlayers: number = 4;
  
  // Join room form
  joinRoomId: string = '';
  joinRoomPassword: string = '';
  
  // Error handling
  errorMessage: string = '';
  successMessage: string = '';
  
  constructor(
    private roomService: RoomService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Only check localStorage in browser (not during SSR)
    if (isPlatformBrowser(this.platformId)) {
      const token = localStorage.getItem('access_token');
      if (!token) {
        this.router.navigate(['/login']);
        return;
      }
      
      this.loadAvailableRooms();
    }
  }

  loadAvailableRooms(): void {
    this.roomService.getAvailableRooms().subscribe({
      next: (rooms) => {
        this.availableRooms = rooms;
      },
      error: (error) => {
        console.error('Error loading rooms:', error);
        this.errorMessage = 'Erreur lors du chargement des rooms';
      }
    });
  }

  createRoom(): void {
    if (!this.createRoomPassword) {
      this.errorMessage = 'Le mot de passe est requis';
      return;
    }
    
    if (this.createRoomMaxPlayers < 2 || this.createRoomMaxPlayers > 8) {
      this.errorMessage = 'Le nombre de joueurs doit être entre 2 et 8';
      return;
    }
    
    const request: RoomCreateRequest = {
      password: this.createRoomPassword,
      maxPlayers: this.createRoomMaxPlayers
    };
    
    this.roomService.createRoom(request).subscribe({
      next: (response) => {
        console.log('Room created:', response.roomId);
        this.successMessage = `Room créée ! ID: ${response.roomId}`;
        // Navigate to game page with the room ID
        this.router.navigate(['/game', response.roomId]);
      },
      error: (error) => {
        console.error('Error creating room:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la création de la room';
      }
    });
  }

  joinRoom(roomId?: string, password?: string): void {
    const id = roomId || this.joinRoomId;
    const pwd = password || this.joinRoomPassword;
    
    if (!id || !pwd) {
      this.errorMessage = 'ID et mot de passe requis';
      return;
    }
    
    const request: RoomJoinRequest = {
      roomId: id,
      password: pwd
    };
    
    this.roomService.joinRoom(request).subscribe({
      next: (room) => {
        console.log('Joined room:', room.id);
        this.successMessage = 'Room rejointe avec succès !';
        // Navigate to game page with the room ID
        this.router.navigate(['/game', room.id]);
      },
      error: (error) => {
        console.error('Error joining room:', error);
        this.errorMessage = error.error?.message || 'Mot de passe incorrect ou room introuvable';
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
  }

  getRoomStatusLabel(status: RoomStatus): string {
    const labels = {
      [RoomStatus.WAITING]: 'En attente',
      [RoomStatus.IN_GAME]: 'En cours',
      [RoomStatus.FINISHED]: 'Terminée'
    };
    return labels[status];
  }

  copyRoomId(roomId: string): void {
    navigator.clipboard.writeText(roomId).then(() => {
      this.successMessage = 'ID copié dans le presse-papier !';
      setTimeout(() => this.successMessage = '', 2000);
    });
  }
}
