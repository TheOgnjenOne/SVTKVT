import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth';

export const RoleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRoles = route.data['roles'] as Array<string>;
  const currentUser = authService.getCurrentUser();

  if (currentUser && expectedRoles.includes(currentUser.role)) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};
