import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/**
 * Error interceptor - Handles global HTTP errors.
 *
 * Provides consistent error handling for:
 * - Network errors (offline, timeout)
 * - Server errors (500, 502, 503, 504)
 * - Client errors (400, 403, 404, etc.)
 *
 * Note: 401 errors are handled by authInterceptor for token refresh.
 */
export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error occurred';

      if (error.status === 0) {
        // Network error (no connection, CORS, timeout)
        errorMessage = 'Unable to connect to server. Please check your internet connection.';
        console.error('Network error:', error.message);
      } else if (error.status >= 500) {
        // Server errors
        errorMessage = 'Server error. Please try again later.';
        console.error(`Server error [${error.status}]:`, error.message);
      } else if (error.status === 400) {
        // Bad request
        errorMessage = error.error?.error?.message || 'Invalid request. Please check your input.';
      } else if (error.status === 403) {
        // Forbidden
        errorMessage = 'You do not have permission to perform this action.';
      } else if (error.status === 404) {
        // Not found
        errorMessage = 'The requested resource was not found.';
      } else if (error.status === 409) {
        // Conflict (e.g., duplicate email)
        errorMessage = error.error?.error?.message || 'A conflict occurred. The resource may already exist.';
      } else if (error.status === 422) {
        // Validation error
        errorMessage = error.error?.error?.message || 'Validation failed. Please check your input.';
      } else if (error.status === 429) {
        // Rate limit
        errorMessage = 'Too many requests. Please wait a moment and try again.';
      }

      // Return the error for component-level handling
      return throwError(() => ({
        ...error,
        userMessage: errorMessage
      }));
    })
  );
};
