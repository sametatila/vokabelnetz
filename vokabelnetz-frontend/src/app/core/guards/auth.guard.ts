import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthStore } from '../state/auth.store';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs/operators';

/**
 * Auth guard - Protects routes that require authentication.
 * Waits for auth initialization before checking.
 */
export const authGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  // If already initialized, check immediately
  if (authStore.isInitialized()) {
    if (authStore.isAuthenticated()) {
      return true;
    }
    router.navigate(['/auth/login']);
    return false;
  }

  // Wait for initialization to complete
  return toObservable(authStore.isInitialized).pipe(
    filter(initialized => initialized),
    take(1),
    map(() => {
      if (authStore.isAuthenticated()) {
        return true;
      }
      router.navigate(['/auth/login']);
      return false;
    })
  );
};

/**
 * Guest guard - Protects routes that should only be accessible to non-authenticated users.
 * Waits for auth initialization before checking.
 */
export const guestGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  // If already initialized, check immediately
  if (authStore.isInitialized()) {
    if (!authStore.isAuthenticated()) {
      return true;
    }
    router.navigate(['/dashboard']);
    return false;
  }

  // Wait for initialization to complete
  return toObservable(authStore.isInitialized).pipe(
    filter(initialized => initialized),
    take(1),
    map(() => {
      if (!authStore.isAuthenticated()) {
        return true;
      }
      router.navigate(['/dashboard']);
      return false;
    })
  );
};
