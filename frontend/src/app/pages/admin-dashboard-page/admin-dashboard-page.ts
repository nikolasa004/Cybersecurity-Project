import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AppShell } from '../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../core/config/role-navigation';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './admin-dashboard-page.html',
  styleUrl: './admin-dashboard-page.css'
})
export class AdminDashboardPage {
  navItems = NAVIGATION_BY_ROLE.ADMIN;


  profile = {
    fullName: 'Admin Administrator',
    roleLabel: 'Administrator',
    organization: 'System Administration',
    email: 'admin@pki.local'
  };
}