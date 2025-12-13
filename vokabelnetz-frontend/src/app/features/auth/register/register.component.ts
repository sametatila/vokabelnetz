import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStore } from '../../../core/services/auth.store';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
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
          @if (errorMessage) {
            <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ errorMessage }}</p>
            </div>
          }

          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="space-y-6">
            <div>
              <label for="displayName" class="block text-sm font-medium text-gray-700">
                Display Name
              </label>
              <input
                id="displayName"
                type="text"
                formControlName="displayName"
                class="input mt-1"
                [class.border-red-500]="isFieldInvalid('displayName')"
                placeholder="Your name"
              />
              @if (isFieldInvalid('displayName')) {
                <p class="mt-1 text-sm text-red-600">
                  @if (registerForm.get('displayName')?.errors?.['required']) {
                    Display name is required
                  } @else if (registerForm.get('displayName')?.errors?.['minlength']) {
                    Display name must be at least 2 characters
                  }
                </p>
              }
            </div>

            <div>
              <label for="email" class="block text-sm font-medium text-gray-700">
                Email address
              </label>
              <input
                id="email"
                type="email"
                formControlName="email"
                class="input mt-1"
                [class.border-red-500]="isFieldInvalid('email')"
                placeholder="your@email.com"
              />
              @if (isFieldInvalid('email')) {
                <p class="mt-1 text-sm text-red-600">
                  @if (registerForm.get('email')?.errors?.['required']) {
                    Email is required
                  } @else if (registerForm.get('email')?.errors?.['email']) {
                    Please enter a valid email
                  }
                </p>
              }
            </div>

            <div>
              <label for="password" class="block text-sm font-medium text-gray-700">
                Password
              </label>
              <input
                id="password"
                type="password"
                formControlName="password"
                class="input mt-1"
                [class.border-red-500]="isFieldInvalid('password')"
                placeholder="••••••••"
              />
              @if (isFieldInvalid('password')) {
                <p class="mt-1 text-sm text-red-600">
                  @if (registerForm.get('password')?.errors?.['required']) {
                    Password is required
                  } @else if (registerForm.get('password')?.errors?.['minlength']) {
                    Password must be at least 8 characters
                  } @else if (registerForm.get('password')?.errors?.['pattern']) {
                    Password must contain uppercase, lowercase, number and special character
                  }
                </p>
              }
            </div>

            <div>
              <label for="nativeLanguage" class="block text-sm font-medium text-gray-700">
                Native Language
              </label>
              <select
                id="nativeLanguage"
                formControlName="nativeLanguage"
                class="input mt-1"
              >
                <option value="TURKISH">Turkish (Türkçe)</option>
                <option value="ENGLISH">English</option>
              </select>
            </div>

            <button
              type="submit"
              [disabled]="authStore.isLoading()"
              class="w-full btn btn-primary flex justify-center items-center"
            >
              @if (authStore.isLoading()) {
                <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Creating account...
              } @else {
                Create account
              }
            </button>
          </form>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  protected readonly authStore = inject(AuthStore);

  errorMessage = '';

  registerForm: FormGroup = this.fb.group({
    displayName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)
    ]],
    nativeLanguage: ['TURKISH']
  });

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.errorMessage = '';

    this.authService.register(this.registerForm.value).subscribe({
      error: (error) => {
        this.errorMessage = error?.error?.error?.message || 'Registration failed. Please try again.';
      }
    });
  }
}
