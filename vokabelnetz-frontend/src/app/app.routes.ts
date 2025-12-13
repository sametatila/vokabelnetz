import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
      }
    ]
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shared/layouts/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'learn',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) // Placeholder
      },
      {
        path: 'vocabulary',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) // Placeholder
      },
      {
        path: 'progress',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) // Placeholder
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) // Placeholder
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];
