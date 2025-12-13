import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div>
          <h2 class="mt-6 text-center text-3xl font-bold text-gray-900">
            Set new password
          </h2>
        </div>
        <div class="card">
          <p class="text-center text-gray-500">Reset password form coming soon...</p>
        </div>
      </div>
    </div>
  `
})
export class ResetPasswordComponent {}
