import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { AuthService } from '../../../auth/services/auth.service';
import { CurrentUserService } from '../../../users/services/current-user.service';
import { CertificatesService } from '../../services/certificates.service';
import { OcspStatusResponse } from '../../models/ocsp-status.model';
import { CrlResponse } from '../../models/crl.model';

@Component({
  selector: 'app-revocation-tools-page',
  standalone: true,
  imports: [CommonModule, FormsModule, AppShell],
  templateUrl: './revocation-tools-page.html',
  styleUrl: './revocation-tools-page.css'
})
export class RevocationToolsPage {
  private authService = inject(AuthService);
  private currentUserService = inject(CurrentUserService);
  private certificatesService = inject(CertificatesService);

  role = this.authService.getRole() as 'ADMIN' | 'CA_USER' | 'USER';

  navItems =
    this.role === 'ADMIN'
      ? NAVIGATION_BY_ROLE.ADMIN
      : this.role === 'CA_USER'
        ? NAVIGATION_BY_ROLE.CA_USER
        : NAVIGATION_BY_ROLE.USER;

  sidebarTitle =
    this.role === 'ADMIN'
      ? 'Admin panel'
      : this.role === 'CA_USER'
        ? 'CA panel'
        : 'User panel';

  profile = {
    fullName: 'Loading...',
    roleLabel:
      this.role === 'ADMIN'
        ? 'Administrator'
        : this.role === 'CA_USER'
          ? 'CA User'
          : 'User',
    organization: 'Loading...'
  };

  ocspSerialNumber = '';
  ocspLoading = false;
  ocspError = '';
  ocspResult: OcspStatusResponse | null = null;

  crlIssuerSerialNumber = '';
  crlLoading = false;
  crlError = '';
  crlResult: CrlResponse | null = null;

  constructor() {
    this.currentUserService.currentUser$.subscribe((user) => {
      if (!user) {
        return;
      }

      this.profile = {
        fullName: user.fullName,
        roleLabel:
          this.role === 'ADMIN'
            ? 'Administrator'
            : this.role === 'CA_USER'
              ? 'CA User'
              : 'User',
        organization:
          user.organizationName ||
          (this.role === 'ADMIN' ? 'System Administration' : 'No organization')
      };
    });
  }

  checkOcsp(): void {
    this.ocspError = '';
    this.ocspResult = null;

    if (!this.ocspSerialNumber.trim()) {
      this.ocspError = 'Serial number is required.';
      return;
    }

    this.ocspLoading = true;

    this.certificatesService.getOcspStatus(this.ocspSerialNumber.trim()).subscribe({
      next: (response) => {
        this.ocspResult = response;
        this.ocspLoading = false;
      },
      error: (err) => {
        this.ocspLoading = false;
        this.ocspError = err?.error?.message || 'Failed to load OCSP status.';
      }
    });
  }

  loadCrl(): void {
    this.crlError = '';
    this.crlResult = null;

    if (!this.crlIssuerSerialNumber.trim()) {
      this.crlError = 'Issuer serial number is required.';
      return;
    }

    this.crlLoading = true;

    this.certificatesService.getCrl(this.crlIssuerSerialNumber.trim()).subscribe({
      next: (response) => {
        this.crlResult = response;
        this.crlLoading = false;
      },
      error: (err) => {
        this.crlLoading = false;
        this.crlError = err?.error?.message || 'Failed to load CRL.';
      }
    });
  }

  clearOcsp(): void {
    this.ocspSerialNumber = '';
    this.ocspResult = null;
    this.ocspError = '';
    this.ocspLoading = false;
  }

  clearCrl(): void {
    this.crlIssuerSerialNumber = '';
    this.crlResult = null;
    this.crlError = '';
    this.crlLoading = false;
  }

  getRevocationReasonLabel(reason: string | null): string {
    switch (reason) {
      case 'UNSPECIFIED': return 'Unspecified';
      case 'KEY_COMPROMISE': return 'Key compromise';
      case 'CA_COMPROMISE': return 'CA compromise';
      case 'AFFILIATION_CHANGED': return 'Affiliation changed';
      case 'SUPERSEDED': return 'Superseded';
      case 'CESSATION_OF_OPERATION': return 'Cessation of operation';
      case 'CERTIFICATE_HOLD': return 'Certificate hold';
      case 'PRIVILEGE_WITHDRAWN': return 'Privilege withdrawn';
      case 'AA_COMPROMISE': return 'AA compromise';
      default: return reason || 'Not available';
    }
  }
}