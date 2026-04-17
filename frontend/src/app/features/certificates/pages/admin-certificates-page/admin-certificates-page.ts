import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { CertificatesService } from '../../services/certificates.service';
import { CertificateListItem } from '../../models/certificate-list.model';

@Component({
  selector: 'app-admin-certificates-page',
  standalone: true,
  imports: [CommonModule, AppShell, RouterLink],
  templateUrl: './admin-certificates-page.html',
  styleUrl: './admin-certificates-page.css'
})
export class AdminCertificatesPage implements OnInit {
  private certificatesService = inject(CertificatesService);

  navItems = NAVIGATION_BY_ROLE.ADMIN;

  profile = {
    fullName: 'Administrator session',
    roleLabel: 'Administrator',
    organization: 'PKI system'
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

    this.certificatesService.getAll().subscribe({
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