import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, inject } from '@angular/core';
import { RulesComponent } from '../rules/rules.component';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs/internal/Observable';
import { AuthenticationService } from '../../services/authentication/authentification.service';
import { ProfilComponent } from '../profil/profil.component';
import { StatisticsComponent } from '../statistics/statistics.component';
import { GameHistoryComponent } from '../game-history/game-history.component';
import { AdminPageComponent } from '../../pages/admin-page/admin-page.component';
import { ThemeService, Theme } from '../../services/theme/theme.service';
import { LanguageService, Language } from '../../services/language/language.service';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RulesComponent,
    ProfilComponent,
    StatisticsComponent,
    GameHistoryComponent,
    AdminPageComponent,
    RouterLink,
    TranslateModule
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {

  private elementRef = inject(ElementRef);

  constructor() { }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.isProfilMenuOpen && !this.elementRef.nativeElement.contains(event.target)) {
      this.isProfilMenuOpen = false;
    }
  }

  isRules: boolean = false;
  isStatistics: boolean = false;
  isHistory: boolean = false;
  isAdminOpen: boolean = false;

  authenticationService = inject(AuthenticationService);
  themeService = inject(ThemeService);
  languageService = inject(LanguageService);

  isLogin$: Observable<Boolean> = this.authenticationService.isLogin$;
  currentTheme$: Observable<Theme> = this.themeService.theme$;
  currentLanguage$: Observable<Language> = this.languageService.language$;
  
  isProfil: boolean = false;
  isPrfil = this.isLogin$
  router = inject(Router);
  isProfilMenuOpen: boolean = false;


  private closeAll(): void {
    this.isRules = false;
    this.isProfil = false;
    this.isStatistics = false;
    this.isHistory = false;
    this.isAdminOpen = false;
    this.isProfilMenuOpen = false;
  }

  toggleIsRules() {
    const next = !this.isRules;
    this.closeAll();
    this.isRules = next;
  }

  toggleIsProfil() {
    const next = !this.isProfil;
    this.closeAll();
    this.isProfil = next;
  }

  toggleIsStatistics() {
    const next = !this.isStatistics;
    this.closeAll();
    this.isStatistics = next;
  }

  toggleProfilMenu() {
    const next = !this.isProfilMenuOpen;
    this.closeAll();
    this.isProfilMenuOpen = next;
  }

  openProfil() {
    this.closeAll();
    this.isProfil = true;
  }

  openStats() {
    this.closeAll();
    this.isStatistics = true;
  }

  openHistory() {
    this.closeAll();
    this.isHistory = true;
  }

  toggleIsHistory() {
    const next = !this.isHistory;
    this.closeAll();
    this.isHistory = next;
  }

  onTitleClick(event: Event) {
    // Vérifier si on est sur la page game
    const currentUrl = this.router.url;
    if (currentUrl.includes('/game/')) {
      // Empêcher la navigation si on est sur la page game
      event.preventDefault();
      return;
    }
    
    // Fermer tous les overlays quand on clique sur le titre
    this.isProfil = false;
    this.isRules = false;
    this.isStatistics = false;
    this.isHistory = false;
    this.isAdminOpen = false;
    this.isProfilMenuOpen = false;
  }

  isAdmin(): boolean {
    return this.authenticationService.isAdmin();
  }

  goToAdmin(): void {
    this.isProfilMenuOpen = false;
    this.isAdminOpen = true;
    this.isRules = false;
    this.isProfil = false;
    this.isStatistics = false;
    this.isHistory = false;
  }

  onLogout(): void {
    // Fermer tous les overlays
    this.isProfilMenuOpen = false;
    this.isProfil = false;
    this.isRules = false;
    this.isStatistics = false;
    this.isHistory = false;

    this.isAdminOpen = false;

    // Supprimer le token
    this.authenticationService.removeToken();

    // Rediriger vers l'accueil
    this.router.navigateByUrl('/');
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  toggleLanguage(): void {
    this.languageService.toggleLanguage();
  }

}


