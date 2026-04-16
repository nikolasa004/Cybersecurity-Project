import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';

interface OrganizationOption {
  id: number;
  name: string;
}

interface CaUserView {
  fullName: string;
  email: string;
  organizationName: string;
  status: string;
}

@Component({
  selector: 'app-ca-users-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShell],
  templateUrl: './ca-users-page.html',
  styleUrl: './ca-users-page.css'
})
export class CaUsersPage {
  navItems = NAVIGATION_BY_ROLE.ADMIN;

  profile = {
    fullName: 'Admin Administrator',
    roleLabel: 'Administrator',
    organization: 'System Administration',
    email: 'admin@pki.local'
  };

  caUserForm: FormGroup;
  submitted = false;
  createdSuccessfully = false;

  organizations: OrganizationOption[] = [
    { id: 1, name: 'Komsiluk Soft' },
    { id: 2, name: 'PKI Labs' },
    { id: 3, name: 'Secure Tech' }
  ];

  caUsers: CaUserView[] = [
    {
      fullName: 'Marko Markovic',
      email: 'marko@komsiluk.rs',
      organizationName: 'Komsiluk Soft',
      status: 'Pending activation'
    },
    {
      fullName: 'Ivana Ilic',
      email: 'ivana@pkilabs.rs',
      organizationName: 'PKI Labs',
      status: 'Active'
    }
  ];

  constructor(private fb: FormBuilder) {
    this.caUserForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      organizationId: ['', [Validators.required]],
      position: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.createdSuccessfully = false;

    if (this.caUserForm.invalid) {
      this.caUserForm.markAllAsTouched();
      return;
    }

    const selectedOrganization = this.organizations.find(
      o => o.id === Number(this.caUserForm.value.organizationId)
    );

    this.caUsers = [
      {
        fullName: `${this.caUserForm.value.firstName} ${this.caUserForm.value.lastName}`,
        email: this.caUserForm.value.email,
        organizationName: selectedOrganization?.name ?? 'Unknown organization',
        status: 'Pending activation'
      },
      ...this.caUsers
    ];

    this.caUserForm.reset();
    this.submitted = false;
    this.createdSuccessfully = true;
  }

  onReset(): void {
    this.caUserForm.reset();
    this.submitted = false;
    this.createdSuccessfully = false;
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.caUserForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }
}