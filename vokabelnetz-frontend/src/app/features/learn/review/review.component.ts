import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="max-w-3xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="px-4 py-6 sm:px-0">
        <h1 class="text-3xl font-bold text-gray-900">Review</h1>
        <div class="mt-8 card">
          <p class="text-center text-gray-500">Review session coming soon...</p>
        </div>
      </div>
    </div>
  `
})
export class ReviewComponent {}
