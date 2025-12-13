import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/**
 * Error interceptor for global error handling and logging.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = error.error.message;
        console.error('Client error:', errorMessage);
      } else {
        // Server-side error
        if (error.error?.error?.message) {
          errorMessage = error.error.error.message;
        } else if (error.error?.message) {
          errorMessage = error.error.message;
        } else {
          errorMessage = getDefaultErrorMessage(error.status);
        }
        console.error(`Server error [${error.status}]:`, errorMessage);
      }

      return throwError(() => ({
        status: error.status,
        message: errorMessage,
        error: error.error
      }));
    })
  );
};

/**
 * Get default error message based on HTTP status code.
 */
function getDefaultErrorMessage(status: number): string {
  switch (status) {
    case 400:
      return 'Invalid request. Please check your input.';
    case 401:
      return 'Authentication required. Please log in.';
    case 403:
      return 'You do not have permission to perform this action.';
    case 404:
      return 'The requested resource was not found.';
    case 409:
      return 'This resource already exists.';
    case 429:
      return 'Too many requests. Please try again later.';
    case 500:
      return 'Server error. Please try again later.';
    case 503:
      return 'Service temporarily unavailable. Please try again later.';
    default:
      return 'An unexpected error occurred. Please try again.';
  }
}
