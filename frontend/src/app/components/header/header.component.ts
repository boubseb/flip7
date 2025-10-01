import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RulesComponent } from '../rules/rules.component';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs/internal/Observable';
import { AuthenticationService } from '../../services/authentication/authentification.service';
import { ProfilComponent } from '../profil/profil.component';
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RulesComponent,
    ProfilComponent,
    RouterLink
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {

  constructor() { }

  isRules: boolean = false;

  authenticationService = inject(AuthenticationService);

  isLogin$: Observable<Boolean> = this.authenticationService.isLogin$;
  isProfil: boolean = false;
  isPrfil = this.isLogin$
  router = inject(Router);


  toggleIsRules() {
    this.isRules = !this.isRules;
  }
  toggleIsProfil() {
    this.isProfil = !this.isProfil;
  }

  onLogout(): void {
    this.authenticationService.removeToken();
    this.router.navigateByUrl('/');
  }

}


