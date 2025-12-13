import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthStore } from '../../../core/services/auth.store';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen bg-gray-50">
      <!-- Top Navigation -->
      <nav class="bg-white shadow-sm border-b border-gray-200">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex justify-between h-16">
            <div class="flex">
              <a routerLink="/dashboard" class="flex items-center">
                <span class="text-xl font-bold text-primary-600">Vokabelnetz</span>
              </a>
              <div class="hidden sm:ml-8 sm:flex sm:space-x-4">
                <a routerLink="/dashboard"
                   routerLinkActive="text-primary-600 border-b-2 border-primary-600"
                   class="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-700 hover:text-primary-600">
                  Dashboard
                </a>
                <a routerLink="/learn"
                   routerLinkActive="text-primary-600 border-b-2 border-primary-600"
                   class="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-700 hover:text-primary-600">
                  Learn
                </a>
                <a routerLink="/vocabulary"
                   routerLinkActive="text-primary-600 border-b-2 border-primary-600"
                   class="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-700 hover:text-primary-600">
                  Vocabulary
                </a>
                <a routerLink="/progress"
                   routerLinkActive="text-primary-600 border-b-2 border-primary-600"
                   class="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-700 hover:text-primary-600">
                  Progress
                </a>
              </div>
            </div>
            <div class="flex items-center space-x-4">
              <span class="text-sm text-gray-600">{{ authStore.user()?.email }}</span>
              <a routerLink="/settings" class="text-gray-600 hover:text-primary-600">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                </svg>
              </a>
              <button (click)="logout()" class="text-gray-600 hover:text-red-600">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </nav>

      <!-- Main Content -->
      <main class="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <router-outlet />
      </main>
    </div>
  `
})
export class MainLayoutComponent {
  protected readonly authStore = inject(AuthStore);
  private readonly authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
  }
}
