import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';

import { CurrentUserService } from '../../../users/services/current-user.service';

import { CertificateRequest } from '../../models/certificate-request.model';
import { CertificateRequestsService } from '../../services/certificate-requests.service';

@Component({
  selector: 'app-ca-certificate-requests-page',
  standalone: true,
  imports: [CommonModule, FormsModule, AppShell],
  templateUrl: './ca-certificate-requests-page.html',
  styleUrl: './ca-certificate-requests-page.css'
})
export class CaCertificateRequestsPage implements OnInit {

  private currentUserService = inject(CurrentUserService);
  private certificateRequestsService = inject(CertificateRequestsService);

  navItems = NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: 'Loading...',
    roleLabel: 'CA User',
    organization: 'Loading...'
  };

  sidebarTitle = 'CA panel';

  requests: CertificateRequest[] = [];

  loading = false;
  processing = false;

  errorMessage = '';
  successMessage = '';

  showRejectModal = false;

  selectedRequestId: number | null = null;
  rejectionReason = '';

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading = true;
    this.errorMessage = '';

    this.certificateRequestsService.getPendingRequests().subscribe({
      next: (requests) => {
        this.requests = requests;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage =
          err?.error?.message ??
          'Failed to load certificate requests.';
      }
    });
  }

  approveRequest(requestId: number): void {
    this.processing = true;

    this.certificateRequestsService.approveRequest(requestId).subscribe({
      next: () => {
        this.processing = false;
        this.successMessage = 'Certificate issued successfully.';
        this.loadRequests();
      },
      error: (err) => {
        this.processing = false;
        this.errorMessage =
          err?.error?.message ??
          'Failed to approve request.';
      }
    });
  }

  openRejectModal(requestId: number): void {
    this.selectedRequestId = requestId;
    this.rejectionReason = '';
    this.showRejectModal = true;
  }

  closeRejectModal(): void {
    this.showRejectModal = false;
    this.selectedRequestId = null;
    this.rejectionReason = '';
  }

  confirmReject(): void {
    if (!this.selectedRequestId) {
      return;
    }

    this.processing = true;

    this.certificateRequestsService.rejectRequest(
      this.selectedRequestId,
      this.rejectionReason
    ).subscribe({
      next: () => {
        this.processing = false;
        this.successMessage = 'Certificate request rejected.';
        this.closeRejectModal();
        this.loadRequests();
      },
      error: (err) => {
        this.processing = false;
        this.errorMessage =
          err?.error?.message ??
          'Failed to reject request.';
      }
    });
  }

  private loadCurrentUser(): void {
    this.currentUserService.currentUser$.subscribe(user => {
      if (!user) {
        return;
      }

      this.profile = {
        fullName: user.fullName,
        roleLabel: 'CA User',
        organization: user.organizationName || 'No organization'
      };
    });
  }
}