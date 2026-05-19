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
import { saveAs } from 'file-saver';

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
  systemOrganizationId: number | null = this.authService.getUserOrganizationId();

  issueForm: FormGroup = this.fb.group({
    type: [this.isAdmin ? 'ROOT' : 'INTERMEDIATE', [Validators.required]],
    issuerCertificateId: [null],
    organizationId: [this.isAdmin ? this.systemOrganizationId : this.authService.getUserOrganizationId()],
    commonName: ['', [Validators.required, Validators.minLength(2)]],
    country: ['RS', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
    state: [''],
    locality: [''],
    organizationalUnit: [''],
    email: ['', [Validators.email]],
    subjectAlternativeName: [''],
    validFrom: ['', [Validators.required]],
    validTo: ['', [Validators.required]],
    ca: [true],
    keyCertSign: [true],
    digitalSignature: [true],
    keyEncipherment: [false]
  }, { validators: this.dateComparisonValidator });

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

  get selectableOrganizations(): Organization[] {
    return this.organizations.filter(org => org.id !== 1);
  }

  issuerMaxDate: string | null = null;

  ngOnInit(): void {
    this.applyTypeDefaults(this.issueForm.value.type);
    this.applyConditionalValidation();
    this.loadCurrentUser();

    if (this.isAdmin) {
      this.loadOrganizations();
    } else {
      this.issueForm.patchValue({
        organizationId: this.authService.getUserOrganizationId()
      });
    }

    this.applyOrganizationDefaultForType(this.issueForm.value.type);
    this.loadIssuersForType(this.issueForm.value.type);

    this.issueForm.get('type')?.valueChanges.subscribe((type) => {
      this.applyTypeDefaults(type);
      this.applyConditionalValidation();
      this.applyOrganizationDefaultForType(type);
      this.loadIssuersForType(type);
    });

    this.issueForm.get('issuerCertificateId')?.valueChanges.subscribe((id) => {
      this.updateMaxDate(id);
    });
  }

  private applyOrganizationDefaultForType(type: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY'): void {
    const organizationControl = this.issueForm.get('organizationId');

    if (!organizationControl) {
      return;
    }

    if (!this.isAdmin) {
      organizationControl.setValue(this.authService.getUserOrganizationId(), { emitEvent: false });
      return;
    }

    if (type === 'ROOT') {
      organizationControl.setValue(this.systemOrganizationId, { emitEvent: false });
    } else if (organizationControl.value === this.systemOrganizationId) {
      organizationControl.setValue(null, { emitEvent: false });
    }
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

  private toLocalDateTimeString(value: string): string {
    return `${value}:00`;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.applyConditionalValidation();
    this.applyOrganizationDefaultForType(this.issueForm.value.type);

    if (this.issueForm.invalid) {
      this.issueForm.markAllAsTouched();
      return;
    }

    const formValue = this.issueForm.getRawValue();

    const payload = {
      ...formValue,
      issuerCertificateId:
        formValue.type === 'ROOT' || formValue.issuerCertificateId == null
          ? null
          : Number(formValue.issuerCertificateId),

      organizationId:
        formValue.type === 'ROOT'
          ? this.systemOrganizationId
          : formValue.organizationId == null
            ? null
            : Number(formValue.organizationId),

      validFrom: this.toLocalDateTimeString(formValue.validFrom),
      validTo: this.toLocalDateTimeString(formValue.validTo),
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
        if (created.certificateData != null) {
          this.downloadCertificate(created.certificateData!, created.subjectCommonName || `certificate-${created.id}`);
        }
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Failed to issue certificate.';
      }
    });
  }


  private downloadCertificate(data: ArrayBuffer, cn: string): void {
    const blob = new Blob([data], {
      type: 'application/x-pkcs12'
    });

    saveAs(blob, `${cn || 'certificate'}.p12`);
  }

  private resetForm(): void {
    const defaultType = this.isAdmin ? 'ROOT' : 'INTERMEDIATE';

    this.issueForm.reset({
      type: defaultType,
      country: 'RS',
      issuerCertificateId: null,
      organizationId: this.isAdmin ? this.systemOrganizationId : this.authService.getUserOrganizationId()
    });

    this.submitted = false;
    this.applyTypeDefaults(defaultType);
    this.applyConditionalValidation();
    this.applyOrganizationDefaultForType(defaultType);
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
      this.issuerMaxDate = null;
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
    const organizationControl = this.issueForm.get('organizationId');
    const type = this.issueForm.value.type;

    if (type === 'ROOT') {
      issuerControl?.clearValidators();
      issuerControl?.setValue(null, { emitEvent: false });

      if (this.isAdmin) {
        organizationControl?.clearValidators();
      }
    } else {
      issuerControl?.setValidators([Validators.required]);

      if (this.isAdmin) {
        organizationControl?.setValidators([Validators.required]);
      }
    }

    issuerControl?.updateValueAndValidity({ emitEvent: false });
    organizationControl?.updateValueAndValidity({ emitEvent: false });
  }
}