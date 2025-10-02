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
  isProfilMenuOpen: boolean = false;


  toggleIsRules() {
    this.isRules = !this.isRules;
    // Fermer le profil quand on ouvre les règles
    if (this.isRules) {
      this.isProfil = false;
    }
  }
  
  toggleIsProfil() {
    this.isProfil = !this.isProfil;
  }

  toggleProfilMenu() {
    this.isProfilMenuOpen = !this.isProfilMenuOpen;
    // Fermer les règles quand on ouvre le menu profil
    if (this.isProfilMenuOpen) {
      this.isRules = false;
    }
  }

  openProfil() {
    this.isProfilMenuOpen = false;
    this.isProfil = true;
    // Fermer les règles si elles sont ouvertes
    this.isRules = false;
  }

  openStats() {
    this.isProfilMenuOpen = false;
    // TODO: Implement stats page
    console.log('Opening stats...');
    // Fermer les règles et le profil si ouverts
    this.isRules = false;
    this.isProfil = false;
  }

  onTitleClick(event: Event) {
    // Vérifier si on est sur la page game
    const currentUrl = this.router.url;
    if (currentUrl.includes('/game/')) {
      // Empêcher la navigation si on est sur la page game
      event.preventDefault();
      console.log('Navigation désactivée sur la page game');
      return;
    }
    
    // Fermer tous les overlays quand on clique sur le titre
    this.isProfil = false;
    this.isRules = false;
    this.isProfilMenuOpen = false;
  }

  onLogout(): void {
    // Fermer tous les overlays
    this.isProfilMenuOpen = false;
    this.isProfil = false;
    this.isRules = false;
    
    // Supprimer le token
    this.authenticationService.removeToken();
    
    // Rediriger vers l'accueil
    this.router.navigateByUrl('/');
  }

}


