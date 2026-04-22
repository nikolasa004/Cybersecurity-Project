import { CommonModule } from '@angular/common';
import { Component, inject, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavigationItem } from '../../../core/models/navigation-item';
import { AuthService } from '../../../features/auth/services/auth.service';

@Component({
  selector: 'app-sidebar-nav',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './sidebar-nav.html',
  styleUrl: './sidebar-nav.css'
})
export class SidebarNav {
  @Input() title = '';
  @Input() items: NavigationItem[] = [];

  authService = inject(AuthService);

  onLogout() {
    this.authService.logout();
  }
}