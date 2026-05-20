import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { AppShell } from '../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../core/config/role-navigation';
import { CurrentUserService } from '../../features/users/services/current-user.service';
import { UserService } from '../../features/users/services/user.service';

@Component({
  selector: 'app-ca-dashboard-page',
  standalone: true,
  imports: [CommonModule, AppShell],
  templateUrl: './ca-dashboard-page.html',
  styleUrl: './ca-dashboard-page.css'
})
export class CaDashboardPage implements OnInit {
  private currentUserService = inject(CurrentUserService);
  private userService = inject(UserService);

  @ViewChild('mfaModal', { static: false }) mfaModal!: ElementRef<HTMLDialogElement>;

  navItems = NAVIGATION_BY_ROLE.CA_USER;

  profile = {
    fullName: 'Loading...',
    roleLabel: 'CA User',
    organization: 'Loading...',
    email: 'Loading...',
    twoFactorEnabled: false
  };

  qrCodeImageUrl: string = '';
  mfaErrorMessage: string = '';

  focusAreas = [
    'Issue intermediate and end-entity certificates within your organization',
    'Work only with organization-scoped certificate data',
    'Use available certificate chains assigned to your organization',
    'Review certificates that belong to your organization'
  ];

  issuingRules = [
    'Certificate issuing is limited to the organization assigned to your account.',
    'Issuer validation must be satisfied before any certificate can be issued.',
    'Certificate validity must remain within the validity period of the selected issuer.',
    'Only organization-relevant certificate workflows are available from this workspace.'
  ];

  accessNotes = [
    'You can review only the certificates that belong to your organization.',
    'Administrator-level certificate visibility is not available from this dashboard.',
    'Organization scope and certificate permissions are enforced through your assigned CA role.'
  ];

  ngOnInit(): void {
    this.currentUserService.currentUser$.subscribe((user) => {
      if (!user) {
        return;
      }

      this.profile = {
        fullName: user.fullName,
        roleLabel: 'CA User',
        organization: user.organizationName || 'No organization',
        email: user.email,
        twoFactorEnabled: user.twoFactorEnabled || false
      };
    });
  }

 onToggle2FA(event: any): void {
    const isChecked = event.target.checked;
    
    if (isChecked) {
      event.target.checked = false;
      
      console.log('Pokretanje 2FA setup-a na backendu...');
      
      this.userService.get2faSetup().subscribe({
       next: (response: any) => {
          console.log('Podaci uspešno stigli sa backenda:', response);
          
          const rawUrl = response.qrCodeUrl || response.qrCodeURL || response.url;
          
          if (rawUrl) {
            const completelyDecodedUrl = decodeURIComponent(decodeURIComponent(rawUrl));
            
            this.qrCodeImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(completelyDecodedUrl)}`;
          } else {
            console.warn('Backend nije vratio url polje. Pravim zamenski kod.');
            const fallbackUrl = `otpauth://totp/PKI-App:${this.profile.email}?secret=${response.secretKey || 'TESTSECRET123'}&issuer=PKI-App`;
            
            this.qrCodeImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(fallbackUrl)}`;
          }
          
          this.mfaErrorMessage = '';
          
          if (this.mfaModal && this.mfaModal.nativeElement) {
            this.mfaModal.nativeElement.showModal();
            console.log('Novi ispravan URL slike:', this.qrCodeImageUrl);
          }
        },
        error: (err) => {
          console.error('Zahtev je odbijen ili je pukao!', err);
          alert('Error communicating with the server.');
        }
      });
      
    } else {
      if (confirm('Are you sure you want to turn off 2FA? Your account will be less secure.')) {
        this.userService.disable2fa().subscribe({
          next: () => {
            this.profile.twoFactorEnabled = false;
            alert('two-factor authentication successfully turned off');
          },
          error: (err) => {
            event.target.checked = true;
            console.error('Error in turning off two-factor authentication', err);
          }
        });
      } else {
        event.target.checked = true;
      }
    }
  }

  confirmMfa(code: string): void {
    if (!code || code.trim().length !== 6) {
      this.mfaErrorMessage = 'Please enter a valid 6-digit code.';
      return;
    }

    this.userService.enable2fa(code.trim()).subscribe({
      next: () => {
        this.profile.twoFactorEnabled = true;
        this.closeMfaModal();
      },
      error: (err) => {
        console.error('Code verification failed', err);
        this.mfaErrorMessage = 'Invalid verification code. Please try again.';
      }
    });
  }

  closeMfaModal(): void {
    if (this.mfaModal && this.mfaModal.nativeElement) {
      this.mfaModal.nativeElement.close();
    }
    this.qrCodeImageUrl = '';
    this.mfaErrorMessage = '';
  }
}