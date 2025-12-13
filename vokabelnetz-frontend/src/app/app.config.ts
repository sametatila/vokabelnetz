import { ApplicationConfig, provideZoneChangeDetection, APP_INITIALIZER, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { AuthService } from './core/services/auth.service';
import { AuthStore } from './core/state/auth.store';

/**
 * Initialize authentication on app startup.
 * Attempts to refresh access token using HttpOnly cookie.
 */
function initializeAuth(): () => Promise<void> {
  const authService = inject(AuthService);
  const authStore = inject(AuthStore);

  return () => new Promise<void>((resolve) => {
    authService.initializeAuth().pipe(
      catchError(() => {
        // No valid refresh token - user needs to login
        authStore.setInitialized(true);
        return of(null);
      })
    ).subscribe({
      complete: () => resolve(),
      error: () => resolve()
    });
  });
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([errorInterceptor, authInterceptor])),
    provideTranslateService({
      defaultLanguage: 'en'
    }),
    provideTranslateHttpLoader({
      prefix: './assets/i18n/',
      suffix: '.json'
    }),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      multi: true
    }
  ]
};
