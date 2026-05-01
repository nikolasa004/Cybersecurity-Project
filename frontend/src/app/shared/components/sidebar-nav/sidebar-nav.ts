import { CommonModule } from '@angular/common';
import { Component, inject, Input } from '@angular/core';
import { RouterLink, RouterModule } from '@angular/router';
import { NavigationItem } from '../../../core/models/navigation-item';
import { AuthService } from '../../../features/auth/services/auth.service';
import { ConfirmDialog } from '../confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-sidebar-nav',
  standalone: true,
  imports: [CommonModule, RouterLink, ConfirmDialog],
  templateUrl: './sidebar-nav.html',
  styleUrl: './sidebar-nav.css'
})
export class SidebarNav {
  @Input() title = '';
  @Input() items: NavigationItem[] = [];

  authService = inject(AuthService);

  showLogoutDialog = false;

  onLogout(): void {
    this.showLogoutDialog = true;
  }

  confirmLogout(): void {
    this.showLogoutDialog = false;
    this.authService.logout();
  }

  closeLogoutDialog(): void {
    this.showLogoutDialog = false;
  }
}