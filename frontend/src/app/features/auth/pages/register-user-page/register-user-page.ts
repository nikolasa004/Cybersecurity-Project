import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { zxcvbn, zxcvbnOptions } from '@zxcvbn-ts/core';
import * as zxcvbnCommonPackage from '@zxcvbn-ts/language-common';
import * as zxcvbnEnPackage from '@zxcvbn-ts/language-en';

import { AuthService } from '../../services/auth.service';
import { OrganizationsService } from '../../../organizations/services/organizations.service';
import { Organization } from '../../../organizations/models/organization.model';
import { passwordPolicyValidator } from '../../../../shared/validators/password-policy.validator';

const options = {
  translations: zxcvbnEnPackage.translations,
  dictionary: {
    ...zxcvbnCommonPackage.dictionary,
    ...zxcvbnEnPackage.dictionary
  },
  graphs: zxcvbnCommonPackage.adjacencyGraphs
};

zxcvbnOptions.setOptions(options);

@Component({
  selector: 'app-register-user-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-user-page.html',
  styleUrl: './register-user-page.css'
})
export class RegisterUserPage implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private organizationsService = inject(OrganizationsService);
  private router = inject(Router);

  registerForm: FormGroup = this.fb.group(
    {
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      organizationId: ['', [Validators.required]],
      password: ['', [Validators.required, passwordPolicyValidator(15, 64)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [this.passwordsMatchValidator] }
  );

  organizations: Organization[] = [];

  submitted = false;
  loading = false;
  loadingOrganizations = false;
  successMessage = '';
  errorMessage = '';

  passwordScore = -1;
  passwordStrengthLabel = 'No password entered';

  ngOnInit(): void {
    this.loadOrganizations();

    this.registerForm.get('password')?.valueChanges.subscribe((value: string) => {
      this.updatePasswordStrength(value ?? '');
    });
  }

  loadOrganizations(): void {
    this.loadingOrganizations = true;

    this.authService.getAll().subscribe({
      next: (organizations) => {
        this.organizations = organizations.filter(org => org.name !== 'PKI System');
        this.loadingOrganizations = false;
      },
      error: (error) => {
        this.loadingOrganizations = false;
        this.errorMessage = error?.error?.message || 'Failed to load organizations.';
      }
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const payload = {
      firstName: this.registerForm.value.firstName,
      lastName: this.registerForm.value.lastName,
      email: this.registerForm.value.email,
      organizationId: Number(this.registerForm.value.organizationId),
      password: this.registerForm.value.password,
      confirmPassword: this.registerForm.value.confirmPassword
    };

    this.authService.register(payload).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message || 'Registration successful. Check your email for activation link.';
        this.registerForm.disable();
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Registration failed.';
      }
    });
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.registerForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  hasFormError(errorName: string): boolean {
    return this.registerForm.hasError(errorName) && this.submitted;
  }

  get passwordLength(): number {
    return this.registerForm.get('password')?.value?.length ?? 0;
  }

  get strengthPercent(): number {
    if (this.passwordScore < 0) {
      return 0;
    }
    return ((this.passwordScore + 1) / 5) * 100;
  }

  goToLogin(): void {
    this.router.navigateByUrl('/auth/login');
  }

  private updatePasswordStrength(password: string): void {
    if (!password) {
      this.passwordScore = -1;
      this.passwordStrengthLabel = 'No password entered';
      return;
    }

    const result = zxcvbn(password);
    this.passwordScore = result.score;

    switch (result.score) {
      case 0:
        this.passwordStrengthLabel = 'Very weak';
        break;
      case 1:
        this.passwordStrengthLabel = 'Weak';
        break;
      case 2:
        this.passwordStrengthLabel = 'Fair';
        break;
      case 3:
        this.passwordStrengthLabel = 'Strong';
        break;
      case 4:
        this.passwordStrengthLabel = 'Very strong';
        break;
      default:
        this.passwordStrengthLabel = 'Unknown';
    }
  }

  private passwordsMatchValidator(formGroup: FormGroup) {
    const password = formGroup.get('password')?.value;
    const confirmPassword = formGroup.get('confirmPassword')?.value;

    if (!password || !confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordsMismatch: true };
  }
}