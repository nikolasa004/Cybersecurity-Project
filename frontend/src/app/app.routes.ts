import { Routes } from '@angular/router';
import { LandingPage } from './pages/landing-page/landing-page';
import { AdminDashboardPage } from './pages/admin-dashboard-page/admin-dashboard-page';
import { CaDashboardPage } from './pages/ca-dashboard-page/ca-dashboard-page';
import { UserDashboardPage } from './pages/user-dashboard-page/user-dashboard-page';
import { OrganizationsPage } from './features/organizations/pages/organizations-page/organizations-page';
import { CaUsersPage } from './features/ca-users/pages/ca-users-page/ca-users-page';
import { ActivateAccountPage } from './features/auth/pages/activate-account-page/activate-account-page';
import { AdminCertificatesPage } from './features/certificates/pages/admin-certificates-page/admin-certificates-page';
import { IssueCertificatePage } from './features/certificates/pages/issue-certificate-page/issue-certificate-page';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { LoginPage } from './features/auth/pages/login-page/login-page';
import { roleGuard } from './core/guards/role.guard';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingPage },

  {
    path: 'auth',
    component: AuthLayout,
    children: [
      { path: 'login', component: LoginPage }
    ]
  },

  { 
    path: 'admin', 
    canActivate: [authGuard, roleGuard], 
    data: { roles: ['ADMIN'] },
    children: [
      { path: '', component: AdminDashboardPage },
      { path: 'organizations', component: OrganizationsPage },
      { path: 'ca-users', component: CaUsersPage },
      { path: 'certificates', component: AdminCertificatesPage },
      { path: 'certificates/issue', component: IssueCertificatePage },
    ]
  },

  { 
    path: 'ca', 
    canActivate: [authGuard, roleGuard], 
    data: { roles: ['CA_USER'] },
    children: [
      { path: '', component: CaDashboardPage },
      { path: 'certificates', component: AdminCertificatesPage },
      { path: 'certificates/issue', component: IssueCertificatePage }
    ]
  },
  { 
    path: 'user', 
    component: UserDashboardPage, 
    canActivate: [authGuard, roleGuard], 
    data: { roles: ['USER'] } 
  },

  { path: 'activate', component: ActivateAccountPage },

  { path: '**', redirectTo: '' }
];