import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';

interface OrganizationView {
  name: string;
  registrationNumber: string;
  address: string;
  city: string;
  country: string;
  emailDomain: string;
}

@Component({
  selector: 'app-organizations-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShell],
  templateUrl: './organizations-page.html',
  styleUrl: './organizations-page.css'
})
export class OrganizationsPage {
  navItems = NAVIGATION_BY_ROLE.ADMIN;

  profile = {
    fullName: 'Admin Administrator',
    roleLabel: 'Administrator',
    organization: 'System Administration',
    email: 'admin@pki.local'
  };

  organizationForm: FormGroup;
  submitted = false;
  createdSuccessfully = false;

  organizations: OrganizationView[] = [
    {
      name: 'Komsiluk Soft',
      registrationNumber: '100200300',
      address: 'Bulevar Oslobodjenja 12',
      city: 'Novi Sad',
      country: 'Serbia',
      emailDomain: 'komsiluk.rs'
    },
    {
      name: 'PKI Labs',
      registrationNumber: '400500600',
      address: 'Futoska 21',
      city: 'Novi Sad',
      country: 'Serbia',
      emailDomain: 'pkilabs.rs'
    }
  ];

  constructor(private fb: FormBuilder) {
    this.organizationForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      registrationNumber: ['', [Validators.required, Validators.minLength(6)]],
      address: ['', [Validators.required, Validators.minLength(3)]],
      city: ['', [Validators.required, Validators.minLength(2)]],
      country: ['', [Validators.required, Validators.minLength(2)]],
      emailDomain: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.createdSuccessfully = false;

    if (this.organizationForm.invalid) {
      this.organizationForm.markAllAsTouched();
      return;
    }

    this.organizations = [
      {
        name: this.organizationForm.value.name,
        registrationNumber: this.organizationForm.value.registrationNumber,
        address: this.organizationForm.value.address,
        city: this.organizationForm.value.city,
        country: this.organizationForm.value.country,
        emailDomain: this.organizationForm.value.emailDomain
      },
      ...this.organizations
    ];

    this.organizationForm.reset();
    this.submitted = false;
    this.createdSuccessfully = true;
  }

  onReset(): void {
    this.organizationForm.reset();
    this.submitted = false;
    this.createdSuccessfully = false;
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.organizationForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }
}