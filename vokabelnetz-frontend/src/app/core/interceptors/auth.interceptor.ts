import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthStore } from '../services/auth.store';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;

/**
 * Auth interceptor that adds JWT token to requests and handles token refresh.
 */
export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authStore = inject(AuthStore);
  const authService = inject(AuthService);

  // Skip auth header for auth endpoints (except logout-all and sessions)
  const isAuthEndpoint = req.url.includes('/auth/') &&
    !req.url.includes('/auth/logout-all') &&
    !req.url.includes('/auth/sessions') &&
    !req.url.includes('/auth/resend-verification');

  if (isAuthEndpoint) {
    return next(req);
  }

  // Add auth header if token exists
  const token = authStore.accessToken();
  if (token) {
    req = addAuthHeader(req, token);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized - try to refresh token
      if (error.status === 401 && !isRefreshing && authStore.refreshToken()) {
        isRefreshing = true;

        return authService.refreshTokens().pipe(
          switchMap(response => {
            isRefreshing = false;
            if (response.success && response.data) {
              // Retry the original request with new token
              const newReq = addAuthHeader(req, response.data.accessToken);
              return next(newReq);
            }
            return throwError(() => error);
          }),
          catchError(refreshError => {
            isRefreshing = false;
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }

      return throwError(() => error);
    })
  );
};

/**
 * Add Authorization header to request.
 */
function addAuthHeader(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}
