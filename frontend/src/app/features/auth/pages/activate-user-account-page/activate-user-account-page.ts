import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PublicUserAuthService } from '../../services/public-user-auth.service';
import { CryptoBrowserService } from '../../services/crypto-browser.service';

@Component({
  selector: 'app-activate-user-account-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './activate-user-account-page.html',
  styleUrl: './activate-user-account-page.css'
})
export class ActivateUserAccountPage implements OnInit {
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
  invalidToken = false;

  encryptedChallenge = '';
  decryptedChallenge = '';

  step: 'publicKey' | 'privateKey' | 'done' = 'publicKey';

  activationForm: FormGroup = this.fb.group({
    publicKey: ['', [Validators.required]],
    privateKey: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.invalidToken = true;
      this.errorMessage = 'Activation token is missing.';
    }
  }

  requestChallenge(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    const publicKeyControl = this.activationForm.get('publicKey');

    if (!publicKeyControl || publicKeyControl.invalid || this.invalidToken) {
      publicKeyControl?.markAsTouched();
      return;
    }

    this.loading = true;

    this.publicUserAuthService.createActivationChallenge({
      token: this.token,
      publicKey: publicKeyControl.value
    }).subscribe({
      next: (response) => {
        this.loading = false;
        this.encryptedChallenge = response.encryptedChallenge;
        this.step = 'privateKey';
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Failed to generate activation challenge.';
      }
    });
  }

  async decryptAndActivate(): Promise<void> {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    const privateKeyControl = this.activationForm.get('privateKey');

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

      this.publicUserAuthService.completeActivation({
        token: this.token,
        decryptedChallenge: this.decryptedChallenge
      }).subscribe({
        next: (response) => {
          this.loading = false;
          this.successMessage = response.message || 'Account activated successfully.';
          this.step = 'done';
          this.activationForm.disable();
        },
        error: (error) => {
          this.loading = false;
          this.errorMessage = error?.error?.message || 'Activation failed.';
        }
      });
    } catch (error) {
      this.loading = false;
      this.errorMessage = 'Private key is invalid or decryption failed.';
    }
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.activationForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  goToLogin(): void {
    this.router.navigateByUrl('/auth/login');
  }
}