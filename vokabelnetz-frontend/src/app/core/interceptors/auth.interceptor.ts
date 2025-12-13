import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthStore } from '../state/auth.store';
import { AuthService } from '../services/auth.service';

/**
 * Auth interceptor - Handles JWT authentication and token refresh.
 *
 * Based on docs/SECURITY.md Authentication Flow.
 */
export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authStore = inject(AuthStore);
  const authService = inject(AuthService);

  const accessToken = authStore.accessToken();

  // Public auth endpoints that don't need token
  const isPublicAuthEndpoint =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register') ||
    req.url.includes('/auth/forgot-password') ||
    req.url.includes('/auth/reset-password') ||
    req.url.includes('/auth/verify-email');

  // Auth endpoints that need cookies (login, register, refresh, logout)
  const isAuthCookieEndpoint =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register') ||
    req.url.includes('/auth/refresh') ||
    req.url.includes('/auth/logout');

  // Clone request with withCredentials for cookie-based auth endpoints
  // This ensures browser accepts Set-Cookie from login/register responses
  if (isAuthCookieEndpoint) {
    req = req.clone({ withCredentials: true });
  }

  // Skip adding auth header for public endpoints
  if (isPublicAuthEndpoint) {
    return next(req);
  }

  // Add auth header if token exists
  if (accessToken) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Try to refresh token on 401 error (not for auth endpoints)
      if (error.status === 401 && authStore.isAuthenticated() && !isAuthCookieEndpoint && !isPublicAuthEndpoint) {
        return authService.refreshToken().pipe(
          switchMap(() => {
            const newToken = authStore.accessToken();
            const clonedReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`
              }
            });
            return next(clonedReq);
          }),
          catchError(refreshError => {
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
