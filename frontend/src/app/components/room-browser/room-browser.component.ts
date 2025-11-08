import { Component, EventEmitter, Input, Output, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Room } from '../../models/room/room.model';

@Component({
  selector: 'app-room-browser',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './room-browser.component.html',
  styleUrls: ['./room-browser.component.scss']
})
export class RoomBrowserComponent implements OnDestroy {
  @Input() availableRooms: Room[] = [];
  @Output() onJoinById = new EventEmitter<{ roomId: string, password: string }>();
  @Output() onSelectRoom = new EventEmitter<string>();
  @Output() onRefreshRooms = new EventEmitter<void>();

  // Tab management
  activeTab: 'byId' | 'browse' = 'browse';
  
  // Auto-refresh interval
  private refreshInterval: any;

  // Join by ID
  joinRoomId: string = '';
  joinRoomPassword: string = '';

  // Browse rooms
  searchQuery: string = '';
  selectedRoomId: string | null = null;

  switchTab(tab: 'byId' | 'browse'): void {
    this.activeTab = tab;
    this.clearSelection();
    
    // Start auto-refresh when switching to browse tab
    if (tab === 'browse') {
      this.startAutoRefresh();
    } else {
      this.stopAutoRefresh();
    }
  }
  
  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }
  
  private startAutoRefresh(): void {
    // Clear any existing interval
    this.stopAutoRefresh();
    
    // Refresh immediately
    this.refreshRooms();
    
    // Then refresh every 5 seconds
    this.refreshInterval = setInterval(() => {
      this.refreshRooms();
    }, 5000);
  }
  
  private stopAutoRefresh(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  clearSelection(): void {
    this.selectedRoomId = null;
    this.joinRoomId = '';
    this.joinRoomPassword = '';
  }

  joinById(): void {
    if (this.joinRoomId && this.joinRoomPassword) {
      this.onJoinById.emit({
        roomId: this.joinRoomId,
        password: this.joinRoomPassword
      });
    }
  }

  selectRoom(roomId: string): void {
    this.selectedRoomId = roomId;
    // Switch to "Join by ID" tab and fill the room ID
    this.activeTab = 'byId';
    this.joinRoomId = roomId;
    // Focus on password input would be nice but requires ViewChild
  }

  refreshRooms(): void {
    this.onRefreshRooms.emit();
  }

  get filteredRooms(): Room[] {
    if (!this.searchQuery) {
      return this.availableRooms;
    }
    
    const query = this.searchQuery.toLowerCase();
    return this.availableRooms.filter(room => 
      room.id.toLowerCase().includes(query) ||
      room.status.toLowerCase().includes(query)
    );
  }

  clearRoomId(): void {
    this.joinRoomId = '';
  }

  clearSearch(): void {
    this.searchQuery = '';
  }
}
