import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
})
export class LoginPage {
  loginForm: FormGroup;
  errorMessage: string = '';
  isMfaRequired: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      mfaCode:['',[Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  onSubmit() {
  if (this.isMfaRequired) {
    const mfaControl = this.loginForm.get('mfaCode');
    if (mfaControl && mfaControl.valid) {
      this.onVerifyMfa();
    } else {
      this.errorMessage = 'Please enter a valid 6-digit code.';
    }
    return;
  }

  if (this.loginForm.valid) {
    this.authService.login(this.loginForm.value).subscribe({
      next: (response: any) => { 
        console.log('Successful login!', response);

        if (response.isTwoFactorRequired) {
          this.isMfaRequired = true;
          this.errorMessage = '';
          
          this.loginForm.get('mfaCode')?.setValidators([
            Validators.required, 
            Validators.pattern(/^\d{6}$/)
          ]);
          this.loginForm.get('mfaCode')?.updateValueAndValidity();
          return; 
        }

        switch (response.role) {
          case 'ADMIN': this.router.navigate(['/admin']); break;
          case 'CA_USER': this.router.navigate(['/ca']); break;
          case 'USER': this.router.navigate(['/user']); break;
          default: this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.errorMessage = 'Invalid email or password.';
      }
    });
  }
}
  onVerifyMfa() {
    const code = this.loginForm.get('mfaCode')?.value;
    const email = this.loginForm.get('email')?.value;

    this.authService.verify2faLogin({ email, code: code.trim() }).subscribe({
      next: (response: { role: any; }) => {
        console.log('2FA Uspešan!', response);
        switch (response.role) {
          case 'ADMIN': this.router.navigate(['/admin']); break;
          case 'CA_USER': this.router.navigate(['/ca']); break;
          case 'USER': this.router.navigate(['/user']); break;
          default: this.router.navigate(['/']);
        }
      },
      error: () => {
        this.errorMessage = 'Invalid verification code. Please try again.';
      }
    });
  }

  cancelMfa() {
    this.isMfaRequired = false;
    this.errorMessage = '';
    this.loginForm.get('mfaCode')?.setValue('');
    this.loginForm.get('mfaCode')?.clearValidators();
    this.loginForm.get('mfaCode')?.updateValueAndValidity();
  }
}
