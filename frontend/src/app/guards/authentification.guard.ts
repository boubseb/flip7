import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthenticationService } from '../services/authentication/authentification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationGuard implements CanActivate {

  constructor(private authenticationSevice: AuthenticationService,
              private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    // SSR : localStorage absent → laisser passer, le guard client s'exécutera après hydratation
    if (typeof localStorage === 'undefined') return true;
    const token = this.authenticationSevice.getToken();
    if (token) return true;
    this.router.navigateByUrl('/');
    return false;
  }
}