import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { zxcvbn, zxcvbnOptions } from '@zxcvbn-ts/core';
import * as zxcvbnCommonPackage from '@zxcvbn-ts/language-common';
import * as zxcvbnEnPackage from '@zxcvbn-ts/language-en';

import { CaUsersService } from '../../../ca-users/services/ca-users.service';
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
  selector: 'app-activate-account-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './activate-account-page.html',
  styleUrl: './activate-account-page.css'
})
export class ActivateAccountPage implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private caUsersService = inject(CaUsersService);

  activationForm: FormGroup = this.fb.group(
    {
      password: ['', [Validators.required, passwordPolicyValidator(15, 64)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [this.passwordsMatchValidator] }
  );

  token = '';
  submitted = false;
  loading = false;
  successMessage = '';
  errorMessage = '';
  invalidToken = false;

  passwordScore = -1;
  passwordStrengthLabel = 'No password entered';

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.invalidToken = true;
      this.errorMessage = 'Activation token is missing.';
    }

    this.activationForm.get('password')?.valueChanges.subscribe((value: string) => {
      this.updatePasswordStrength(value ?? '');
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.successMessage = '';
    this.errorMessage = '';

    if (this.invalidToken) {
      return;
    }

    if (this.activationForm.invalid) {
      this.activationForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.caUsersService.activate({
      token: this.token,
      password: this.activationForm.value.password
    }).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message || 'Password set successfully.';
        this.activationForm.disable();
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Activation failed.';
      }
    });
  }

  goToLogin(): void {
    this.router.navigateByUrl('/');
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.activationForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  hasFormError(errorName: string): boolean {
    return this.activationForm.hasError(errorName) && this.submitted;
  }

  get passwordLength(): number {
    return this.activationForm.get('password')?.value?.length ?? 0;
  }

  get strengthPercent(): number {
    if (this.passwordScore < 0) {
      return 0;
    }

    return ((this.passwordScore + 1) / 5) * 100;
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