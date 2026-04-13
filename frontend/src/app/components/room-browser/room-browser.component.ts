import { Component, EventEmitter, Input, Output, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Room, RoomStatus } from '../../models/room/room.model';

@Component({
  selector: 'app-room-browser',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './room-browser.component.html',
  styleUrls: ['./room-browser.component.scss']
})
export class RoomBrowserComponent implements OnDestroy {
  @Input() availableRooms: Room[] = [];
  @Input() currentUserId: string = '';
  @Output() onJoinById = new EventEmitter<{ roomId: string, password: string }>();
  @Output() onRefreshRooms = new EventEmitter<void>();

  private refreshInterval: any;

  joinRoomId: string = '';
  joinRoomPassword: string = '';
  selectedRoomId: string | null = null;

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  ngOnInit(): void {
    this.startAutoRefresh();
  }

  private startAutoRefresh(): void {
    this.stopAutoRefresh();
    this.refreshRooms();
    this.refreshInterval = setInterval(() => this.refreshRooms(), 5000);
  }

  private stopAutoRefresh(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  /** Rooms ouvertes (WAITING) où le joueur n'est pas encore dedans */
  get openRooms(): Room[] {
    return this.availableRooms.filter(r =>
      r.status === RoomStatus.WAITING &&
      !(r.players ?? []).includes(this.currentUserId)
    );
  }

  /** Rooms où le joueur est déjà présent */
  get myRooms(): Room[] {
    return this.availableRooms.filter(r =>
      (r.players ?? []).includes(this.currentUserId)
    );
  }

  selectRoom(roomId: string): void {
    this.selectedRoomId = roomId;
    this.joinRoomId = roomId;
  }

  joinById(): void {
    if (this.joinRoomId && this.joinRoomPassword) {
      this.onJoinById.emit({ roomId: this.joinRoomId, password: this.joinRoomPassword });
    }
  }

  refreshRooms(): void {
    this.onRefreshRooms.emit();
  }

  clearRoomId(): void {
    this.joinRoomId = '';
    this.selectedRoomId = null;
  }
}
