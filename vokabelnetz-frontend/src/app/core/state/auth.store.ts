import { Injectable, computed, signal } from '@angular/core';
import { AuthUser } from '../models';

export interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  loading: boolean;
  error: string | null;
  initialized: boolean;
}

/**
 * AuthStore - Manages authentication state using Angular Signals.
 *
 * SECURITY: Access token stored in memory ONLY (not localStorage).
 * Refresh token managed via HttpOnly cookie by the server.
 * See docs/SECURITY.md for detailed token management strategy.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  // Private writable state
  private state = signal<AuthState>({
    user: null,
    accessToken: null,
    loading: false,
    error: null,
    initialized: false
  });

  // Public read-only selectors
  readonly user = computed(() => this.state().user);
  readonly accessToken = computed(() => this.state().accessToken);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
  readonly isInitialized = computed(() => this.state().initialized);

  // Computed derived state
  readonly isAuthenticated = computed(() => !!this.state().accessToken && !!this.state().user);
  readonly isAdmin = computed(() => this.state().user?.role === 'ROLE_ADMIN');
  readonly displayName = computed(() => this.state().user?.displayName || 'Guest');

  // Legacy compatibility aliases
  readonly isLoading = this.loading;

  /**
   * Set authentication data after login/register.
   */
  setAuth(accessToken: string, refreshToken: string, user: AuthUser): void {
    this.state.update(s => ({
      ...s,
      accessToken,
      user,
      error: null,
      initialized: true
    }));
    // Note: refreshToken should be handled via HttpOnly cookie by the server
  }

  /**
   * Update tokens after refresh.
   */
  updateTokens(accessToken: string, refreshToken: string): void {
    this.state.update(s => ({
      ...s,
      accessToken
    }));
  }

  /**
   * Update user data.
   */
  updateUser(user: Partial<AuthUser>): void {
    this.state.update(s => ({
      ...s,
      user: s.user ? { ...s.user, ...user } : null
    }));
  }

  /**
   * Set loading state.
   */
  setLoading(loading: boolean): void {
    this.state.update(s => ({ ...s, loading }));
  }

  /**
   * Set error state.
   */
  setError(error: string | null): void {
    this.state.update(s => ({ ...s, error }));
  }

  /**
   * Mark auth as initialized (after app startup check).
   */
  setInitialized(initialized: boolean): void {
    this.state.update(s => ({ ...s, initialized }));
  }

  /**
   * Clear all auth data on logout.
   */
  clearAuth(): void {
    this.state.set({
      user: null,
      accessToken: null,
      loading: false,
      error: null,
      initialized: true
    });
  }

  /**
   * Get current access token (for interceptor).
   */
  getAccessToken(): string | null {
    return this.state().accessToken;
  }

  /**
   * Get refresh token - deprecated, use HttpOnly cookie instead.
   * @deprecated Server should handle refresh token via HttpOnly cookie
   */
  refreshToken(): string | null {
    console.warn('refreshToken() is deprecated. Server should use HttpOnly cookie.');
    return null;
  }
}
