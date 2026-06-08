import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { CertificatesService } from '../../services/certificates.service';
import { CertificateListItem } from '../../models/certificate-list.model';
import { AuthService } from '../../../auth/services/auth.service';
import { saveAs } from 'file-saver';
import { FormsModule } from '@angular/forms';
import { RevokeCertificateRequest } from '../../models/revoke-certificate-request.model';

type RevocationReason =
  | 'UNSPECIFIED'
  | 'KEY_COMPROMISE'
  | 'CA_COMPROMISE'
  | 'AFFILIATION_CHANGED'
  | 'SUPERSEDED'
  | 'CESSATION_OF_OPERATION'
  | 'CERTIFICATE_HOLD'
  | 'PRIVILEGE_WITHDRAWN'
  | 'AA_COMPROMISE';

@Component({
  selector: 'app-user-certificates-page',
  standalone: true,
  imports: [CommonModule, AppShell, FormsModule],
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
  successMessage = '';

  showRevokeModal = false;
  revokeCertificateId: number | null = null;
  revokeCertificateName = '';
  revokeReason: RevocationReason = 'UNSPECIFIED';
  revoking = false;

  readonly revokeReasonOptions: { value: RevocationReason; label: string }[] = [
    { value: 'UNSPECIFIED', label: 'Unspecified' },
    { value: 'KEY_COMPROMISE', label: 'Key compromise' },
    { value: 'CA_COMPROMISE', label: 'CA compromise' },
    { value: 'AFFILIATION_CHANGED', label: 'Affiliation changed' },
    { value: 'SUPERSEDED', label: 'Superseded' },
    { value: 'CESSATION_OF_OPERATION', label: 'Cessation of operation' },
    { value: 'CERTIFICATE_HOLD', label: 'Certificate hold' },
    { value: 'PRIVILEGE_WITHDRAWN', label: 'Privilege withdrawn' },
    { value: 'AA_COMPROMISE', label: 'AA compromise' }
  ];

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

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

  openRevokeModal(certificate: CertificateListItem): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.revokeCertificateId = certificate.id;
    this.revokeCertificateName = certificate.subjectCommonName || `certificate-${certificate.id}`;
    this.revokeReason = 'UNSPECIFIED';
    this.showRevokeModal = true;
  }

  closeRevokeModal(): void {
    this.showRevokeModal = false;
    this.revokeCertificateId = null;
    this.revokeCertificateName = '';
    this.revokeReason = 'UNSPECIFIED';
    this.revoking = false;
  }

  confirmRevoke(): void {
    if (this.revokeCertificateId == null) {
      return;
    }

    this.revoking = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload: RevokeCertificateRequest = {
      reason: this.revokeReason
    };

    this.certificatesService.revokeAsUser(this.revokeCertificateId, payload).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Certificate revoked successfully.';
        this.closeRevokeModal();
        this.loadCertificates();
      },
      error: (err) => {
        this.revoking = false;
        this.errorMessage = err?.error?.message || 'Failed to revoke certificate.';
      }
    });
  }

  canRevoke(certificate: CertificateListItem): boolean {
    return certificate.status === 'ACTIVE';
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
      case 'END_ENTITY':
        return 'End-Entity';
      case 'ROOT':
        return 'Root';
      case 'INTERMEDIATE':
        return 'Intermediate';
      default:
        return type;
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