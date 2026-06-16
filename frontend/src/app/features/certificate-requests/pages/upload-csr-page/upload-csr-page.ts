import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';

import { CertificatesService } from '../../../certificates/services/certificates.service';
import { IssuerOption } from '../../../certificates/models/issuer-option.model';

import { CertificateRequestsService } from '../../services/certificate-requests.service';
import { CertificateRequest } from '../../models/certificate-request.model';

import { CurrentUserService } from '../../../users/services/current-user.service';

@Component({
  selector: 'app-upload-csr-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppShell
  ],
  templateUrl: './upload-csr-page.html',
  styleUrl: './upload-csr-page.css'
})
export class UploadCsrPage implements OnInit {
  private fb = inject(FormBuilder);
  private certificatesService = inject(CertificatesService);
  private certificateRequestsService = inject(CertificateRequestsService);
  private currentUserService = inject(CurrentUserService);

  navItems = NAVIGATION_BY_ROLE.USER;

  profile = {
    fullName: 'Loading...',
    roleLabel: 'User',
    organization: 'Loading...'
  };

  uploadForm: FormGroup = this.fb.group(
    {
      issuerCertificateId: [null, Validators.required],
      validFrom: ['', Validators.required],
      validTo: ['', Validators.required]
    },
    { validators: this.dateComparisonValidator }
  );

  selectedFile: File | null = null;

  issuers: IssuerOption[] = [];
  requests: CertificateRequest[] = [];

  loadingIssuers = false;
  loadingRequests = false;
  submitting = false;
  submitted = false;

  successMessage = '';
  errorMessage = '';

  minDate = this.formatDateForInput(new Date());
  issuerMaxDate: string | null = null;

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadIssuers();
    this.loadMyRequests();

    this.uploadForm.get('issuerCertificateId')?.valueChanges.subscribe((issuerId) => {
      this.updateIssuerMaxDate(issuerId);
    });
  }

  private loadCurrentUser(): void {
    this.currentUserService.load().subscribe({
      next: (user) => {
        this.profile = {
          fullName: user.fullName,
          roleLabel: 'User',
          organization: user.organizationName || 'No organization'
        };
      }
    });
  }

  private loadIssuers(): void {
    this.loadingIssuers = true;

    this.certificatesService.getAvailableIssuers(false).subscribe({
      next: (issuers) => {
        this.issuers = issuers;
        this.loadingIssuers = false;
      },
      error: (err) => {
        console.error(err);
        this.loadingIssuers = false;
        this.errorMessage = 'Failed to load issuer certificates.';
      }
    });
  }

loadMyRequests(): void {
  this.loadingRequests = true;

  this.certificateRequestsService.getMyRequests().subscribe({
    next: (requests) => {
      this.requests = requests;
      this.loadingRequests = false;
    },
    error: (err) => {
      console.error(err);
      this.loadingRequests = false;
    }
  });
}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  submit(): void {
    this.submitted = true;
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.selectedFile) {
      this.errorMessage = 'Please select a CSR file.';
      return;
    }

    if (this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      return;
    }

    const formValue = this.uploadForm.value;

    const payload = {
      issuerCertificateId: Number(formValue.issuerCertificateId),
      validFrom: this.toLocalDateTimeString(formValue.validFrom),
      validTo: this.toLocalDateTimeString(formValue.validTo)
    };

    this.submitting = true;

    this.certificateRequestsService
      .createRequest(this.selectedFile, payload)
      .subscribe({
        next: () => {
          this.submitting = false;
          this.successMessage = 'CSR request submitted successfully.';

          this.uploadForm.reset({
            issuerCertificateId: null,
            validFrom: '',
            validTo: ''
          });

          this.selectedFile = null;
          this.submitted = false;
          this.issuerMaxDate = null;

          this.loadMyRequests();
        },
        error: (err) => {
          console.error(err);
          this.submitting = false;
          this.errorMessage =
            err?.error?.message || 'Failed to submit CSR request.';
        }
      });
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.uploadForm.get(controlName);

    return !!control &&
      control.hasError(errorName) &&
      (control.touched || this.submitted);
  }

  getStatusLabel(status: CertificateRequest['status']): string {
    if (status === 'PENDING') {
      return 'Pending';
    }

    if (status === 'APPROVED') {
      return 'Approved';
    }

    return 'Rejected';
  }

  private updateIssuerMaxDate(issuerId: string | number | null): void {
    if (!issuerId) {
      this.issuerMaxDate = null;
      return;
    }

    const selectedIssuer = this.issuers.find(
      issuer => issuer.id === Number(issuerId)
    );

    if (!selectedIssuer) {
      this.issuerMaxDate = null;
      return;
    }

    this.issuerMaxDate = this.formatDateForInput(selectedIssuer.validTo);

    const currentValidTo = this.uploadForm.get('validTo')?.value;

    if (currentValidTo && this.issuerMaxDate && currentValidTo > this.issuerMaxDate) {
      this.uploadForm.patchValue({
        validTo: ''
      });
    }
  }

  private dateComparisonValidator(group: FormGroup): any {
    const from = group.get('validFrom')?.value;
    const to = group.get('validTo')?.value;

    if (from && to && new Date(from) >= new Date(to)) {
      return { datesInvalid: true };
    }

    return null;
  }

  private formatDateForInput(dateInput: string | Date): string {
    const d = new Date(dateInput);

    const year = d.getFullYear();
    const month = ('0' + (d.getMonth() + 1)).slice(-2);
    const day = ('0' + d.getDate()).slice(-2);
    const hours = ('0' + d.getHours()).slice(-2);
    const minutes = ('0' + d.getMinutes()).slice(-2);

    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private toLocalDateTimeString(value: string): string {
    return `${value}:00`;
  }
}