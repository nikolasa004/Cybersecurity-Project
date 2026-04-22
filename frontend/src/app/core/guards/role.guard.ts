import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../../features/auth/services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  const expectedRoles = route.data['roles'] as Array<string>;
  const userRole = authService.getRole();

  if (authService.isLoggedIn() && expectedRoles.includes(userRole || '')) {
    return true;
  }

  router.navigate(['/']); 
  return false;
};