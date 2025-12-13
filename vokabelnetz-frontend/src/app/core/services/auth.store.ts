import { Injectable, computed, signal } from '@angular/core';
import { AuthUser } from '../models';

/**
 * Auth store using Angular signals.
 * Access token is stored in memory only (not localStorage) for security.
 * Based on SECURITY.md documentation.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  // Private signals
  private readonly _accessToken = signal<string | null>(null);
  private readonly _refreshToken = signal<string | null>(null);
  private readonly _user = signal<AuthUser | null>(null);
  private readonly _isLoading = signal<boolean>(false);

  // Public readonly signals
  readonly accessToken = this._accessToken.asReadonly();
  readonly refreshToken = this._refreshToken.asReadonly();
  readonly user = this._user.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();

  // Computed signals
  readonly isAuthenticated = computed(() => !!this._accessToken() && !!this._user());
  readonly isAdmin = computed(() => this._user()?.role === 'ROLE_ADMIN');

  /**
   * Set authentication data after login/register.
   */
  setAuth(accessToken: string, refreshToken: string, user: AuthUser): void {
    this._accessToken.set(accessToken);
    this._refreshToken.set(refreshToken);
    this._user.set(user);
  }

  /**
   * Update tokens after refresh.
   */
  updateTokens(accessToken: string, refreshToken: string): void {
    this._accessToken.set(accessToken);
    this._refreshToken.set(refreshToken);
  }

  /**
   * Update user data.
   */
  updateUser(user: Partial<AuthUser>): void {
    const currentUser = this._user();
    if (currentUser) {
      this._user.set({ ...currentUser, ...user });
    }
  }

  /**
   * Clear all auth data on logout.
   */
  clearAuth(): void {
    this._accessToken.set(null);
    this._refreshToken.set(null);
    this._user.set(null);
  }

  /**
   * Set loading state.
   */
  setLoading(loading: boolean): void {
    this._isLoading.set(loading);
  }
}
