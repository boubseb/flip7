import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';

export type Language = 'fr' | 'en';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  private readonly LANGUAGE_KEY = 'flip7-language';
  private languageSubject = new BehaviorSubject<Language>(this.getInitialLanguage());
  
  public language$ = this.languageSubject.asObservable();

  constructor(private translate: TranslateService) {
    this.initTranslation();
  }

  private initTranslation(): void {
    this.translate.addLangs(['fr', 'en']);
    this.translate.setDefaultLang('fr');
    
    const lang = this.languageSubject.value;
    this.translate.use(lang);
  }

  private getInitialLanguage(): Language {
    // Vérifier si on est dans un navigateur
    if (typeof window === 'undefined' || typeof localStorage === 'undefined') {
      return 'fr';
    }
    
    const saved = localStorage.getItem(this.LANGUAGE_KEY);
    if (saved === 'fr' || saved === 'en') {
      return saved;
    }
    // Détection de la langue du navigateur
    const browserLang = navigator.language.split('-')[0];
    return browserLang === 'fr' ? 'fr' : 'en';
  }

  public toggleLanguage(): void {
    const newLang: Language = this.languageSubject.value === 'fr' ? 'en' : 'fr';
    this.setLanguage(newLang);
  }

  public setLanguage(lang: Language): void {
    this.languageSubject.next(lang);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.LANGUAGE_KEY, lang);
    }
    this.translate.use(lang);
  }

  public getCurrentLanguage(): Language {
    return this.languageSubject.value;
  }

  public translate$(key: string, params?: any) {
    return this.translate.get(key, params);
  }
}
