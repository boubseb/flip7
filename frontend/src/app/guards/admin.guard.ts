import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from '../services/authentication/authentification.service';

export const AdminGuard: CanActivateFn = () => {
  const auth = inject(AuthenticationService);
  const router = inject(Router);
  if (auth.isAdmin()) return true;
  router.navigate(['/home']);
  return false;
};
