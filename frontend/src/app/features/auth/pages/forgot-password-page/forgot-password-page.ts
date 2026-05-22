import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { zxcvbn, zxcvbnOptions } from '@zxcvbn-ts/core';
import * as zxcvbnCommonPackage from '@zxcvbn-ts/language-common';
import * as zxcvbnEnPackage from '@zxcvbn-ts/language-en';

import { PublicUserAuthService } from '../../services/public-user-auth.service';
import { CryptoBrowserService } from '../../services/crypto-browser.service';
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
  selector: 'app-forgot-password-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password-page.html',
  styleUrl: './forgot-password-page.css'
})
export class ForgotPasswordPage implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private publicUserAuthService = inject(PublicUserAuthService);
  private cryptoBrowserService = inject(CryptoBrowserService);

  token = '';
  loading = false;
  submitted = false;
  successMessage = '';
  errorMessage = '';

  encryptedChallenge = '';
  decryptedChallenge = '';

  step: 'request' | 'challenge' | 'password' | 'done' = 'request';

  requestForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  resetForm: FormGroup = this.fb.group(
    {
      privateKey: ['', [Validators.required]],
      newPassword: ['', [Validators.required, passwordPolicyValidator(15, 64)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [this.passwordsMatchValidator] }
  );

  passwordScore = -1;
  passwordStrengthLabel = 'No password entered';

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (this.token) {
      this.step = 'challenge';
      this.loadChallenge();
    }

    this.resetForm.get('newPassword')?.valueChanges.subscribe((value: string) => {
      this.updatePasswordStrength(value ?? '');
    });
  }

  requestReset(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.requestForm.invalid) {
      this.requestForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.publicUserAuthService.requestPasswordReset({
      email: this.requestForm.value.email
    }).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message || 'If an account with that email exists, a reset link has been sent.';
        this.requestForm.disable();
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Failed to start password reset.';
      }
    });
  }

  loadChallenge(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.loading = true;

    this.publicUserAuthService.createPasswordResetChallenge({
      token: this.token
    }).subscribe({
      next: (response) => {
        this.loading = false;
        this.encryptedChallenge = response.encryptedChallenge;
        this.step = 'challenge';
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Failed to load password reset challenge.';
      }
    });
  }

  async decryptChallenge(): Promise<void> {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    const privateKeyControl = this.resetForm.get('privateKey');

    if (!privateKeyControl || privateKeyControl.invalid) {
      privateKeyControl?.markAsTouched();
      return;
    }

    if (!this.encryptedChallenge) {
      this.errorMessage = 'Encrypted challenge is missing.';
      return;
    }

    this.loading = true;

    try {
      this.decryptedChallenge = await this.cryptoBrowserService.decryptChallengeWithPrivateKey(
        privateKeyControl.value,
        this.encryptedChallenge
      );

      this.loading = false;
      this.step = 'password';
    } catch (error) {
      this.loading = false;
      this.errorMessage = 'Private key is invalid or decryption failed.';
    }
  }

  completeReset(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (
      this.resetForm.get('newPassword')?.invalid ||
      this.resetForm.get('confirmPassword')?.invalid ||
      this.resetForm.hasError('passwordsMismatch')
    ) {
      this.resetForm.markAllAsTouched();
      return;
    }

    if (!this.decryptedChallenge) {
      this.errorMessage = 'Challenge was not decrypted successfully.';
      return;
    }

    this.loading = true;

    this.publicUserAuthService.completePasswordReset({
      token: this.token,
      decryptedChallenge: this.decryptedChallenge,
      newPassword: this.resetForm.value.newPassword,
      confirmPassword: this.resetForm.value.confirmPassword
    }).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message || 'Password changed successfully.';
        this.step = 'done';
        this.resetForm.disable();
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Password reset failed.';
      }
    });
  }

  hasRequestError(controlName: string, errorName: string): boolean {
    const control = this.requestForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  hasResetError(controlName: string, errorName: string): boolean {
    const control = this.resetForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  hasResetFormError(errorName: string): boolean {
    return this.resetForm.hasError(errorName) && this.submitted;
  }

  get passwordLength(): number {
    return this.resetForm.get('newPassword')?.value?.length ?? 0;
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
    const password = formGroup.get('newPassword')?.value;
    const confirmPassword = formGroup.get('confirmPassword')?.value;

    if (!password || !confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordsMismatch: true };
  }
}