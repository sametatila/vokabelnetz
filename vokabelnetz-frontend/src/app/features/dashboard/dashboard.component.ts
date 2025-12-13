import { Component, inject } from '@angular/core';
import { AuthStore } from '../../core/services/auth.store';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold text-gray-900">
        Welcome, {{ authStore.user()?.displayName || 'User' }}!
      </h1>
      <p class="mt-2 text-gray-600">Start learning German vocabulary today.</p>

      <div class="mt-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div class="card">
          <h3 class="font-semibold text-gray-900">Quick Start</h3>
          <p class="mt-2 text-sm text-gray-600">Begin your daily vocabulary practice</p>
        </div>
        <div class="card">
          <h3 class="font-semibold text-gray-900">Progress</h3>
          <p class="mt-2 text-sm text-gray-600">Track your learning journey</p>
        </div>
        <div class="card">
          <h3 class="font-semibold text-gray-900">Vocabulary</h3>
          <p class="mt-2 text-sm text-gray-600">Browse and manage your word lists</p>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent {
  protected readonly authStore = inject(AuthStore);
}
