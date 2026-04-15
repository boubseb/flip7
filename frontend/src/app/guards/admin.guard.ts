import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from '../services/authentication/authentification.service';

export const AdminGuard: CanActivateFn = () => {
  const auth = inject(AuthenticationService);
  const router = inject(Router);
  // Pendant le SSR, localStorage n'existe pas — on laisse passer,
  // le guard client-side s'exécutera après hydratation.
  if (!isPlatformBrowser(inject(PLATFORM_ID))) return true;
  if (auth.isAdmin()) return true;
  router.navigate(['/home']);
  return false;
};
