import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-learn',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="px-4 py-6 sm:px-0">
        <h1 class="text-3xl font-bold text-gray-900">Learn</h1>
        <p class="mt-2 text-gray-600">Choose your learning mode</p>

        <div class="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2">
          <a routerLink="session" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">New Session</h3>
            <p class="mt-2 text-sm text-gray-500">Start a new learning session with fresh words</p>
          </a>

          <a routerLink="review" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">Review</h3>
            <p class="mt-2 text-sm text-gray-500">Review words that are due for practice</p>
          </a>
        </div>
      </div>
    </div>
  `
})
export class LearnComponent {}
