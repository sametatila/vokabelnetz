import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStore } from '../../../core/state/auth.store';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div>
          <h2 class="mt-6 text-center text-3xl font-bold text-gray-900">
            Set new password
          </h2>
          <p class="mt-2 text-center text-sm text-gray-600">
            Enter your new password below.
          </p>
        </div>

        <div class="card">
          @if (successMessage) {
            <div class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-600">{{ successMessage }}</p>
              <a routerLink="/auth/login" class="text-sm font-medium text-primary-600 hover:text-primary-500 mt-2 block">
                Go to login
              </a>
            </div>
          }

          @if (errorMessage) {
            <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ errorMessage }}</p>
            </div>
          }

          @if (!successMessage) {
            <form [formGroup]="resetPasswordForm" (ngSubmit)="onSubmit()" class="space-y-6">
              <div>
                <label for="newPassword" class="block text-sm font-medium text-gray-700">
                  New Password
                </label>
                <input
                  id="newPassword"
                  type="password"
                  formControlName="newPassword"
                  class="input mt-1"
                  [class.border-red-500]="isFieldInvalid('newPassword')"
                  placeholder="••••••••••"
                />
                @if (isFieldInvalid('newPassword')) {
                  <p class="mt-1 text-sm text-red-600">
                    @if (resetPasswordForm.get('newPassword')?.errors?.['required']) {
                      Password is required
                    } @else if (resetPasswordForm.get('newPassword')?.errors?.['minlength']) {
                      Password must be at least 10 characters
                    } @else if (resetPasswordForm.get('newPassword')?.errors?.['pattern']) {
                      Password must contain uppercase, lowercase, number and special character
                    }
                  </p>
                }
              </div>

              <div>
                <label for="confirmPassword" class="block text-sm font-medium text-gray-700">
                  Confirm Password
                </label>
                <input
                  id="confirmPassword"
                  type="password"
                  formControlName="confirmPassword"
                  class="input mt-1"
                  [class.border-red-500]="isFieldInvalid('confirmPassword')"
                  placeholder="••••••••••"
                />
                @if (isFieldInvalid('confirmPassword')) {
                  <p class="mt-1 text-sm text-red-600">
                    @if (resetPasswordForm.get('confirmPassword')?.errors?.['required']) {
                      Please confirm your password
                    }
                  </p>
                }
                @if (resetPasswordForm.errors?.['passwordMismatch'] && resetPasswordForm.get('confirmPassword')?.touched) {
                  <p class="mt-1 text-sm text-red-600">Passwords do not match</p>
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
                  Resetting...
                } @else {
                  Reset password
                }
              </button>
            </form>
          }

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
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly authStore = inject(AuthStore);

  private token = '';
  successMessage = '';
  errorMessage = '';

  resetPasswordForm: FormGroup = this.fb.group({
    newPassword: ['', [
      Validators.required,
      Validators.minLength(10),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)
    ]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: this.passwordMatchValidator });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.errorMessage = 'Invalid or missing reset token. Please request a new password reset link.';
    }
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.resetPasswordForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  onSubmit(): void {
    if (this.resetPasswordForm.invalid || !this.token) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.authService.resetPassword({
      token: this.token,
      newPassword: this.resetPasswordForm.get('newPassword')?.value
    }).subscribe({
      next: (response) => {
        this.successMessage = response.data?.message || 'Password has been reset successfully.';
      },
      error: (error) => {
        this.errorMessage = error?.error?.error?.message || 'Failed to reset password. The link may have expired.';
      }
    });
  }
}
