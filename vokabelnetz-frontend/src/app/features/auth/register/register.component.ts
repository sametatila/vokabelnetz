import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
      <div class="max-w-md w-full">
        <h2 class="text-center text-3xl font-bold text-gray-900">Create account</h2>
        <p class="mt-2 text-center text-sm text-gray-600">
          Already have an account?
          <a routerLink="/auth/login" class="text-primary-600 hover:text-primary-500">Sign in</a>
        </p>
        <div class="card mt-8">
          <p class="text-gray-500 text-center">Registration form coming soon...</p>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {}
