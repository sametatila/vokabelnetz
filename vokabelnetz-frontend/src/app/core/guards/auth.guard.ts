import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthStore } from '../services/auth.store';

/**
 * Guard that prevents unauthenticated users from accessing protected routes.
 */
export const authGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  if (authStore.isAuthenticated()) {
    return true;
  }

  // Redirect to login page
  router.navigate(['/auth/login'], {
    queryParams: { returnUrl: router.url }
  });
  return false;
};

/**
 * Guard that prevents authenticated users from accessing auth pages (login, register).
 */
export const noAuthGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  if (!authStore.isAuthenticated()) {
    return true;
  }

  // Redirect to dashboard
  router.navigate(['/dashboard']);
  return false;
};
