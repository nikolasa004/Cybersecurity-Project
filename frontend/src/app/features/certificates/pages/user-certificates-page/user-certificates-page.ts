import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { CertificatesService } from '../../services/certificates.service';
import { CertificateListItem } from '../../models/certificate-list.model';
import { AuthService } from '../../../auth/services/auth.service';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-user-certificates-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './user-certificates-page.html',
  styleUrl: './user-certificates-page.css'
})
export class UserCertificatesPage implements OnInit {
  private certificatesService = inject(CertificatesService);
  private authService = inject(AuthService);

  navItems = NAVIGATION_BY_ROLE.USER;

  profile = {
    fullName: 'User session',
    roleLabel: 'User',
    organization: this.authService.getUserOrganizationName() || 'Unknown Organization'
  };

  certificates: CertificateListItem[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.loading = true;
    this.errorMessage = '';

    this.certificatesService.getUserCertificates().subscribe({
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
      case 'ACTIVE': return 'Active';
      case 'REVOKED': return 'Revoked';
      case 'EXPIRED': return 'Expired';
      default: return status;
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'END_ENTITY': return 'End-Entity';
      default: return type;
    }
  }

  downloadPem(certificateId: number, commonName: string): void {
    this.certificatesService.downloadPem(certificateId, 'USER').subscribe({
      next: (response) => {
        const blob = new Blob([response.pemContent], { type: 'text/plain;charset=utf-8' });
        saveAs(blob, `${commonName || 'certificate'}.pem`);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to download .PEM certificate.';
      }
    });
  }
}