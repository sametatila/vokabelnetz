import { Routes } from '@angular/router';

export const PROGRESS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./progress.component').then(m => m.ProgressComponent)
  },
  {
    path: 'statistics',
    loadComponent: () => import('./statistics/statistics.component').then(m => m.StatisticsComponent)
  },
  {
    path: 'vocabulary',
    loadComponent: () => import('./vocabulary/vocabulary.component').then(m => m.VocabularyComponent)
  }
];
