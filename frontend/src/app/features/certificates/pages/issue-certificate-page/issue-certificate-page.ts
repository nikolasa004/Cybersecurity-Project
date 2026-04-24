import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { CertificatesService } from '../../services/certificates.service';
import { IssuerOption } from '../../models/issuer-option.model';
import { OrganizationsService } from '../../../organizations/services/organizations.service';
import { Organization } from '../../../organizations/models/organization.model';
import { AuthService } from '../../../auth/services/auth.service';

@Component({
  selector: 'app-issue-certificate-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShell],
  templateUrl: './issue-certificate-page.html',
  styleUrl: './issue-certificate-page.css'
})
export class IssueCertificatePage implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private certificatesService = inject(CertificatesService);
  private organizationsService = inject(OrganizationsService);
  private authService = inject(AuthService);

  isAdmin = this.authService.getRole() === 'ADMIN';
  navItems = this.isAdmin ? NAVIGATION_BY_ROLE.ADMIN : NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: this.isAdmin ? 'Administrator session' : 'CA User session',
    roleLabel: this.isAdmin ? 'Administrator' : 'CA Representative',
    organization: this.isAdmin ? 'PKI system' : this.authService.getUserOrganizationName() || 'Unknown Organization'
  };

  sidebarTitle = this.isAdmin ? 'Admin panel' : 'CA panel';

  issueForm: FormGroup = this.fb.group({
    type: [this.isAdmin ? 'ROOT' : 'INTERMEDIATE', [Validators.required]],
    issuerCertificateId: [''],
    organizationId: [''],
    commonName: ['', [Validators.required, Validators.minLength(2)]],
    country: ['RS', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
    state: [''],
    locality: [''],
    organizationalUnit: [''],
    email: ['', [Validators.email]],
    subjectSerialNumber: [''],
    validFrom: ['', [Validators.required]],
    validTo: ['', [Validators.required]],
    ca: [true],
    keyCertSign: [true],
    digitalSignature: [true],
    keyEncipherment: [false]
  });

  submitted = false;
  loadingIssuers = false;
  loadingOrganizations = false;
  submitting = false;
  successMessage = '';
  errorMessage = '';

  issuers: IssuerOption[] = [];
  organizations: Organization[] = [];

  ngOnInit(): void {
    this.applyTypeDefaults(this.issueForm.value.type);
    if (this.isAdmin) {
      this.loadOrganizations();
    } else {
      this.issueForm.patchValue({ organizationId: this.authService.getUserOrganizationId() });
    }
    this.loadIssuersForType(this.issueForm.value.type);

    this.issueForm.get('type')?.valueChanges.subscribe((type) => {
      this.applyTypeDefaults(type);
      this.loadIssuersForType(type);
    });
  }

  onSubmit(): void {
    console.log('Form submitted with value:', this.issueForm.value);
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.applyConditionalValidation();

    if (this.issueForm.invalid) {
      this.issueForm.markAllAsTouched();
      return;
    }

    const payload = {
      ...this.issueForm.value,
      issuerCertificateId: this.issueForm.value.type === 'ROOT' ? null : Number(this.issueForm.value.issuerCertificateId),
      organizationId: Number(this.issueForm.value.organizationId),
      validFrom: new Date(this.issueForm.value.validFrom).toISOString(),
      validTo: new Date(this.issueForm.value.validTo).toISOString()
    };

    this.submitting = true;

    this.certificatesService.issue(payload, this.isAdmin).subscribe({
      next: (created) => {
        this.submitting = false;
        this.successMessage = `Certificate "${created.subjectCommonName}" issued successfully.`;
        this.resetForm();
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Failed to issue certificate.';
      }
    });
  }

  private resetForm(): void {
    const defaultType = this.isAdmin ? 'ROOT' : 'INTERMEDIATE';
    this.issueForm.reset({
      type: defaultType,
      country: 'RS',
      organizationId: this.isAdmin ? '' : this.authService.getUserOrganizationId()
    });
    this.submitted = false;
    this.applyTypeDefaults(defaultType);
    this.loadIssuersForType(defaultType);
  }

  goToCertificates(): void {
    const path = this.isAdmin ? '/admin/certificates' : '/ca/certificates';
    this.router.navigateByUrl(path);
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.issueForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  private loadOrganizations(): void {
    this.loadingOrganizations = true;
    this.organizationsService.getAll().subscribe({
      next: (orgs) => {
        this.organizations = orgs;
        this.loadingOrganizations = false;
      }
    });
  }

  private loadIssuersForType(type: string): void {
    if (type === 'ROOT') {
      this.issuers = [];
      return;
    }
    this.loadingIssuers = true;
    this.certificatesService.getAvailableIssuers(this.isAdmin, type).subscribe({
      next: (issuers) => {
        this.issuers = issuers;
        this.loadingIssuers = false;
      },
      error: () => {
        this.loadingIssuers = false;
        this.errorMessage = 'Failed to load issuers.';
      }
    });
  }

  private applyTypeDefaults(type: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY'): void {
    if (type === 'ROOT') {
      this.issueForm.patchValue(
        {
          issuerCertificateId: '',
          ca: true,
          keyCertSign: true,
          digitalSignature: true,
          keyEncipherment: false
        },
        { emitEvent: false }
      );
      return;
    }

    if (type === 'INTERMEDIATE') {
      this.issueForm.patchValue(
        {
          ca: true,
          keyCertSign: true,
          digitalSignature: true,
          keyEncipherment: false
        },
        { emitEvent: false }
      );
      return;
    }

    this.issueForm.patchValue(
      {
        ca: false,
        keyCertSign: false,
        digitalSignature: true,
        keyEncipherment: true
      },
      { emitEvent: false }
    );
  }

  private applyConditionalValidation(): void {
    const issuerControl = this.issueForm.get('issuerCertificateId');

    if (this.issueForm.value.type === 'ROOT') {
      issuerControl?.clearValidators();
      issuerControl?.setValue('', { emitEvent: false });
    } else {
      issuerControl?.setValidators([Validators.required]);
    }

    issuerControl?.updateValueAndValidity({ emitEvent: false });
  }

  private normalizeOptional(value: string | null | undefined): string | null {
    if (value == null) {
      return null;
    }

    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }

  private toIsoDateTime(value: string): string {
    return new Date(value).toISOString();
  }
}