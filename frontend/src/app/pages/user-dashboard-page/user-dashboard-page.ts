import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AppShell } from '../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../core/config/role-navigation';
import { CurrentUserService } from '../../features/users/services/current-user.service';

@Component({
  selector: 'app-user-dashboard-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './user-dashboard-page.html',
  styleUrl: './user-dashboard-page.css'
})
export class UserDashboardPage implements OnInit {
  private currentUserService = inject(CurrentUserService);

  navItems = NAVIGATION_BY_ROLE.USER;

  profile = {
    fullName: 'Loading...',
    roleLabel: 'User',
    organization: 'Loading...',
    email: 'Loading...'
  };

  requestOptions = [
    'Request end-entity certificates through the available user workflows',
    'Upload CSR files when you already have your own generated key pair',
    'Work only with certificate requests and certificates that belong to your own account'
  ];

  certificateActions = [
    'Review only your own end-entity certificates',
    'Download certificate material when it is available',
    'Access certificate-related actions through your user navigation',
    'Revoke your own certificates when necessary'
  ];

  securityNotes = [
    'Your account uses role-based access and organization scope within the system.',
    'Sensitive certificate actions are limited to resources assigned to your user account.',
    'Additional security features such as activation, recovery and multi-factor authentication are handled through your account workflows.'
  ];

  ngOnInit(): void {
    this.currentUserService.currentUser$.subscribe((user) => {
      if (!user) {
        return;
      }

      this.profile = {
        fullName: user.fullName,
        roleLabel: 'User',
        organization: user.organizationName || 'No organization',
        email: user.email
      };
    });
  }
}