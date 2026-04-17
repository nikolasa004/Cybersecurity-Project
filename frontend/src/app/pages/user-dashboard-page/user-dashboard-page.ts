import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AppShell } from '../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../core/config/role-navigation';

@Component({
  selector: 'app-user-dashboard-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './user-dashboard-page.html',
  styleUrl: './user-dashboard-page.css'
})
export class UserDashboardPage {
  navItems = NAVIGATION_BY_ROLE.USER;

  profile = {
    fullName: 'Nikola Savic',
    roleLabel: 'User',
    organization: 'Komsiluk Soft',
    email: 'nikola@komsiluk.rs'
  };
}