import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { CertificatesService } from '../../services/certificates.service';
import { CertificateListItem } from '../../models/certificate-list.model';
import { AuthService } from '../../../auth/services/auth.service';
import { CurrentUserService } from '../../../users/services/current-user.service';
import { saveAs } from 'file-saver';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-certificates-page',
  standalone: true,
  imports: [CommonModule, AppShell, RouterLink,FormsModule],
  templateUrl: './admin-certificates-page.html',
  styleUrl: './admin-certificates-page.css'
})
export class AdminCertificatesPage implements OnInit {
  private certificatesService = inject(CertificatesService);
  private authService = inject(AuthService);
  private currentUserService = inject(CurrentUserService);

  showPasswordModal = false;

  downloadCertificateId: number | null = null;
  downloadCertificateName = '';

  pkcs12Password = '';

  isAdmin = this.authService.getRole() === 'ADMIN';
  navItems = this.isAdmin ? NAVIGATION_BY_ROLE.ADMIN : NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: 'Loading...',
    roleLabel: this.isAdmin ? 'Administrator' : 'CA User',
    organization: 'Loading...'
  };

  issueRoute = this.isAdmin ? '/admin/certificates/issue' : '/ca/certificates/issue';
  sidebarTitle = this.isAdmin ? 'Admin panel' : 'CA panel';

  certificates: CertificateListItem[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadCertificates();
    this.loadCurrentUser();
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

  private loadCurrentUser(): void {
    this.currentUserService.currentUser$.subscribe((user) => {
      if (!user) {
        return;
      }

      this.sidebarTitle = this.isAdmin ? 'Admin panel' : 'CA panel';
      this.profile = {
        fullName: user.fullName,
        roleLabel: this.isAdmin ? 'Administrator' : 'CA User',
        organization: user.organizationName || (this.isAdmin ? 'System Administration' : 'No organization')
      };
    });
  }

  


  downloadWithKey(
    certificateId: number,
    commonName: string
  ): void {

    this.downloadCertificateId = certificateId;
    this.downloadCertificateName = commonName;

    this.pkcs12Password = '';

    this.showPasswordModal = true;
  }

  confirmDownload(): void {

    if (this.downloadCertificateId == null) {
      return;
    }

    this.certificatesService
      .downloadWithKey(
        this.isAdmin,
        this.downloadCertificateId,
        this.pkcs12Password
      )
      .subscribe({

        next: (data: ArrayBuffer) => {

          this.downloadCertificate(
            data,
            this.downloadCertificateName
          );

          this.closePasswordModal();
        },

        error: (err) => {

          this.errorMessage =
            err?.error?.message
            || 'Failed to download certificate.';

          this.closePasswordModal();
        }
      });
  }

  closePasswordModal(): void {

    this.showPasswordModal = false;

    this.downloadCertificateId = null;

    this.downloadCertificateName = '';

    this.pkcs12Password = '';
  }

  private downloadCertificate(data: ArrayBuffer, cn: string): void {
    const blob = new Blob([data], {
      type: 'application/x-pkcs12'
    });

    saveAs(blob, `${cn || 'certificate'}.p12`);
  }

  downloadPem(certificateId: number, commonName: string): void {
    const currentRole: 'ADMIN' | 'CA_USER' | 'USER' = this.isAdmin ? 'ADMIN' : 'CA_USER';

    this.certificatesService.downloadPem(certificateId, currentRole).subscribe({
      next: (response) => {
        const blob = new Blob([response.pemContent], { type: 'text/plain;charset=utf-8' });
        saveAs(blob, `${commonName || 'certificate'}.pem`);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to download .PEM certificate.';
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