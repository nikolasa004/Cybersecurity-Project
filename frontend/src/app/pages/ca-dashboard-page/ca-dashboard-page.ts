import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AppShell } from '../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../core/config/role-navigation';

@Component({
  selector: 'app-ca-dashboard-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './ca-dashboard-page.html',
  styleUrl: './ca-dashboard-page.css'
})
export class CaDashboardPage {
  navItems = NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: 'Marko Markovic',
    roleLabel: 'CA User',
    organization: 'Komsiluk Soft',
    email: 'marko@komsiluk.rs'
  };
}