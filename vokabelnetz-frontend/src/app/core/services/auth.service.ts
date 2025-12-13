import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, finalize } from 'rxjs';
import { AuthStore } from '../state/auth.store';
import { LoginRequest, RegisterRequest, ForgotPasswordRequest, ResetPasswordRequest, AuthResponse, ApiResponse } from '../models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authStore = inject(AuthStore);

  private readonly apiUrl = 'http://localhost:8080/api';

  /**
   * Login user with email and password.
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    this.authStore.setLoading(true);

    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.authStore.setAuth(
            response.data.accessToken,
            response.data.refreshToken,
            response.data.user
          );
          this.router.navigate(['/dashboard']);
        }
      }),
      catchError(error => {
        return throwError(() => error);
      }),
      finalize(() => this.authStore.setLoading(false))
    );
  }

  /**
   * Register new user.
   */
  register(userData: RegisterRequest): Observable<AuthResponse> {
    this.authStore.setLoading(true);

    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, userData).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.authStore.setAuth(
            response.data.accessToken,
            response.data.refreshToken,
            response.data.user
          );
          this.router.navigate(['/auth/verify-email']);
        }
      }),
      catchError(error => {
        return throwError(() => error);
      }),
      finalize(() => this.authStore.setLoading(false))
    );
  }

  /**
   * Logout user.
   * Calls server to invalidate refresh token (HttpOnly cookie).
   */
  logout(): void {
    this.http.post(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      complete: () => this.clearAndRedirect(),
      error: () => this.clearAndRedirect()
    });
  }

  /**
   * Refresh access token using HttpOnly cookie.
   */
  refreshToken(): Observable<ApiResponse<{ accessToken: string; refreshToken: string }>> {
    return this.http.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
      `${this.apiUrl}/auth/refresh`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.authStore.updateTokens(response.data.accessToken, response.data.refreshToken);
        }
      })
    );
  }

  /**
   * Initialize auth state on app startup.
   * Attempts to get new access token using refresh cookie.
   */
  initializeAuth(): Observable<ApiResponse<{ accessToken: string; refreshToken: string }>> {
    this.authStore.setLoading(true);

    return this.refreshToken().pipe(
      tap(() => this.authStore.setInitialized(true)),
      catchError(error => {
        this.authStore.setInitialized(true);
        return throwError(() => error);
      }),
      finalize(() => this.authStore.setLoading(false))
    );
  }

  private clearAndRedirect(): void {
    this.authStore.clearAuth();
    this.router.navigate(['/auth/login']);
  }

  /**
   * Request password reset email.
   */
  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<{ message: string }>> {
    this.authStore.setLoading(true);

    return this.http.post<ApiResponse<{ message: string }>>(
      `${this.apiUrl}/auth/forgot-password`,
      request
    ).pipe(
      finalize(() => this.authStore.setLoading(false))
    );
  }

  /**
   * Reset password with token.
   */
  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<{ message: string }>> {
    this.authStore.setLoading(true);

    return this.http.post<ApiResponse<{ message: string }>>(
      `${this.apiUrl}/auth/reset-password`,
      request
    ).pipe(
      finalize(() => this.authStore.setLoading(false))
    );
  }

  /**
   * Verify email with token.
   */
  verifyEmail(token: string): Observable<ApiResponse<{ message: string }>> {
    this.authStore.setLoading(true);

    return this.http.get<ApiResponse<{ message: string }>>(
      `${this.apiUrl}/auth/verify-email`,
      { params: { token } }
    ).pipe(
      finalize(() => this.authStore.setLoading(false))
    );
  }
}
