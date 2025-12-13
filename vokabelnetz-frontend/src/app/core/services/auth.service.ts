import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, finalize } from 'rxjs';
import { AuthStore } from './auth.store';
import { LoginRequest, RegisterRequest, AuthResponse, ApiResponse } from '../models';

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
   */
  logout(): void {
    const refreshToken = this.authStore.refreshToken();

    if (refreshToken) {
      this.http.post(`${this.apiUrl}/auth/logout`, { refreshToken }).subscribe({
        complete: () => this.clearAndRedirect()
      });
    } else {
      this.clearAndRedirect();
    }
  }

  /**
   * Refresh access token.
   */
  refreshToken(): Observable<ApiResponse<{ accessToken: string; refreshToken: string }>> {
    const refreshToken = this.authStore.refreshToken();

    return this.http.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
      `${this.apiUrl}/auth/refresh`,
      { refreshToken }
    ).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.authStore.updateTokens(response.data.accessToken, response.data.refreshToken);
        }
      })
    );
  }

  private clearAndRedirect(): void {
    this.authStore.clearAuth();
    this.router.navigate(['/auth/login']);
  }
}
