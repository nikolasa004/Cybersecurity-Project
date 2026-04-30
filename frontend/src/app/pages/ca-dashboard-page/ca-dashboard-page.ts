import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AppShell } from '../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../core/config/role-navigation';
import { CurrentUserService } from '../../features/users/services/current-user.service';

@Component({
  selector: 'app-ca-dashboard-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './ca-dashboard-page.html',
  styleUrl: './ca-dashboard-page.css'
})
export class CaDashboardPage implements OnInit {
  private currentUserService = inject(CurrentUserService);

  navItems = NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: 'Loading...',
    roleLabel: 'CA User',
    organization: 'Loading...',
    email: 'Loading...'
  };

  focusAreas = [
    'Issue intermediate and end-entity certificates within your organization',
    'Work only with organization-scoped certificate data',
    'Use available certificate chains assigned to your organization',
    'Review certificates that belong to your organization'
  ];

  issuingRules = [
    'Certificate issuing is limited to the organization assigned to your account.',
    'Issuer validation must be satisfied before any certificate can be issued.',
    'Certificate validity must remain within the validity period of the selected issuer.',
    'Only organization-relevant certificate workflows are available from this workspace.'
  ];

  accessNotes = [
    'You can review only the certificates that belong to your organization.',
    'Administrator-level certificate visibility is not available from this dashboard.',
    'Organization scope and certificate permissions are enforced through your assigned CA role.'
  ];

  ngOnInit(): void {
    this.currentUserService.currentUser$.subscribe((user) => {
      if (!user) {
        return;
      }

      this.profile = {
        fullName: user.fullName,
        roleLabel: 'CA User',
        organization: user.organizationName || 'No organization',
        email: user.email
      };
    });
  }
}