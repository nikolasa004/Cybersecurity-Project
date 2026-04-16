import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { Organization } from '../../models/organization.model';
import { OrganizationsService } from '../../services/organizations.service';

@Component({
  selector: 'app-organizations-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShell],
  templateUrl: './organizations-page.html',
  styleUrl: './organizations-page.css'
})
export class OrganizationsPage implements OnInit {
  private fb = inject(FormBuilder);
  private organizationsService = inject(OrganizationsService);

  navItems = NAVIGATION_BY_ROLE.ADMIN;

  profile = {
    fullName: 'Administrator session',
    roleLabel: 'Administrator',
    organization: 'PKI system'
  };

  organizationForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    registrationNumber: ['', [Validators.required, Validators.minLength(6)]],
    address: ['', [Validators.required, Validators.minLength(3)]],
    city: ['', [Validators.required, Validators.minLength(2)]],
    country: ['', [Validators.required, Validators.minLength(2)]],
    emailDomain: ['', [Validators.required, Validators.minLength(3)]]
  });

  submitted = false;
  createdSuccessfully = false;
  loading = false;
  loadingOrganizations = false;
  errorMessage = '';

  organizations: Organization[] = [];

  ngOnInit(): void {
    this.loadOrganizations();
  }

  loadOrganizations(): void {
    this.loadingOrganizations = true;
    this.errorMessage = '';

    this.organizationsService.getAll().subscribe({
      next: (organizations) => {
        this.organizations = organizations;
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
    this.createdSuccessfully = false;
    this.errorMessage = '';

    if (this.organizationForm.invalid) {
      this.organizationForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.organizationsService.create(this.organizationForm.getRawValue()).subscribe({
      next: (createdOrganization) => {
        this.organizations = [createdOrganization, ...this.organizations];
        this.organizationForm.reset();
        this.submitted = false;
        this.createdSuccessfully = true;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Failed to create organization.';
      }
    });
  }

  onReset(): void {
    this.organizationForm.reset();
    this.submitted = false;
    this.createdSuccessfully = false;
    this.errorMessage = '';
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.organizationForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }
}