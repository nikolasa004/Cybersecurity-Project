import { Routes } from '@angular/router';
import { LandingPage } from './pages/landing-page/landing-page';
import { AdminDashboardPage } from './pages/admin-dashboard-page/admin-dashboard-page';
import { CaDashboardPage } from './pages/ca-dashboard-page/ca-dashboard-page';
import { UserDashboardPage } from './pages/user-dashboard-page/user-dashboard-page';
import { OrganizationsPage } from './features/organizations/pages/organizations-page/organizations-page';
import { CaUsersPage } from './features/ca-users/pages/ca-users-page/ca-users-page';
import { ActivateAccountPage } from './features/auth/pages/activate-account-page/activate-account-page';

export const routes: Routes = [
  { path: '', component: LandingPage },

  { path: 'admin', component: AdminDashboardPage },
  { path: 'admin/organizations', component: OrganizationsPage },
  { path: 'admin/ca-users', component: CaUsersPage },

  { path: 'ca', component: CaDashboardPage },
  { path: 'user', component: UserDashboardPage },
  
  { path: 'activate', component: ActivateAccountPage },

  { path: '**', redirectTo: '' }
];