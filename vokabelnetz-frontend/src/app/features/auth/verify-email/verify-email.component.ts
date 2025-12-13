import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStore } from '../../../core/services/auth.store';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div class="card text-center">
          @if (authStore.isLoading()) {
            <div class="py-8">
              <svg class="animate-spin mx-auto h-12 w-12 text-primary-600" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <p class="mt-4 text-gray-600">Verifying your email...</p>
            </div>
          }

          @if (successMessage) {
            <div class="py-8">
              <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100">
                <svg class="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 class="mt-4 text-xl font-bold text-gray-900">Email Verified!</h2>
              <p class="mt-2 text-gray-600">{{ successMessage }}</p>
              <a routerLink="/dashboard" class="mt-6 inline-block btn btn-primary">
                Go to Dashboard
              </a>
            </div>
          }

          @if (errorMessage) {
            <div class="py-8">
              <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100">
                <svg class="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>
              <h2 class="mt-4 text-xl font-bold text-gray-900">Verification Failed</h2>
              <p class="mt-2 text-gray-600">{{ errorMessage }}</p>
              <a routerLink="/auth/login" class="mt-6 inline-block btn btn-primary">
                Go to Login
              </a>
            </div>
          }

          @if (pendingVerification) {
            <div class="py-8">
              <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-yellow-100">
                <svg class="h-6 w-6 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <h2 class="mt-4 text-xl font-bold text-gray-900">Check Your Email</h2>
              <p class="mt-2 text-gray-600">
                We've sent a verification link to your email address. Please check your inbox and click the link to verify your account.
              </p>
              <p class="mt-4 text-sm text-gray-500">
                Didn't receive the email? Check your spam folder or
                <a routerLink="/auth/login" class="text-primary-600 hover:text-primary-500">try logging in</a>
                to resend the verification email.
              </p>
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class VerifyEmailComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  protected readonly authStore = inject(AuthStore);

  successMessage = '';
  errorMessage = '';
  pendingVerification = false;

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'];

    if (token) {
      this.verifyEmail(token);
    } else {
      this.pendingVerification = true;
    }
  }

  private verifyEmail(token: string): void {
    this.authService.verifyEmail(token).subscribe({
      next: (response) => {
        this.successMessage = response.data?.message || 'Your email has been verified successfully.';
      },
      error: (error) => {
        this.errorMessage = error?.error?.error?.message || 'Failed to verify email. The link may have expired.';
      }
    });
  }
}
