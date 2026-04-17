import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppShell } from '../../../../shared/components/app-shell/app-shell';
import { NAVIGATION_BY_ROLE } from '../../../../core/config/role-navigation';
import { Organization } from '../../../organizations/models/organization.model';
import { OrganizationsService } from '../../../organizations/services/organizations.service';
import { CaUser } from '../../models/ca-user.model';
import { CaUsersService } from '../../services/ca-users.service';

@Component({
  selector: 'app-ca-users-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShell],
  templateUrl: './ca-users-page.html',
  styleUrl: './ca-users-page.css'
})
export class CaUsersPage implements OnInit {
  private fb = inject(FormBuilder);
  private organizationsService = inject(OrganizationsService);
  private caUsersService = inject(CaUsersService);

  navItems = NAVIGATION_BY_ROLE.ADMIN;

  profile = {
    fullName: 'Administrator session',
    roleLabel: 'Administrator',
    organization: 'PKI system'
  };

  caUserForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    organizationId: ['', [Validators.required]]
  });

  submitted = false;
  createdSuccessfully = false;
  loading = false;
  loadingOrganizations = false;
  loadingCaUsers = false;
  errorMessage = '';

  organizations: Organization[] = [];
  caUsers: CaUser[] = [];

  ngOnInit(): void {
    this.loadOrganizations();
    this.loadCaUsers();
  }

  loadOrganizations(): void {
    this.loadingOrganizations = true;

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

  loadCaUsers(): void {
    this.loadingCaUsers = true;

    this.caUsersService.getAll().subscribe({
      next: (caUsers) => {
        this.caUsers = caUsers;
        this.loadingCaUsers = false;
      },
      error: (error) => {
        this.loadingCaUsers = false;
        this.errorMessage = error?.error?.message || 'Failed to load CA users.';
      }
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.createdSuccessfully = false;
    this.errorMessage = '';

    if (this.caUserForm.invalid) {
      this.caUserForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const payload = {
      firstName: this.caUserForm.value.firstName,
      lastName: this.caUserForm.value.lastName,
      email: this.caUserForm.value.email,
      organizationId: Number(this.caUserForm.value.organizationId)
    };

    this.caUsersService.create(payload).subscribe({
      next: (createdUser) => {
        this.caUsers = [createdUser, ...this.caUsers];
        this.caUserForm.reset();
        this.submitted = false;
        this.createdSuccessfully = true;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error?.error?.message || 'Failed to create CA user.';
      }
    });
  }

  onReset(): void {
    this.caUserForm.reset();
    this.submitted = false;
    this.createdSuccessfully = false;
    this.errorMessage = '';
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.caUserForm.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || this.submitted);
  }

  getStatusLabel(active: boolean): string {
    return active ? 'Active' : 'Pending activation';
  }
}