import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AppShell } from '../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../core/config/role-navigation';
import { CurrentUserService } from '../../features/users/services/current-user.service';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './admin-dashboard-page.html',
  styleUrl: './admin-dashboard-page.css'
})
export class AdminDashboardPage implements OnInit {
  private currentUserService = inject(CurrentUserService);

  navItems = NAVIGATION_BY_ROLE.ADMIN;

  profile = {
    fullName: 'Loading...',
    roleLabel: 'Administrator',
    organization: 'Loading...',
    email: 'Loading...'
  };

  focusAreas = [
    'Register and manage organizations',
    'Register and manage CA users',
    'Issue root, intermediate and end-entity certificates',
    'Review certificates across the entire system',
    'Revoke certificates and manage the revocation list'
  ];

  certificateCapabilities = [
    'Use any valid certificate chain in the system for issuing',
    'Review certificate validity, status and ownership',
    'Manage the certificate lifecycle'
  ];

  managementNotes = [
    'Organization management is available through the dedicated organizations page.',
    'CA user registration is available through the CA users page.',
    'Certificate issuing and certificate overview are available from the certificate pages.',
    'Revocation management is available from the revocation page.'
  ];

  ngOnInit(): void {
    this.currentUserService.currentUser$.subscribe((user) => {
      if (!user) {
        return;
      }

      this.profile = {
        fullName: user.fullName,
        roleLabel: 'Administrator',
        organization: user.organizationName || 'System Administration',
        email: user.email
      };
    });
  }
}