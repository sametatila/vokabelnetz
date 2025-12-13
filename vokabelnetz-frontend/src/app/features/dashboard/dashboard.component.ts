import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="px-4 py-6 sm:px-0">
        <h1 class="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p class="mt-2 text-gray-600">Welcome to Vokabelnetz - Your German vocabulary learning companion</p>

        <div class="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <a routerLink="/learn" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">Start Learning</h3>
            <p class="mt-2 text-sm text-gray-500">Practice vocabulary with spaced repetition</p>
          </a>

          <a routerLink="/progress" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">View Progress</h3>
            <p class="mt-2 text-sm text-gray-500">Track your learning statistics</p>
          </a>

          <a routerLink="/settings" class="card hover:shadow-lg transition-shadow cursor-pointer">
            <h3 class="text-lg font-medium text-gray-900">Settings</h3>
            <p class="mt-2 text-sm text-gray-500">Customize your learning experience</p>
          </a>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent {}
