import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { CertificatesService } from '../../services/certificates.service';
import { IssuerOption } from '../../models/issuer-option.model';

@Component({
  selector: 'app-issue-certificate-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShell],
  templateUrl: './issue-certificate-page.html',
  styleUrl: './issue-certificate-page.css'
})
export class IssueCertificatePage implements OnInit {
  private fb = inject(FormBuilder);
  private certificatesService = inject(CertificatesService);

  navItems = NAVIGATION_BY_ROLE.ADMIN;

  profile = {
    fullName: 'Administrator session',
    roleLabel: 'Administrator',
    organization: 'PKI system'
  };

  issueForm: FormGroup = this.fb.group({
    type: ['END_ENTITY', [Validators.required]],
    issuerCertificateId: [''],
    commonName: ['', [Validators.required, Validators.minLength(2)]],
    country: ['RS', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
    state: [''],
    locality: [''],
    organization: [''],
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
  errorMessage = '';
  issuers: IssuerOption[] = [];

  ngOnInit(): void {
    this.loadIssuersForType(this.issueForm.value.type);

    this.issueForm.get('type')?.valueChanges.subscribe((type) => {
      this.applyTypeDefaults(type);
      this.loadIssuersForType(type);
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    if (this.issueForm.invalid) {
      this.issueForm.markAllAsTouched();
      return;
    }

    // Backend POST issuing endpoint is not implemented yet.
    console.log('Issue certificate payload ready:', this.issueForm.getRawValue());
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.issueForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  private loadIssuersForType(type: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY'): void {
    this.loadingIssuers = true;
    this.errorMessage = '';

    this.certificatesService.getAvailableIssuers(type).subscribe({
      next: (issuers) => {
        this.issuers = issuers;
        this.loadingIssuers = false;

        if (type === 'ROOT') {
          this.issueForm.patchValue({ issuerCertificateId: '' }, { emitEvent: false });
        }
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
}