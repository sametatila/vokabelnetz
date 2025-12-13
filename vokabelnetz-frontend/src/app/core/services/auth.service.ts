import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStore } from './auth.store';
import {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  Session
} from '../models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authStore = inject(AuthStore);

  private readonly apiUrl = `${environment.apiUrl}/auth`;

  /**
   * Register a new user.
   */
  register(request: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    this.authStore.setLoading(true);
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/register`, request).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Login with email and password.
   */
  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    this.authStore.setLoading(true);
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.handleAuthSuccess(response.data);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Refresh access token using refresh token.
   */
  refreshTokens(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = this.authStore.refreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.authStore.updateTokens(response.data.accessToken, response.data.refreshToken);
        }
      }),
      catchError(error => {
        // If refresh fails, logout user
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout current session.
   */
  logout(): void {
    const refreshToken = this.authStore.refreshToken();
    if (refreshToken) {
      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).subscribe();
    }
    this.authStore.clearAuth();
    this.router.navigate(['/auth/login']);
  }

  /**
   * Logout from all devices.
   */
  logoutAll(): Observable<ApiResponse<{ message: string }>> {
    return this.http.post<ApiResponse<{ message: string }>>(`${this.apiUrl}/logout-all`, {}).pipe(
      tap(() => {
        this.authStore.clearAuth();
        this.router.navigate(['/auth/login']);
      })
    );
  }

  /**
   * Request password reset.
   */
  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<{ message: string }>> {
    return this.http.post<ApiResponse<{ message: string }>>(`${this.apiUrl}/forgot-password`, request);
  }

  /**
   * Reset password with token.
   */
  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<{ message: string }>> {
    return this.http.post<ApiResponse<{ message: string }>>(`${this.apiUrl}/reset-password`, request);
  }

  /**
   * Verify email with token.
   */
  verifyEmail(token: string): Observable<ApiResponse<{ message: string }>> {
    return this.http.get<ApiResponse<{ message: string }>>(`${this.apiUrl}/verify-email`, {
      params: { token }
    });
  }

  /**
   * Resend verification email.
   */
  resendVerification(): Observable<ApiResponse<{ message: string }>> {
    return this.http.post<ApiResponse<{ message: string }>>(`${this.apiUrl}/resend-verification`, {});
  }

  /**
   * Get active sessions.
   */
  getSessions(): Observable<ApiResponse<{ sessions: Session[]; totalSessions: number }>> {
    return this.http.get<ApiResponse<{ sessions: Session[]; totalSessions: number }>>(`${this.apiUrl}/sessions`);
  }

  /**
   * Revoke a specific session.
   */
  revokeSession(sessionId: number): Observable<ApiResponse<{ message: string }>> {
    return this.http.delete<ApiResponse<{ message: string }>>(`${this.apiUrl}/sessions/${sessionId}`);
  }

  /**
   * Handle successful authentication.
   */
  private handleAuthSuccess(data: AuthResponse): void {
    this.authStore.setAuth(data.accessToken, data.refreshToken, data.user);
    this.authStore.setLoading(false);
  }

  /**
   * Handle authentication errors.
   */
  private handleError(error: unknown): Observable<never> {
    this.authStore.setLoading(false);
    return throwError(() => error);
  }
}
