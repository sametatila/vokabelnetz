import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div>
          <h2 class="mt-6 text-center text-3xl font-bold text-gray-900">
            Verify your email
          </h2>
        </div>
        <div class="card">
          <p class="text-center text-gray-500">Email verification coming soon...</p>
        </div>
      </div>
    </div>
  `
})
export class VerifyEmailComponent {}
