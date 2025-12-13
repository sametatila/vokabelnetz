import { Injectable, signal, inject, effect } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type UiLanguage = 'tr' | 'en' | 'de';
export type SourceLanguage = 'tr' | 'en';

interface LanguageSettings {
  ui: UiLanguage;
  source: SourceLanguage;
  showBoth: boolean;
}

/**
 * LanguageService - Manages UI and learning language preferences.
 *
 * UI Language: The language of the interface (menus, buttons, messages)
 * Source Language: The language to translate FROM when learning German
 *
 * Based on docs/ARCHITECTURE.md Language System.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);

  // Signals for reactive state
  readonly currentUiLanguage = signal<UiLanguage>(this.detectDefaultLanguage());
  readonly currentSourceLanguage = signal<SourceLanguage>(this.detectDefaultSourceLanguage());
  readonly showBothTranslations = signal<boolean>(false);

  constructor() {
    this.loadPersistedSettings();

    // Set up available languages
    this.translate.addLangs(['en', 'tr', 'de']);
    this.translate.setDefaultLang('en');

    // Sync TranslateService with current UI language
    this.translate.use(this.currentUiLanguage());

    // Keep TranslateService in sync when UI language changes
    effect(() => {
      this.translate.use(this.currentUiLanguage());
    });
  }

  /**
   * Detect default UI language from browser/system settings.
   * Returns 'tr' if Turkish, 'en' otherwise (fallback).
   */
  private detectDefaultLanguage(): UiLanguage {
    const browserLang = navigator.language?.toLowerCase() || '';

    if (browserLang.startsWith('tr')) {
      return 'tr';
    }

    // Default to English for all other languages
    return 'en';
  }

  /**
   * Detect default source language (learning FROM).
   * Same logic as UI language.
   */
  private detectDefaultSourceLanguage(): SourceLanguage {
    const browserLang = navigator.language?.toLowerCase() || '';

    if (browserLang.startsWith('tr')) {
      return 'tr';
    }

    return 'en';
  }

  /**
   * Set UI language.
   */
  setUiLanguage(lang: UiLanguage): void {
    this.currentUiLanguage.set(lang);
    this.persistSettings();
  }

  /**
   * Set source language (learning FROM).
   */
  setSourceLanguage(lang: SourceLanguage): void {
    this.currentSourceLanguage.set(lang);
    this.persistSettings();
  }

  /**
   * Toggle showing both translations.
   */
  setShowBothTranslations(show: boolean): void {
    this.showBothTranslations.set(show);
    this.persistSettings();
  }

  /**
   * Get word translation based on current source language.
   */
  getWordTranslation(word: { turkish: string; english: string }): string {
    return this.currentSourceLanguage() === 'tr'
      ? word.turkish
      : word.english;
  }

  /**
   * Persist settings to localStorage.
   */
  private persistSettings(): void {
    const settings: LanguageSettings = {
      ui: this.currentUiLanguage(),
      source: this.currentSourceLanguage(),
      showBoth: this.showBothTranslations()
    };
    localStorage.setItem('language_settings', JSON.stringify(settings));
  }

  /**
   * Load persisted settings from localStorage.
   */
  private loadPersistedSettings(): void {
    const saved = localStorage.getItem('language_settings');
    if (saved) {
      try {
        const settings: LanguageSettings = JSON.parse(saved);
        this.currentUiLanguage.set(settings.ui);
        this.currentSourceLanguage.set(settings.source);
        this.showBothTranslations.set(settings.showBoth);
      } catch {
        // Invalid JSON, ignore
      }
    }
  }
}
