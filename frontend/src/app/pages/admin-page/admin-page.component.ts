import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthenticationService } from '../../services/authentication/authentification.service';
import {
  AdminService,
  UserAdminDTO,
  RoomAdminDTO,
  GameHistoryAdminDTO,
  AdminStatsDTO,
} from '../../services/admin/admin.service';

type Tab = 'dashboard' | 'users' | 'rooms' | 'history';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './admin-page.component.html',
  styleUrl: './admin-page.component.scss',
})
export class AdminPageComponent implements OnInit {
  private adminService = inject(AdminService);
  private authService = inject(AuthenticationService);
  private router = inject(Router);

  activeTab: Tab = 'dashboard';

  // Dashboard
  stats: AdminStatsDTO | null = null;

  // Users
  users: UserAdminDTO[] = [];
  filteredUsers: UserAdminDTO[] = [];
  userSearch = '';
  isSuperAdmin = false;
  currentUserId = '';

  // Rooms
  rooms: RoomAdminDTO[] = [];
  filteredRooms: RoomAdminDTO[] = [];
  roomStatusFilter = '';

  // History
  history: GameHistoryAdminDTO[] = [];
  filteredHistory: GameHistoryAdminDTO[] = [];
  historyRoomFilter = '';

  // Confirmation modal
  showConfirmModal = false;
  confirmMessage = '';
  confirmAction: (() => void) | null = null;

  // Reset password modal
  showResetModal = false;
  resetTargetId = '';
  resetTargetPseudo = '';
  newPassword = '';
  resetError = '';

  // Role change modal
  showRoleModal = false;
  roleTargetId = '';
  roleTargetPseudo = '';
  selectedRole = 'USER';

  // Loading / error
  loading = false;
  errorMsg = '';
  successMsg = '';

  ngOnInit(): void {
    this.isSuperAdmin = this.authService.isSuperAdmin();
    this.currentUserId = this.authService.getToken() ?? '';
    this.loadTab('dashboard');
  }

  setTab(tab: Tab): void {
    this.activeTab = tab;
    this.errorMsg = '';
    this.successMsg = '';
    this.loadTab(tab);
  }

  private loadTab(tab: Tab): void {
    if (tab === 'dashboard') this.loadStats();
    else if (tab === 'users') this.loadUsers();
    else if (tab === 'rooms') this.loadRooms();
    else if (tab === 'history') this.loadHistory();
  }

  // ─── Dashboard ────────────────────────────────────────────────────────────

  loadStats(): void {
    this.loading = true;
    this.adminService.getStats().subscribe({
      next: (s) => { this.stats = s; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  // ─── Users ────────────────────────────────────────────────────────────────

  loadUsers(): void {
    this.loading = true;
    this.adminService.getUsers().subscribe({
      next: (u) => {
        this.users = u;
        this.applyUserFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  applyUserFilter(): void {
    const q = this.userSearch.toLowerCase();
    this.filteredUsers = q
      ? this.users.filter(u => u.pseudo.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
      : [...this.users];
  }

  openRoleModal(user: UserAdminDTO): void {
    this.roleTargetId = user.id;
    this.roleTargetPseudo = user.pseudo;
    this.selectedRole = user.role;
    this.showRoleModal = true;
  }

  confirmRoleChange(): void {
    this.adminService.updateUserRole(this.roleTargetId, this.selectedRole).subscribe({
      next: () => {
        this.showRoleModal = false;
        this.showSuccess('Rôle mis à jour');
        this.loadUsers();
      },
      error: (e: any) => { this.showError(e.error?.message || 'Erreur'); },
    });
  }

  openResetModal(user: UserAdminDTO): void {
    this.resetTargetId = user.id;
    this.resetTargetPseudo = user.pseudo;
    this.newPassword = '';
    this.resetError = '';
    this.showResetModal = true;
  }

  confirmResetPassword(): void {
    if (!this.newPassword || this.newPassword.length < 4) {
      this.resetError = 'Minimum 4 caractères';
      return;
    }
    this.adminService.resetUserPassword(this.resetTargetId, this.newPassword).subscribe({
      next: () => {
        this.showResetModal = false;
        this.showSuccess('Mot de passe réinitialisé');
      },
      error: (e: any) => { this.resetError = e.error?.message || 'Erreur'; },
    });
  }

  confirmDeleteUser(user: UserAdminDTO): void {
    this.openConfirm(
      `Supprimer l'utilisateur "${user.pseudo}" ? Cette action est irréversible.`,
      () => {
        this.adminService.deleteUser(user.id).subscribe({
          next: () => { this.showSuccess('Utilisateur supprimé'); this.loadUsers(); },
          error: (e: any) => { this.showError(e.error?.message || 'Erreur'); },
        });
      }
    );
  }

  getRoleBadgeClass(role: string): string {
    if (role === 'SUPERADMIN') return 'badge-superadmin';
    if (role === 'ADMIN') return 'badge-admin';
    return 'badge-user';
  }

  // ─── Rooms ────────────────────────────────────────────────────────────────

  loadRooms(): void {
    this.loading = true;
    this.adminService.getRooms().subscribe({
      next: (r) => {
        this.rooms = r;
        this.applyRoomFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  applyRoomFilter(): void {
    this.filteredRooms = this.roomStatusFilter
      ? this.rooms.filter(r => r.status === this.roomStatusFilter)
      : [...this.rooms];
  }

  confirmDeleteRoom(room: RoomAdminDTO): void {
    this.openConfirm(
      `Supprimer la room "${room.id}" ? Cette action est irréversible.`,
      () => {
        this.adminService.deleteRoom(room.id).subscribe({
          next: () => { this.showSuccess('Room supprimée'); this.loadRooms(); },
          error: (e: any) => { this.showError(e.error?.message || 'Erreur'); },
        });
      }
    );
  }

  getStatusClass(status: string): string {
    if (status === 'IN_GAME') return 'status-active';
    if (status === 'WAITING') return 'status-waiting';
    return 'status-finished';
  }

  // ─── History ──────────────────────────────────────────────────────────────

  loadHistory(): void {
    this.loading = true;
    this.adminService.getHistory().subscribe({
      next: (h) => {
        this.history = h;
        this.applyHistoryFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  applyHistoryFilter(): void {
    const q = this.historyRoomFilter.toLowerCase();
    this.filteredHistory = q
      ? this.history.filter(h => h.roomId.toLowerCase().includes(q))
      : [...this.history];
  }

  confirmDeleteHistory(entry: GameHistoryAdminDTO): void {
    this.openConfirm(
      `Supprimer cet historique de partie (room ${entry.roomId}) ?`,
      () => {
        this.adminService.deleteHistory(entry.id).subscribe({
          next: () => { this.showSuccess('Historique supprimé'); this.loadHistory(); },
          error: (e: any) => { this.showError(e.error?.message || 'Erreur'); },
        });
      }
    );
  }

  confirmDeleteHistoryByRoom(roomId: string): void {
    if (!roomId) return;
    this.openConfirm(
      `Supprimer tout l'historique de la room "${roomId}" ?`,
      () => {
        this.adminService.deleteHistoryByRoom(roomId).subscribe({
          next: () => { this.showSuccess('Historique supprimé'); this.loadHistory(); },
          error: (e: any) => { this.showError(e.error?.message || 'Erreur'); },
        });
      }
    );
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleString();
  }

  // ─── Confirm modal ────────────────────────────────────────────────────────

  private openConfirm(message: string, action: () => void): void {
    this.confirmMessage = message;
    this.confirmAction = action;
    this.showConfirmModal = true;
  }

  onConfirm(): void {
    this.showConfirmModal = false;
    if (this.confirmAction) this.confirmAction();
    this.confirmAction = null;
  }

  onCancelConfirm(): void {
    this.showConfirmModal = false;
    this.confirmAction = null;
  }

  // ─── Feedback ─────────────────────────────────────────────────────────────

  private showSuccess(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => (this.successMsg = ''), 3000);
  }

  private showError(msg: string): void {
    this.errorMsg = msg;
    setTimeout(() => (this.errorMsg = ''), 4000);
  }

  goBack(): void {
    this.router.navigate(['/home']);
  }
}
