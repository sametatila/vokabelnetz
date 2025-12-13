import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../../core/services/dashboard.service';
import { UserProfile, DailyStats, StreakStatus } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="p-6 max-w-7xl mx-auto">
      <!-- Welcome Header -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900">
          Welcome back, {{ userProfile()?.displayName || 'Learner' }}!
        </h1>
        <p class="mt-1 text-gray-600">Ready to learn some German vocabulary today?</p>
      </div>

      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <!-- Streak Card -->
        <div class="card bg-gradient-to-br from-orange-50 to-orange-100 border-orange-200">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-orange-600">Current Streak</p>
              <p class="text-3xl font-bold text-orange-700">{{ streakStatus()?.currentStreak || 0 }}</p>
              <p class="text-xs text-orange-500 mt-1">
                Best: {{ streakStatus()?.longestStreak || 0 }} days
              </p>
            </div>
            <div class="text-4xl">🔥</div>
          </div>
          @if (streakStatus()?.atRisk) {
            <div class="mt-3 text-xs text-orange-600 bg-orange-200 rounded px-2 py-1">
              ⚠️ Practice today to keep your streak!
            </div>
          }
        </div>

        <!-- Words Learned Card -->
        <div class="card bg-gradient-to-br from-green-50 to-green-100 border-green-200">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-green-600">Words Learned</p>
              <p class="text-3xl font-bold text-green-700">{{ userProfile()?.totalWordsLearned || 0 }}</p>
              <p class="text-xs text-green-500 mt-1">
                +{{ dailyStats()?.newWordsLearned || 0 }} today
              </p>
            </div>
            <div class="text-4xl">📚</div>
          </div>
        </div>

        <!-- Elo Rating Card -->
        <div class="card bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-blue-600">Skill Rating</p>
              <p class="text-3xl font-bold text-blue-700">{{ userProfile()?.eloRating || 1000 }}</p>
              <p class="text-xs text-blue-500 mt-1">Elo Rating</p>
            </div>
            <div class="text-4xl">⭐</div>
          </div>
        </div>

        <!-- Today's Progress Card -->
        <div class="card bg-gradient-to-br from-purple-50 to-purple-100 border-purple-200">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-purple-600">Today's Progress</p>
              <p class="text-3xl font-bold text-purple-700">
                {{ dailyStats()?.wordsReviewed || 0 }}/{{ userProfile()?.dailyGoal || 20 }}
              </p>
              <p class="text-xs text-purple-500 mt-1">
                {{ getAccuracy() }}% accuracy
              </p>
            </div>
            <div class="text-4xl">🎯</div>
          </div>
          <!-- Progress Bar -->
          <div class="mt-3 h-2 bg-purple-200 rounded-full overflow-hidden">
            <div
              class="h-full bg-purple-600 rounded-full transition-all duration-300"
              [style.width.%]="getProgressPercentage()"
            ></div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <a routerLink="/learn" class="card hover:shadow-lg transition-shadow cursor-pointer group">
          <div class="flex items-center space-x-4">
            <div class="p-3 bg-primary-100 rounded-lg group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">Start Learning</h3>
              <p class="text-sm text-gray-600">Begin your daily practice session</p>
            </div>
          </div>
        </a>

        <a routerLink="/vocabulary" class="card hover:shadow-lg transition-shadow cursor-pointer group">
          <div class="flex items-center space-x-4">
            <div class="p-3 bg-green-100 rounded-lg group-hover:bg-green-200 transition-colors">
              <svg class="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">Vocabulary</h3>
              <p class="text-sm text-gray-600">Browse and manage your word lists</p>
            </div>
          </div>
        </a>

        <a routerLink="/progress" class="card hover:shadow-lg transition-shadow cursor-pointer group">
          <div class="flex items-center space-x-4">
            <div class="p-3 bg-blue-100 rounded-lg group-hover:bg-blue-200 transition-colors">
              <svg class="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">Progress</h3>
              <p class="text-sm text-gray-600">View detailed statistics</p>
            </div>
          </div>
        </a>
      </div>

      <!-- Weekly Overview -->
      <div class="card">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">This Week</h2>
        <div class="grid grid-cols-7 gap-2">
          @for (day of weekDays; track day.name) {
            <div class="text-center">
              <p class="text-xs text-gray-500 mb-2">{{ day.name }}</p>
              <div
                class="w-10 h-10 mx-auto rounded-lg flex items-center justify-center text-sm font-medium"
                [class]="getDayClass(day)"
              >
                {{ day.count }}
              </div>
            </div>
          }
        </div>
      </div>

      <!-- Loading State -->
      @if (isLoading()) {
        <div class="fixed inset-0 bg-white/50 flex items-center justify-center z-50">
          <div class="text-center">
            <svg class="animate-spin h-8 w-8 text-primary-600 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p class="mt-2 text-gray-600">Loading dashboard...</p>
          </div>
        </div>
      }
    </div>
  `
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  userProfile = signal<UserProfile | null>(null);
  dailyStats = signal<DailyStats | null>(null);
  streakStatus = signal<StreakStatus | null>(null);
  weeklyStats = signal<DailyStats[]>([]);
  isLoading = signal(true);

  weekDays: { name: string; count: number; completed: boolean }[] = [];

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.isLoading.set(true);

    // Load user profile
    this.dashboardService.getUserProfile().subscribe({
      next: (response) => {
        if (response.success) {
          this.userProfile.set(response.data);
        }
      },
      error: () => this.isLoading.set(false)
    });

    // Load daily stats
    this.dashboardService.getDailyStats().subscribe({
      next: (response) => {
        if (response.success) {
          this.dailyStats.set(response.data);
        }
      }
    });

    // Load streak status
    this.dashboardService.getStreakStatus().subscribe({
      next: (response) => {
        if (response.success) {
          this.streakStatus.set(response.data);
        }
      }
    });

    // Load weekly stats
    this.dashboardService.getWeeklyStats().subscribe({
      next: (response) => {
        if (response.success) {
          this.weeklyStats.set(response.data);
          this.buildWeekDays(response.data);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.buildWeekDays([]);
        this.isLoading.set(false);
      }
    });
  }

  private buildWeekDays(stats: DailyStats[]): void {
    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const today = new Date();

    this.weekDays = [];

    for (let i = 6; i >= 0; i--) {
      const date = new Date(today);
      date.setDate(today.getDate() - i);
      const dateStr = date.toISOString().split('T')[0];

      const dayStat = stats.find(s => s.statDate === dateStr);

      this.weekDays.push({
        name: dayNames[date.getDay()],
        count: dayStat?.wordsReviewed || 0,
        completed: dayStat?.streakMaintained || false
      });
    }
  }

  getAccuracy(): number {
    const stats = this.dailyStats();
    if (!stats || stats.wordsReviewed === 0) return 0;
    return Math.round((stats.wordsCorrect / stats.wordsReviewed) * 100);
  }

  getProgressPercentage(): number {
    const reviewed = this.dailyStats()?.wordsReviewed || 0;
    const goal = this.userProfile()?.dailyGoal || 20;
    return Math.min(100, Math.round((reviewed / goal) * 100));
  }

  getDayClass(day: { count: number; completed: boolean }): string {
    if (day.completed) {
      return 'bg-green-100 text-green-700';
    } else if (day.count > 0) {
      return 'bg-yellow-100 text-yellow-700';
    }
    return 'bg-gray-100 text-gray-400';
  }
}
