import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="px-4 py-6 sm:px-0">
        <h1 class="text-3xl font-bold text-gray-900">Progress</h1>
        <p class="mt-2 text-gray-600">Track your learning journey</p>

        <div class="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2">
          <a routerLink="statistics" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">Statistics</h3>
            <p class="mt-2 text-sm text-gray-500">View detailed learning statistics and charts</p>
          </a>

          <a routerLink="vocabulary" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">Vocabulary List</h3>
            <p class="mt-2 text-sm text-gray-500">Browse all words in your vocabulary</p>
          </a>
        </div>
      </div>
    </div>
  `
})
export class ProgressComponent {}
