import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { saveAs } from 'file-saver';

import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';


import { AuthService } from '../../../auth/services/auth.service';
import { CurrentUserService } from '../../../users/services/current-user.service';
import { CertificatesService } from '../../../certificates/services/certificates.service';
import { IssuerOption } from '../../../certificates/models/issuer-option.model';

@Component({
  selector: 'app-autogenerate-certificate-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppShell
  ],
  templateUrl: './autogenerate-certificate.html',
  styleUrl: './autogenerate-certificate.css'
})
export class AutogenerateCertificatePage implements OnInit {

  private fb = inject(FormBuilder);
  private router = inject(Router);

  private certificatesService =
    inject(CertificatesService);

  private authService =
    inject(AuthService);

  private currentUserService =
    inject(CurrentUserService);

  navItems = NAVIGATION_BY_ROLE.USER;

  sidebarTitle = 'User panel';

  profile = {
    fullName: 'Loading...',
    roleLabel: 'User',
    organization: 'Loading...'
  };

  minDate: string =
    this.formatDateForInput(new Date());

  issuerMaxDate: string | null = null;

  submitted = false;
  submitting = false;
  loadingIssuers = false;

  successMessage = '';
  errorMessage = '';

  issuers: IssuerOption[] = [];

  form: FormGroup = this.fb.group(
    {
      issuerCertificateId: [
        null,
        Validators.required
      ],

      commonName: [
        '',
        [Validators.required, Validators.minLength(2)]
      ],

      country: [
        'RS',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(2)
        ]
      ],

      state: [''],

      locality: [''],

      organization: [
        '',
        Validators.required
      ],

      organizationalUnit: [''],

      email: [
        '',
        Validators.email
      ],

      subjectAlternativeName: [''],

      validFrom: [
        '',
        Validators.required
      ],

      validTo: [
        '',
        Validators.required
      ]
    },
    {
      validators: this.dateComparisonValidator
    }
  );

  ngOnInit(): void {

    this.loadCurrentUser();

    this.loadIssuers();

    this.form
      .get('issuerCertificateId')
      ?.valueChanges
      .subscribe(id => {
        this.updateMaxDate(id);
      });
  }

  private loadCurrentUser(): void {

    this.currentUserService.currentUser$
      .subscribe(user => {

        if (!user) {
          return;
        }

        this.profile = {
          fullName: user.fullName,
          roleLabel: 'User',
          organization:
            user.organizationName ??
            'No organization'
        };
      });
  }

  private loadIssuers(): void {

    this.loadingIssuers = true;

    this.certificatesService
      .getAvailableIssuers(false, 'END_ENTITY')
      .subscribe({
        next: issuers => {
          this.issuers = issuers;
          this.loadingIssuers = false;
        },
        error: () => {
          this.loadingIssuers = false;
          this.errorMessage =
            'Failed to load issuers.';
        }
      });
  }

  onSubmit(): void {

    this.submitted = true;

    this.errorMessage = '';
    this.successMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      ...this.form.getRawValue(),

      issuerCertificateId:
        Number(
          this.form.value.issuerCertificateId
        ),

      validFrom:
        this.toLocalDateTimeString(
          this.form.value.validFrom
        ),

      validTo:
        this.toLocalDateTimeString(
          this.form.value.validTo
        )
    };

    this.submitting = true;

    this.certificatesService
      .autoGenerate(payload)
      .subscribe({

        next: created => {

          this.submitting = false;

          this.successMessage =
            `Certificate "${created.subjectCommonName}" generated successfully.`;

          if (
            created.certificateData &&
            created.certificateData.length > 0
          ) {

            this.downloadCertificate(
              created.certificateData,
              created.subjectCommonName ??
              'certificate'
            );
          }
        },

        error: err => {

          this.submitting = false;

          this.errorMessage =
            err?.error?.message ??
            'Failed to generate certificate.';
        }
      });
  }

  goBack(): void {
    this.router.navigateByUrl(
      '/user/certificates'
    );
  }

  hasError(
    controlName: string,
    errorName: string
  ): boolean {

    const control =
      this.form.get(controlName);

    return !!control &&
      control.hasError(errorName) &&
      (
        control.touched ||
        this.submitted
      );
  }

  private updateMaxDate(
    issuerId: string | number
  ): void {

    const selectedIssuer =
      this.issuers.find(
        i => i.id === Number(issuerId)
      );

    if (
      selectedIssuer &&
      selectedIssuer.validTo
    ) {

      this.issuerMaxDate =
        this.formatDateForInput(
          selectedIssuer.validTo
        );
    }
  }

  private dateComparisonValidator(
    group: FormGroup
  ): any {

    const from =
      group.get('validFrom')?.value;

    const to =
      group.get('validTo')?.value;

    if (
      from &&
      to &&
      new Date(from) >= new Date(to)
    ) {
      return {
        datesInvalid: true
      };
    }

    return null;
  }

  private formatDateForInput(
    dateInput: string | Date
  ): string {

    const d = new Date(dateInput);

    const year = d.getFullYear();
    const month =
      ('0' + (d.getMonth() + 1)).slice(-2);

    const day =
      ('0' + d.getDate()).slice(-2);

    const hours =
      ('0' + d.getHours()).slice(-2);

    const minutes =
      ('0' + d.getMinutes()).slice(-2);

    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private toLocalDateTimeString(
    value: string
  ): string {

    return `${value}:00`;
  }

  private downloadCertificate(
    base64: string,
    cn: string
  ): void {

    const binary = atob(base64);

    const bytes =
      new Uint8Array(binary.length);

    for (
      let i = 0;
      i < binary.length;
      i++
    ) {
      bytes[i] =
        binary.charCodeAt(i);
    }

    const blob = new Blob(
      [bytes],
      {
        type:
          'application/x-pkcs12'
      }
    );

    saveAs(
      blob,
      `${cn || 'certificate'}.p12`
    );
  }
}