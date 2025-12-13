import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div>
          <h2 class="mt-6 text-center text-3xl font-bold text-gray-900">
            Create your account
          </h2>
          <p class="mt-2 text-center text-sm text-gray-600">
            Already have an account?
            <a routerLink="/auth/login" class="font-medium text-primary-600 hover:text-primary-500">
              Sign in
            </a>
          </p>
        </div>
        <div class="card">
          <p class="text-center text-gray-500">Register form coming soon...</p>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {}
