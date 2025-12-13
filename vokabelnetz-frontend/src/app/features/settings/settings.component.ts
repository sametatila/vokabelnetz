import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="px-4 py-6 sm:px-0">
        <h1 class="text-3xl font-bold text-gray-900">Settings</h1>
        <p class="mt-2 text-gray-600">Manage your account and preferences</p>

        <div class="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2">
          <a routerLink="profile" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">Profile</h3>
            <p class="mt-2 text-sm text-gray-500">Update your personal information</p>
          </a>

          <a routerLink="security" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">Security</h3>
            <p class="mt-2 text-sm text-gray-500">Change password and security settings</p>
          </a>
        </div>
      </div>
    </div>
  `
})
export class SettingsComponent {}
