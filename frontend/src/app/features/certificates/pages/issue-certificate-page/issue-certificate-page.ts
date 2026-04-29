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
import { CurrentUserService } from '../../../users/services/current-user.service';

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
  private currentUserService = inject(CurrentUserService);

  isAdmin = this.authService.getRole() === 'ADMIN';
  navItems = this.isAdmin ? NAVIGATION_BY_ROLE.ADMIN : NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: 'Loading...',
    roleLabel: this.isAdmin ? 'Administrator' : 'CA User',
    organization: 'Loading...'
  };

  sidebarTitle = this.isAdmin ? 'Admin panel' : 'CA panel';

  minDate: string = this.formatDateForInput(new Date());

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
  }, { validators: this.dateComparisonValidator});

  private dateComparisonValidator(group: FormGroup): any {
    const from = group.get('validFrom')?.value;
    const to = group.get('validTo')?.value;
    
    if (from && to && new Date(from) >= new Date(to)) {
      return { datesInvalid: true };
    }
    return null;
  }

  submitted = false;
  loadingIssuers = false;
  loadingOrganizations = false;
  submitting = false;
  successMessage = '';
  errorMessage = '';

  issuers: IssuerOption[] = [];
  organizations: Organization[] = [];

  issuerMaxDate: string | null = null;

  ngOnInit(): void {
    this.applyTypeDefaults(this.issueForm.value.type);
    this.loadCurrentUser();
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

    this.issueForm.get('issuerCertificateId')?.valueChanges.subscribe((id) => {
      this.updateMaxDate(id);
    });
  }

  private updateMaxDate(issuerId: string | number): void {
    if (!issuerId || this.issueForm.value.type === 'ROOT') {
      this.issuerMaxDate = null;
      return;
    }

    const selectedIssuer = this.issuers.find(i => i.id === Number(issuerId));
    
    if (selectedIssuer && selectedIssuer.validTo) {
      this.issuerMaxDate = this.formatDateForInput(selectedIssuer.validTo);
      
      const currentValidTo = this.issueForm.get('validTo')?.value;
      if (currentValidTo && currentValidTo > this.issuerMaxDate) {
        this.issueForm.patchValue({ validTo: '' });
      }
    }
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

  private formatDateForInput(dateInput: string | Date): string {
    const d = new Date(dateInput);
    const year = d.getFullYear();
    const month = ('0' + (d.getMonth() + 1)).slice(-2);
    const day = ('0' + d.getDate()).slice(-2);
    const hours = ('0' + d.getHours()).slice(-2);
    const minutes = ('0' + d.getMinutes()).slice(-2);
    return `${year}-${month}-${day}T${hours}:${minutes}`;
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

    const formValue = this.issueForm.getRawValue();

    const payload = {
      ...formValue,
      issuerCertificateId: formValue.type === 'ROOT' ? null : Number(formValue.issuerCertificateId),
      organizationId: Number(formValue.organizationId),
      validFrom: new Date(formValue.validFrom).toISOString(),
      validTo: new Date(formValue.validTo).toISOString(),
      ca: !!formValue.ca,
      keyCertSign: !!formValue.keyCertSign,
      digitalSignature: !!formValue.digitalSignature,
      keyEncipherment: !!formValue.keyEncipherment
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
    const caCtrl = this.issueForm.get('ca');
    const keySignCtrl = this.issueForm.get('keyCertSign');

    if (type === 'END_ENTITY') {
      this.issueForm.patchValue({
        ca: false,
        keyCertSign: false,
        digitalSignature: true,
        keyEncipherment: true
      }, { emitEvent: false });
      
      caCtrl?.disable();
      keySignCtrl?.disable();
    } else {
      this.issueForm.patchValue({
        ca: true,
        keyCertSign: true,
        digitalSignature: true,
        keyEncipherment: false
      }, { emitEvent: false });
      
      caCtrl?.disable();
      keySignCtrl?.disable();
    }
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