import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SettingsService, UserPreferences, LanguageSettings } from '../../core/services/settings.service';
import { AuthStore } from '../../core/state/auth.store';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../core/models';

type SettingsTab = 'profile' | 'preferences' | 'language' | 'security';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-4xl mx-auto">

        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Settings</h1>
          <p class="mt-2 text-gray-600">Manage your account and preferences</p>
        </div>

        <!-- Tabs -->
        <div class="card mb-6">
          <div class="flex flex-wrap gap-2">
            <button
              (click)="activeTab.set('profile')"
              class="px-4 py-2 rounded-lg font-medium transition-colors"
              [class]="activeTab() === 'profile' ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            >
              Profile
            </button>
            <button
              (click)="activeTab.set('preferences')"
              class="px-4 py-2 rounded-lg font-medium transition-colors"
              [class]="activeTab() === 'preferences' ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            >
              Preferences
            </button>
            <button
              (click)="activeTab.set('language')"
              class="px-4 py-2 rounded-lg font-medium transition-colors"
              [class]="activeTab() === 'language' ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            >
              Language
            </button>
            <button
              (click)="activeTab.set('security')"
              class="px-4 py-2 rounded-lg font-medium transition-colors"
              [class]="activeTab() === 'security' ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            >
              Security
            </button>
          </div>
        </div>

        @if (loading()) {
          <div class="text-center py-12">
            <svg class="animate-spin h-8 w-8 text-primary-600 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
        } @else {

          <!-- Profile Tab -->
          @if (activeTab() === 'profile') {
            <div class="card">
              <h2 class="text-lg font-semibold text-gray-900 mb-6">Profile Information</h2>

              <!-- Avatar -->
              <div class="flex items-center gap-4 mb-6">
                <div class="w-20 h-20 rounded-full bg-primary-100 flex items-center justify-center">
                  @if (profile()?.avatarUrl) {
                    <img [src]="profile()!.avatarUrl" alt="Avatar" class="w-full h-full rounded-full object-cover" />
                  } @else {
                    <span class="text-3xl text-primary-600">
                      {{ getInitial() }}
                    </span>
                  }
                </div>
                <div>
                  <p class="font-medium text-gray-900">{{ profile()?.displayName }}</p>
                  <p class="text-sm text-gray-500">{{ profile()?.email }}</p>
                </div>
              </div>

              <!-- Display Name -->
              <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-1">Display Name</label>
                <input
                  type="text"
                  [(ngModel)]="displayName"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
              </div>

              <!-- Timezone -->
              <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-1">Timezone</label>
                <select
                  [(ngModel)]="timezone"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  <option value="Europe/Istanbul">Europe/Istanbul (UTC+3)</option>
                  <option value="Europe/Berlin">Europe/Berlin (UTC+1)</option>
                  <option value="Europe/London">Europe/London (UTC+0)</option>
                  <option value="America/New_York">America/New_York (UTC-5)</option>
                </select>
              </div>

              <!-- Stats -->
              <div class="grid grid-cols-2 gap-4 p-4 bg-gray-50 rounded-lg mb-6">
                <div>
                  <p class="text-sm text-gray-500">Member since</p>
                  <p class="font-medium">{{ formatDate(profile()?.createdAt) }}</p>
                </div>
                <div>
                  <p class="text-sm text-gray-500">Elo Rating</p>
                  <p class="font-medium">{{ profile()?.eloRating }}</p>
                </div>
              </div>

              <button
                (click)="saveProfile()"
                [disabled]="saving()"
                class="btn btn-primary"
              >
                {{ saving() ? 'Saving...' : 'Save Changes' }}
              </button>

              @if (successMessage()) {
                <p class="mt-3 text-sm text-green-600">{{ successMessage() }}</p>
              }
              @if (errorMessage()) {
                <p class="mt-3 text-sm text-red-600">{{ errorMessage() }}</p>
              }
            </div>
          }

          <!-- Preferences Tab -->
          @if (activeTab() === 'preferences') {
            <div class="card">
              <h2 class="text-lg font-semibold text-gray-900 mb-6">Learning Preferences</h2>

              <!-- Daily Goal -->
              <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-1">Daily Word Goal</label>
                <input
                  type="number"
                  [(ngModel)]="preferences.dailyWordGoal"
                  min="5"
                  max="100"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
                <p class="text-sm text-gray-500 mt-1">Number of words to learn each day</p>
              </div>

              <!-- Session Duration -->
              <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-1">Session Duration (minutes)</label>
                <input
                  type="number"
                  [(ngModel)]="preferences.sessionDurationMin"
                  min="5"
                  max="60"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
              </div>

              <!-- New Words Per Session -->
              <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-1">New Words Per Session</label>
                <input
                  type="number"
                  [(ngModel)]="preferences.newWordsPerSession"
                  min="1"
                  max="20"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
              </div>

              <h3 class="text-md font-medium text-gray-900 mb-4 mt-8">Display Options</h3>

              <!-- Toggle Options -->
              <div class="space-y-4">
                <label class="flex items-center justify-between">
                  <span class="text-gray-700">Show example sentences</span>
                  <input
                    type="checkbox"
                    [(ngModel)]="preferences.showExampleSentences"
                    class="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                  />
                </label>

                <label class="flex items-center justify-between">
                  <span class="text-gray-700">Show word type</span>
                  <input
                    type="checkbox"
                    [(ngModel)]="preferences.showWordType"
                    class="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                  />
                </label>

                <label class="flex items-center justify-between">
                  <span class="text-gray-700">Show article hints for nouns</span>
                  <input
                    type="checkbox"
                    [(ngModel)]="preferences.showArticleHints"
                    class="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                  />
                </label>

                <label class="flex items-center justify-between">
                  <span class="text-gray-700">Auto-play audio</span>
                  <input
                    type="checkbox"
                    [(ngModel)]="preferences.autoPlayAudio"
                    class="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                  />
                </label>

                <label class="flex items-center justify-between">
                  <span class="text-gray-700">Enable sound effects</span>
                  <input
                    type="checkbox"
                    [(ngModel)]="preferences.soundEnabled"
                    class="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                  />
                </label>
              </div>

              <h3 class="text-md font-medium text-gray-900 mb-4 mt-8">Notifications</h3>

              <label class="flex items-center justify-between mb-4">
                <span class="text-gray-700">Enable daily reminders</span>
                <input
                  type="checkbox"
                  [(ngModel)]="preferences.notificationEnabled"
                  class="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                />
              </label>

              @if (preferences.notificationEnabled) {
                <div class="mb-6">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Reminder Time</label>
                  <input
                    type="time"
                    [(ngModel)]="preferences.notificationTime"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>
              }

              <button
                (click)="savePreferences()"
                [disabled]="saving()"
                class="btn btn-primary mt-6"
              >
                {{ saving() ? 'Saving...' : 'Save Preferences' }}
              </button>

              @if (successMessage()) {
                <p class="mt-3 text-sm text-green-600">{{ successMessage() }}</p>
              }
              @if (errorMessage()) {
                <p class="mt-3 text-sm text-red-600">{{ errorMessage() }}</p>
              }
            </div>
          }

          <!-- Language Tab -->
          @if (activeTab() === 'language') {
            <div class="card">
              <h2 class="text-lg font-semibold text-gray-900 mb-6">Language Settings</h2>

              <!-- UI Language -->
              <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-1">Interface Language</label>
                <select
                  [(ngModel)]="languageSettings.uiLanguage"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  <option value="tr">Turkce</option>
                  <option value="en">English</option>
                  <option value="de">Deutsch</option>
                </select>
              </div>

              <!-- Source Language -->
              <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-1">Translation Language</label>
                <select
                  [(ngModel)]="languageSettings.sourceLanguage"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  <option value="tr">Turkce</option>
                  <option value="en">English</option>
                </select>
                <p class="text-sm text-gray-500 mt-1">Words will be translated to this language</p>
              </div>

              <!-- Show Both Translations -->
              <label class="flex items-center justify-between mb-6">
                <div>
                  <span class="text-gray-700">Show both translations</span>
                  <p class="text-sm text-gray-500">Display Turkish and English translations together</p>
                </div>
                <input
                  type="checkbox"
                  [(ngModel)]="languageSettings.showBothTranslations"
                  class="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                />
              </label>

              <button
                (click)="saveLanguageSettings()"
                [disabled]="saving()"
                class="btn btn-primary"
              >
                {{ saving() ? 'Saving...' : 'Save Language Settings' }}
              </button>

              @if (successMessage()) {
                <p class="mt-3 text-sm text-green-600">{{ successMessage() }}</p>
              }
              @if (errorMessage()) {
                <p class="mt-3 text-sm text-red-600">{{ errorMessage() }}</p>
              }
            </div>
          }

          <!-- Security Tab -->
          @if (activeTab() === 'security') {
            <div class="space-y-6">
              <!-- Change Password -->
              <div class="card">
                <h2 class="text-lg font-semibold text-gray-900 mb-6">Change Password</h2>

                <div class="mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
                  <input
                    type="password"
                    [(ngModel)]="currentPassword"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>

                <div class="mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                  <input
                    type="password"
                    [(ngModel)]="newPassword"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>

                <div class="mb-6">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
                  <input
                    type="password"
                    [(ngModel)]="confirmPassword"
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>

                <button
                  (click)="changePassword()"
                  [disabled]="saving() || !currentPassword || !newPassword || newPassword !== confirmPassword"
                  class="btn btn-primary"
                >
                  {{ saving() ? 'Changing...' : 'Change Password' }}
                </button>

                @if (passwordSuccess()) {
                  <p class="mt-3 text-sm text-green-600">{{ passwordSuccess() }}</p>
                }
                @if (passwordError()) {
                  <p class="mt-3 text-sm text-red-600">{{ passwordError() }}</p>
                }
              </div>

              <!-- Logout -->
              <div class="card">
                <h2 class="text-lg font-semibold text-gray-900 mb-4">Session</h2>
                <p class="text-gray-600 mb-4">Sign out of your account on this device.</p>
                <button
                  (click)="logout()"
                  class="btn bg-gray-100 hover:bg-gray-200 text-gray-700"
                >
                  Logout
                </button>
              </div>

              <!-- Delete Account -->
              <div class="card border-red-200">
                <h2 class="text-lg font-semibold text-red-600 mb-4">Delete Account</h2>
                <p class="text-gray-600 mb-4">
                  Permanently delete your account and all associated data. This action cannot be undone.
                </p>
                <button
                  (click)="showDeleteModal.set(true)"
                  class="btn bg-red-100 hover:bg-red-200 text-red-700"
                >
                  Delete My Account
                </button>
              </div>
            </div>
          }

        }

        <!-- Delete Account Modal -->
        @if (showDeleteModal()) {
          <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" (click)="showDeleteModal.set(false)">
            <div class="bg-white rounded-2xl shadow-xl max-w-md w-full p-6" (click)="$event.stopPropagation()">
              <h3 class="text-xl font-bold text-red-600 mb-4">Delete Account</h3>
              <p class="text-gray-600 mb-4">
                Are you sure you want to delete your account? This will permanently remove all your data after 30 days.
              </p>

              <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-1">Reason (optional)</label>
                <textarea
                  [(ngModel)]="deleteReason"
                  rows="3"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                  placeholder="Help us improve by telling us why you're leaving..."
                ></textarea>
              </div>

              <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-1">Confirm your email</label>
                <input
                  type="email"
                  [(ngModel)]="confirmEmail"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                  [placeholder]="profile()?.email"
                />
              </div>

              <div class="flex gap-3">
                <button
                  (click)="showDeleteModal.set(false)"
                  class="flex-1 btn bg-gray-100 hover:bg-gray-200 text-gray-700"
                >
                  Cancel
                </button>
                <button
                  (click)="deleteAccount()"
                  [disabled]="confirmEmail !== profile()?.email"
                  class="flex-1 btn bg-red-600 hover:bg-red-700 text-white disabled:opacity-50"
                >
                  Delete Account
                </button>
              </div>
            </div>
          </div>
        }

      </div>
    </div>
  `
})
export class SettingsComponent implements OnInit {
  private readonly settingsService = inject(SettingsService);
  private readonly authStore = inject(AuthStore);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // State
  readonly activeTab = signal<SettingsTab>('profile');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly profile = signal<UserProfile | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly passwordSuccess = signal<string | null>(null);
  readonly passwordError = signal<string | null>(null);
  readonly showDeleteModal = signal(false);

  // Profile form
  displayName = '';
  timezone = 'Europe/Istanbul';

  // Preferences form
  preferences: Partial<UserPreferences> = {
    dailyWordGoal: 20,
    sessionDurationMin: 15,
    newWordsPerSession: 5,
    notificationEnabled: false,
    notificationTime: '09:00',
    soundEnabled: true,
    autoPlayAudio: false,
    showExampleSentences: true,
    showWordType: true,
    showArticleHints: true
  };

  // Language form
  languageSettings: Partial<LanguageSettings> = {
    uiLanguage: 'tr',
    sourceLanguage: 'tr',
    showBothTranslations: false
  };

  // Security form
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  deleteReason = '';
  confirmEmail = '';

  ngOnInit(): void {
    this.loadProfile();
    this.loadPreferences();
    this.loadLanguageSettings();
  }

  private loadProfile(): void {
    this.settingsService.getProfile().subscribe({
      next: (response) => {
        if (response.success) {
          this.profile.set(response.data);
          this.displayName = response.data.displayName;
          this.timezone = response.data.timezone;
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private loadPreferences(): void {
    this.settingsService.getPreferences().subscribe({
      next: (response) => {
        if (response.success) {
          this.preferences = { ...this.preferences, ...response.data };
        }
      }
    });
  }

  private loadLanguageSettings(): void {
    this.settingsService.getLanguageSettings().subscribe({
      next: (response) => {
        if (response.success) {
          this.languageSettings = { ...this.languageSettings, ...response.data };
        }
      }
    });
  }

  saveProfile(): void {
    this.clearMessages();
    this.saving.set(true);

    this.settingsService.updateProfile({
      displayName: this.displayName,
      timezone: this.timezone
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.profile.set(response.data);
          this.successMessage.set('Profile updated successfully');
        }
        this.saving.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message || 'Failed to update profile');
        this.saving.set(false);
      }
    });
  }

  savePreferences(): void {
    this.clearMessages();
    this.saving.set(true);

    this.settingsService.updatePreferences(this.preferences).subscribe({
      next: (response) => {
        if (response.success) {
          this.preferences = response.data;
          this.successMessage.set('Preferences saved successfully');
        }
        this.saving.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message || 'Failed to save preferences');
        this.saving.set(false);
      }
    });
  }

  saveLanguageSettings(): void {
    this.clearMessages();
    this.saving.set(true);

    this.settingsService.updateLanguageSettings(this.languageSettings).subscribe({
      next: (response) => {
        if (response.success) {
          this.languageSettings = response.data;
          this.successMessage.set('Language settings saved successfully');
        }
        this.saving.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message || 'Failed to save language settings');
        this.saving.set(false);
      }
    });
  }

  changePassword(): void {
    this.passwordSuccess.set(null);
    this.passwordError.set(null);

    if (this.newPassword !== this.confirmPassword) {
      this.passwordError.set('Passwords do not match');
      return;
    }

    this.saving.set(true);

    this.settingsService.changePassword({
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.passwordSuccess.set('Password changed successfully');
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.saving.set(false);
      },
      error: (error) => {
        this.passwordError.set(error?.error?.error?.message || 'Failed to change password');
        this.saving.set(false);
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  deleteAccount(): void {
    const email = this.profile()?.email;
    if (!email || this.confirmEmail !== email) {
      return;
    }

    this.settingsService.deleteAccount({
      reason: this.deleteReason,
      confirmEmail: this.confirmEmail
    }).subscribe({
      next: () => {
        this.authStore.clearAuth();
        this.router.navigate(['/auth/login']);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message || 'Failed to delete account');
        this.showDeleteModal.set(false);
      }
    });
  }

  private clearMessages(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
  }

  formatDate(dateString?: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  }

  getInitial(): string {
    const name = this.profile()?.displayName;
    return name ? name.charAt(0).toUpperCase() : '';
  }
}
