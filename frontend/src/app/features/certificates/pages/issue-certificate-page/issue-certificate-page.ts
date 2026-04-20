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

  navItems = NAVIGATION_BY_ROLE.ADMIN;

  lastIssuedCertificateName = '';

  profile = {
    fullName: 'Administrator session',
    roleLabel: 'Administrator',
    organization: 'PKI system'
  };

  issueForm: FormGroup = this.fb.group({
    type: ['END_ENTITY', [Validators.required]],
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
    ca: [false],
    keyCertSign: [false],
    digitalSignature: [true],
    keyEncipherment: [true]
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
    this.loadOrganizations();
    this.loadIssuersForType(this.issueForm.value.type);

    this.issueForm.get('type')?.valueChanges.subscribe((type) => {
      this.applyTypeDefaults(type);
      this.loadIssuersForType(type);
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.applyConditionalValidation();

    if (this.issueForm.invalid) {
      this.issueForm.markAllAsTouched();
      return;
    }

    const selectedOrganization = this.organizations.find(
      (organization) => organization.id === Number(this.issueForm.value.organizationId)
    );

    const payload = {
      type: this.issueForm.value.type,
      issuerCertificateId:
        this.issueForm.value.type === 'ROOT' || !this.issueForm.value.issuerCertificateId
          ? null
          : Number(this.issueForm.value.issuerCertificateId),
      organizationId: this.issueForm.value.organizationId
        ? Number(this.issueForm.value.organizationId)
        : null,
      commonName: this.issueForm.value.commonName,
      country: this.normalizeOptional(this.issueForm.value.country),
      state: this.normalizeOptional(this.issueForm.value.state),
      locality: this.normalizeOptional(this.issueForm.value.locality),
      organization: selectedOrganization ? selectedOrganization.name : null,
      organizationalUnit: this.normalizeOptional(this.issueForm.value.organizationalUnit),
      email: this.normalizeOptional(this.issueForm.value.email),
      subjectSerialNumber: this.normalizeOptional(this.issueForm.value.subjectSerialNumber),
      validFrom: this.toIsoDateTime(this.issueForm.value.validFrom),
      validTo: this.toIsoDateTime(this.issueForm.value.validTo),
      ca: !!this.issueForm.value.ca,
      keyCertSign: !!this.issueForm.value.keyCertSign,
      digitalSignature: !!this.issueForm.value.digitalSignature,
      keyEncipherment: !!this.issueForm.value.keyEncipherment
    };

    this.submitting = true;

    this.certificatesService.issue(payload).subscribe({
    next: (createdCertificate) => {
      this.submitting = false;
      this.lastIssuedCertificateName =
        createdCertificate.subjectCommonName || 'Certificate';

      this.successMessage = `Certificate "${this.lastIssuedCertificateName}" was issued successfully.`;

      this.issueForm.reset({
        type: 'END_ENTITY',
        issuerCertificateId: '',
        organizationId: '',
        commonName: '',
        country: 'RS',
        state: '',
        locality: '',
        organizationalUnit: '',
        email: '',
        subjectSerialNumber: '',
        validFrom: '',
        validTo: '',
        ca: false,
        keyCertSign: false,
        digitalSignature: true,
        keyEncipherment: true
      },
       { emitEvent: false }
      );

      this.submitted = false;
      this.applyTypeDefaults('END_ENTITY');
      this.loadIssuersForType('END_ENTITY');
    },
      error: (error) => {
        this.submitting = false;
        this.errorMessage = error?.error?.message || 'Failed to issue certificate.';
      }
    });
  }

  goToCertificates(): void {
    this.router.navigateByUrl('/admin/certificates');
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.issueForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  private loadOrganizations(): void {
    this.loadingOrganizations = true;

    this.organizationsService.getAll().subscribe({
      next: (organizations) => {
        this.organizations = organizations;
        this.loadingOrganizations = false;
      },
      error: (error) => {
        this.loadingOrganizations = false;
        this.errorMessage = error?.error?.message || 'Failed to load organizations.';
      }
    });
  }

  private loadIssuersForType(type: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY'): void {
    if (type === 'ROOT') {
      this.issuers = [];
      this.issueForm.patchValue({ issuerCertificateId: '' }, { emitEvent: false });
      this.loadingIssuers = false;
      return;
    }

    this.loadingIssuers = true;
    this.errorMessage = '';

    this.certificatesService.getAvailableIssuers(type).subscribe({
      next: (issuers) => {
        this.issuers = issuers;
        this.loadingIssuers = false;
      },
      error: (error) => {
        this.issuers = [];
        this.loadingIssuers = false;
        this.errorMessage = error?.error?.message || 'Failed to load available issuers.';
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