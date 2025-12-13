import { Routes } from '@angular/router';

export const LEARN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./learn.component').then(m => m.LearnComponent)
  },
  {
    path: 'session',
    loadComponent: () => import('./session/session.component').then(m => m.SessionComponent)
  },
  {
    path: 'review',
    loadComponent: () => import('./review/review.component').then(m => m.ReviewComponent)
  }
];
