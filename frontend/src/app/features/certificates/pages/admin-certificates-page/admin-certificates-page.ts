import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { CertificatesService } from '../../services/certificates.service';
import { CertificateListItem } from '../../models/certificate-list.model';
import { AuthService } from '../../../auth/services/auth.service';

@Component({
  selector: 'app-admin-certificates-page',
  standalone: true,
  imports: [CommonModule, AppShell, RouterLink],
  templateUrl: './admin-certificates-page.html',
  styleUrl: './admin-certificates-page.css'
})
export class AdminCertificatesPage implements OnInit {
  private certificatesService = inject(CertificatesService);
  private authService = inject(AuthService);

  isAdmin = this.authService.getRole() === 'ADMIN';
  navItems = this.isAdmin ? NAVIGATION_BY_ROLE.ADMIN : NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: this.isAdmin ? 'Administrator session' : 'CA User session',
    roleLabel: this.isAdmin ? 'Administrator' : 'CA Representative',
    organization: this.isAdmin ? 'PKI system' : this.authService.getUserOrganizationName() || 'Unknown Organization'
  };

  issueRoute = this.isAdmin ? '/admin/certificates/issue' : '/ca/certificates/issue';
  sidebarTitle = this.isAdmin ? 'Admin panel' : 'CA panel';

  certificates: CertificateListItem[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.loading = true;
    this.errorMessage = '';

    this.certificatesService.getAll(this.isAdmin).subscribe({
      next: (certificates) => {
        this.certificates = certificates;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Failed to load certificates.';
      }
    });
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'Active';
      case 'REVOKED':
        return 'Revoked';
      case 'EXPIRED':
        return 'Expired';
      default:
        return status;
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'ROOT':
        return 'Root';
      case 'INTERMEDIATE':
        return 'Intermediate';
      case 'END_ENTITY':
        return 'End-Entity';
      default:
        return type;
    }
  }
}