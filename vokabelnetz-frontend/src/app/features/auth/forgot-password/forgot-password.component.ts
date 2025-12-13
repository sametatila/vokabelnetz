import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStore } from '../../../core/services/auth.store';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div>
          <h2 class="mt-6 text-center text-3xl font-bold text-gray-900">
            Reset your password
          </h2>
          <p class="mt-2 text-center text-sm text-gray-600">
            Enter your email address and we'll send you a link to reset your password.
          </p>
        </div>

        <div class="card">
          @if (successMessage) {
            <div class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ successMessage }}</p>
            </div>
          }

          @if (errorMessage) {
            <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ errorMessage }}</p>
            </div>
          }

          <form [formGroup]="forgotPasswordForm" (ngSubmit)="onSubmit()" class="space-y-6">
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
                  @if (forgotPasswordForm.get('email')?.errors?.['required']) {
                    Email is required
                  } @else if (forgotPasswordForm.get('email')?.errors?.['email']) {
                    Please enter a valid email
                  }
                </p>
              }
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
                Sending...
              } @else {
                Send reset link
              }
            </button>
          </form>

          <div class="mt-4 text-center">
            <a routerLink="/auth/login" class="text-sm font-medium text-primary-600 hover:text-primary-500">
              Back to login
            </a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  protected readonly authStore = inject(AuthStore);

  successMessage = '';
  errorMessage = '';

  forgotPasswordForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  isFieldInvalid(fieldName: string): boolean {
    const field = this.forgotPasswordForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  onSubmit(): void {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.authService.forgotPassword(this.forgotPasswordForm.value).subscribe({
      next: (response) => {
        this.successMessage = response.data?.message || 'If an account with that email exists, a password reset link has been sent.';
        this.forgotPasswordForm.reset();
      },
      error: (error) => {
        this.errorMessage = error?.error?.error?.message || 'Failed to send reset link. Please try again.';
      }
    });
  }
}
