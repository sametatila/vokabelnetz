import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, UserProfile } from '../models';
import { environment } from '../../../environments/environment';

// User Preferences
export interface UserPreferences {
  uiLanguage: string;
  sourceLanguage: string;
  showBothTranslations: boolean;
  primaryTranslation: string;
  dailyWordGoal: number;
  sessionDurationMin: number;
  newWordsPerSession: number;
  notificationEnabled: boolean;
  notificationTime: string;
  soundEnabled: boolean;
  darkMode: boolean;
  showPronunciation: boolean;
  autoPlayAudio: boolean;
  showExampleSentences: boolean;
  showWordType: boolean;
  showArticleHints: boolean;
}

// Language Settings
export interface LanguageSettings {
  uiLanguage: string;
  sourceLanguage: string;
  targetLanguage: string;
  showBothTranslations: boolean;
  primaryTranslation: string;
}

// Update Profile Request
export interface UpdateProfileRequest {
  displayName?: string;
  avatarUrl?: string;
  timezone?: string;
}

// Change Password Request
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// Delete Account Request
export interface DeleteAccountRequest {
  reason: string;
  confirmEmail: string;
}

/**
 * SettingsService - Handles user settings and preferences API calls.
 *
 * Based on docs/API.md User Endpoints.
 */
@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Get current user profile.
   */
  getProfile(): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.apiUrl}/users/me`);
  }

  /**
   * Update user profile.
   */
  updateProfile(data: UpdateProfileRequest): Observable<ApiResponse<UserProfile>> {
    return this.http.put<ApiResponse<UserProfile>>(`${this.apiUrl}/users/me`, data);
  }

  /**
   * Change password.
   */
  changePassword(data: ChangePasswordRequest): Observable<ApiResponse<{ message: string }>> {
    return this.http.put<ApiResponse<{ message: string }>>(`${this.apiUrl}/users/me/password`, data);
  }

  /**
   * Get user preferences.
   */
  getPreferences(): Observable<ApiResponse<UserPreferences>> {
    return this.http.get<ApiResponse<UserPreferences>>(`${this.apiUrl}/users/me/preferences`);
  }

  /**
   * Update user preferences.
   */
  updatePreferences(data: Partial<UserPreferences>): Observable<ApiResponse<UserPreferences>> {
    return this.http.put<ApiResponse<UserPreferences>>(`${this.apiUrl}/users/me/preferences`, data);
  }

  /**
   * Get language settings.
   */
  getLanguageSettings(): Observable<ApiResponse<LanguageSettings>> {
    return this.http.get<ApiResponse<LanguageSettings>>(`${this.apiUrl}/users/me/language`);
  }

  /**
   * Update language settings.
   */
  updateLanguageSettings(data: Partial<LanguageSettings>): Observable<ApiResponse<LanguageSettings>> {
    return this.http.put<ApiResponse<LanguageSettings>>(`${this.apiUrl}/users/me/language`, data);
  }

  /**
   * Quick switch source language.
   */
  switchSourceLanguage(sourceLanguage: string): Observable<ApiResponse<LanguageSettings>> {
    return this.http.patch<ApiResponse<LanguageSettings>>(
      `${this.apiUrl}/users/me/language/source`,
      { sourceLanguage }
    );
  }

  /**
   * Delete account (soft delete).
   */
  deleteAccount(data: DeleteAccountRequest): Observable<ApiResponse<{ deletedAt: string; permanentDeletionAt: string }>> {
    return this.http.delete<ApiResponse<{ deletedAt: string; permanentDeletionAt: string }>>(
      `${this.apiUrl}/users/me`,
      { body: data }
    );
  }
}
