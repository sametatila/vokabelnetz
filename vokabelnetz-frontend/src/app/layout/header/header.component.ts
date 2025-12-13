import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthStore } from '../../core/services/auth.store';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="bg-white shadow-sm border-b border-gray-200">
      <nav class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <a routerLink="/dashboard" class="text-xl font-bold text-primary-600">
              Vokabelnetz
            </a>
          </div>

          @if (authStore.isAuthenticated()) {
            <div class="flex items-center space-x-4">
              <a routerLink="/learn" class="text-gray-600 hover:text-gray-900">Learn</a>
              <a routerLink="/progress" class="text-gray-600 hover:text-gray-900">Progress</a>
              <a routerLink="/settings" class="text-gray-600 hover:text-gray-900">Settings</a>
              <button
                (click)="logout()"
                class="btn btn-secondary text-sm"
              >
                Logout
              </button>
            </div>
          }
        </div>
      </nav>
    </header>
  `
})
export class HeaderComponent {
  protected authStore = inject(AuthStore);
  private authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
  }
}
