import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import {
  ProgressService,
  OverallProgress,
  AchievementsResponse,
  StreakInfo,
  ActivityData
} from '../../core/services/progress.service';
import { CefrLevel } from '../../core/models';

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-6xl mx-auto">

        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Your Progress</h1>
          <p class="mt-2 text-gray-600">Track your learning journey</p>
        </div>

        @if (loading()) {
          <div class="text-center py-12">
            <svg class="animate-spin h-8 w-8 text-primary-600 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p class="mt-2 text-gray-600">Loading progress...</p>
          </div>
        } @else if (progress()) {

          <!-- Overview Stats -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <div class="card text-center">
              <p class="text-3xl font-bold text-primary-600">{{ progress()!.overview.totalWordsLearned }}</p>
              <p class="text-sm text-gray-600">Words Learned</p>
            </div>
            <div class="card text-center">
              <p class="text-3xl font-bold text-green-600">{{ progress()!.overview.overallAccuracy | number:'1.0-0' }}%</p>
              <p class="text-sm text-gray-600">Accuracy</p>
            </div>
            <div class="card text-center">
              <p class="text-3xl font-bold text-blue-600">{{ progress()!.overview.totalReviews }}</p>
              <p class="text-sm text-gray-600">Total Reviews</p>
            </div>
            <div class="card text-center">
              <p class="text-3xl font-bold text-orange-600">{{ formatTime(progress()!.overview.totalTimeSpentMinutes) }}</p>
              <p class="text-sm text-gray-600">Time Spent</p>
            </div>
          </div>

          <!-- Elo & Streak Row -->
          <div class="grid md:grid-cols-2 gap-6 mb-8">
            <!-- Elo Rating -->
            <div class="card">
              <h2 class="text-lg font-semibold text-gray-900 mb-4">Elo Rating</h2>
              <div class="flex items-center justify-between mb-4">
                <div>
                  <p class="text-4xl font-bold text-primary-600">{{ progress()!.elo.currentRating }}</p>
                  <p class="text-sm text-gray-500">Current Rating</p>
                </div>
                <div class="text-right">
                  <p class="text-lg font-semibold text-gray-700">{{ progress()!.elo.highestRating }}</p>
                  <p class="text-sm text-gray-500">Highest</p>
                </div>
              </div>
              <div class="flex gap-4">
                <div class="flex-1 p-3 bg-gray-50 rounded-lg">
                  <p class="text-sm text-gray-500">Last 7 Days</p>
                  <p class="font-semibold" [class]="progress()!.elo.change7Days >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ progress()!.elo.change7Days >= 0 ? '+' : '' }}{{ progress()!.elo.change7Days }}
                  </p>
                </div>
                <div class="flex-1 p-3 bg-gray-50 rounded-lg">
                  <p class="text-sm text-gray-500">Last 30 Days</p>
                  <p class="font-semibold" [class]="progress()!.elo.change30Days >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ progress()!.elo.change30Days >= 0 ? '+' : '' }}{{ progress()!.elo.change30Days }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Streak -->
            <div class="card">
              <h2 class="text-lg font-semibold text-gray-900 mb-4">Streak</h2>
              <div class="flex items-center justify-between mb-4">
                <div class="flex items-center gap-3">
                  <div class="p-3 bg-orange-100 rounded-full">
                    <svg class="w-8 h-8 text-orange-600" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M12 23c6.075 0 11-4.925 11-11 0-4.418-2.687-8.235-6.522-9.872.128.497.197 1.02.197 1.558 0 2.97-1.97 5.858-4.175 8.374-2.205-2.516-4.175-5.404-4.175-8.374 0-.538.069-1.061.197-1.558C4.687 3.765 2 7.582 2 12c0 6.075 4.925 11 10 11z"/>
                    </svg>
                  </div>
                  <div>
                    <p class="text-4xl font-bold text-orange-600">{{ progress()!.streak.currentStreak }}</p>
                    <p class="text-sm text-gray-500">Day Streak</p>
                  </div>
                </div>
                <div class="text-right">
                  <p class="text-lg font-semibold text-gray-700">{{ progress()!.streak.longestStreak }}</p>
                  <p class="text-sm text-gray-500">Longest</p>
                </div>
              </div>

              <!-- Streak Freezes Available -->
              @if (streak()?.freezesAvailable !== undefined) {
                <div class="flex items-center gap-2 mb-4 p-3 bg-blue-50 rounded-lg">
                  <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8zm0 18c-3.35 0-6-2.57-6-6.2 0-2.34 1.95-5.44 6-9.14 4.05 3.7 6 6.79 6 9.14 0 3.63-2.65 6.2-6 6.2z"/>
                  </svg>
                  <span class="text-sm text-blue-700">
                    {{ streak()!.freezesAvailable }} streak freeze{{ streak()!.freezesAvailable !== 1 ? 's' : '' }} available
                  </span>
                </div>
              }

              <!-- Streak Status Messages -->
              @if (streak()?.atRisk && !streak()?.todayCompleted) {
                <div class="p-3 bg-red-50 border border-red-200 rounded-lg">
                  <p class="text-sm text-red-700">
                    <span class="font-medium">Streak at risk!</span> Practice today to keep your {{ progress()!.streak.currentStreak }}-day streak.
                  </p>
                </div>
              } @else if (streak()?.todayCompleted) {
                <div class="p-3 bg-green-50 border border-green-200 rounded-lg">
                  <p class="text-sm text-green-700">
                    <span class="font-medium">Great job!</span> Today's goal completed. Keep it up!
                  </p>
                </div>
              } @else if (progress()!.streak.currentStreak === 0) {
                <div class="p-3 bg-gray-50 border border-gray-200 rounded-lg">
                  <p class="text-sm text-gray-700">
                    Start learning today to begin a new streak!
                  </p>
                </div>
              }

              <!-- Next Milestone -->
              @if (streak()?.milestones) {
                <div class="mt-4 pt-4 border-t border-gray-100">
                  <div class="flex justify-between text-sm">
                    <span class="text-gray-500">Next milestone</span>
                    <span class="font-medium text-gray-700">{{ streak()!.milestones.nextMilestone }} days ({{ streak()!.milestones.daysUntilNext }} to go)</span>
                  </div>
                </div>
              }
            </div>
          </div>

          <!-- CEFR Level Progress -->
          <div class="card mb-8">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">Level Progress</h2>
            <div class="space-y-4">
              @for (level of cefrLevels; track level) {
                @if (progress()!.levelProgress[level]) {
                  <div>
                    <div class="flex justify-between text-sm mb-1">
                      <span class="font-medium" [class]="getLevelTextClass(level)">{{ level }}</span>
                      <span class="text-gray-500">
                        {{ progress()!.levelProgress[level].learned }} / {{ progress()!.levelProgress[level].total }}
                        ({{ progress()!.levelProgress[level].percentage | number:'1.0-0' }}%)
                      </span>
                    </div>
                    <div class="h-3 bg-gray-200 rounded-full overflow-hidden">
                      <div
                        class="h-full rounded-full transition-all duration-300"
                        [class]="getLevelBgClass(level)"
                        [style.width.%]="progress()!.levelProgress[level].percentage"
                      ></div>
                    </div>
                  </div>
                }
              }
            </div>
          </div>

          <!-- Achievements -->
          @if (achievements()) {
            <div class="card mb-8">
              <div class="flex justify-between items-center mb-4">
                <h2 class="text-lg font-semibold text-gray-900">Achievements</h2>
                <span class="text-sm text-gray-500">
                  {{ achievements()!.totalEarned }} / {{ achievements()!.totalAvailable + achievements()!.totalEarned }}
                </span>
              </div>

              <!-- Earned Achievements -->
              @if (achievements()!.earned.length > 0) {
                <div class="mb-6">
                  <h3 class="text-sm font-medium text-gray-700 mb-3">Earned</h3>
                  <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
                    @for (achievement of achievements()!.earned; track achievement.type) {
                      <div class="p-4 bg-green-50 border border-green-200 rounded-lg text-center">
                        <span class="text-3xl">{{ achievement.icon }}</span>
                        <p class="font-medium text-gray-900 mt-2">{{ achievement.name }}</p>
                        <p class="text-xs text-gray-500 mt-1">{{ achievement.description }}</p>
                      </div>
                    }
                  </div>
                </div>
              }

              <!-- Available Achievements -->
              @if (achievements()!.available.length > 0) {
                <div>
                  <h3 class="text-sm font-medium text-gray-700 mb-3">In Progress</h3>
                  <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
                    @for (achievement of achievements()!.available.slice(0, 4); track achievement.type) {
                      <div class="p-4 bg-gray-50 border border-gray-200 rounded-lg text-center">
                        <span class="text-3xl opacity-50">{{ achievement.icon }}</span>
                        <p class="font-medium text-gray-700 mt-2">{{ achievement.name }}</p>
                        @if (achievement.progress) {
                          <div class="mt-2">
                            <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                              <div
                                class="h-full bg-primary-500 rounded-full"
                                [style.width.%]="achievement.progress.percentage"
                              ></div>
                            </div>
                            <p class="text-xs text-gray-500 mt-1">
                              {{ achievement.progress.current }} / {{ achievement.progress.target }}
                            </p>
                          </div>
                        }
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
          }

          <!-- Activity Heatmap -->
          @if (activity()) {
            <div class="card">
              <h2 class="text-lg font-semibold text-gray-900 mb-4">Activity</h2>
              <div class="flex items-center gap-4 mb-4">
                <span class="text-sm text-gray-500">{{ activity()!.totalActiveDays }} active days in {{ activity()!.year }}</span>
              </div>

              <!-- Simple Activity Grid (last 12 weeks) -->
              <div class="overflow-x-auto">
                <div class="flex gap-1 min-w-max">
                  @for (week of getWeeks(); track $index) {
                    <div class="flex flex-col gap-1">
                      @for (day of week; track day.date) {
                        <div
                          class="w-3 h-3 rounded-sm"
                          [class]="getActivityClass(day.level)"
                          [title]="day.date + ': ' + day.count + ' words'"
                        ></div>
                      }
                    </div>
                  }
                </div>
              </div>

              <!-- Legend -->
              <div class="flex items-center justify-end gap-2 mt-4 text-xs text-gray-500">
                <span>Less</span>
                <div class="w-3 h-3 rounded-sm bg-gray-100"></div>
                <div class="w-3 h-3 rounded-sm bg-green-200"></div>
                <div class="w-3 h-3 rounded-sm bg-green-400"></div>
                <div class="w-3 h-3 rounded-sm bg-green-600"></div>
                <div class="w-3 h-3 rounded-sm bg-green-800"></div>
                <span>More</span>
              </div>
            </div>
          }

          <!-- Today's Stats -->
          <div class="card mt-8">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">Today</h2>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div class="p-4 bg-gray-50 rounded-lg text-center">
                <p class="text-2xl font-bold text-primary-600">{{ progress()!.todayStats.wordsLearned }}</p>
                <p class="text-sm text-gray-600">New Words</p>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg text-center">
                <p class="text-2xl font-bold text-blue-600">{{ progress()!.todayStats.wordsReviewed }}</p>
                <p class="text-sm text-gray-600">Reviewed</p>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg text-center">
                <p class="text-2xl font-bold text-green-600">{{ progress()!.todayStats.correctAnswers }}</p>
                <p class="text-sm text-gray-600">Correct</p>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg text-center">
                <p class="text-2xl font-bold text-orange-600">{{ progress()!.todayStats.timeSpentMinutes }}</p>
                <p class="text-sm text-gray-600">Minutes</p>
              </div>
            </div>

            <!-- Daily Goal Progress -->
            <div class="mt-4">
              <div class="flex justify-between text-sm mb-2">
                <span class="text-gray-600">Daily Goal Progress</span>
                <span class="font-medium">{{ progress()!.todayStats.goalProgress }}%</span>
              </div>
              <div class="h-3 bg-gray-200 rounded-full overflow-hidden">
                <div
                  class="h-full bg-primary-600 rounded-full transition-all duration-300"
                  [style.width.%]="Math.min(progress()!.todayStats.goalProgress, 100)"
                ></div>
              </div>
            </div>
          </div>

        }
      </div>
    </div>
  `
})
export class ProgressComponent implements OnInit {
  private readonly progressService = inject(ProgressService);

  readonly loading = signal(true);
  readonly progress = signal<OverallProgress | null>(null);
  readonly achievements = signal<AchievementsResponse | null>(null);
  readonly streak = signal<StreakInfo | null>(null);
  readonly activity = signal<ActivityData | null>(null);

  readonly cefrLevels: CefrLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
  readonly Math = Math;

  ngOnInit(): void {
    this.loadProgress();
    this.loadAchievements();
    this.loadStreak();
    this.loadActivity();
  }

  private loadProgress(): void {
    this.progressService.getOverallProgress().subscribe({
      next: (response) => {
        if (response.success) {
          this.progress.set(response.data);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private loadAchievements(): void {
    this.progressService.getAchievements().subscribe({
      next: (response) => {
        if (response.success) {
          this.achievements.set(response.data);
        }
      }
    });
  }

  private loadStreak(): void {
    this.progressService.getStreak().subscribe({
      next: (response) => {
        if (response.success) {
          this.streak.set(response.data);
        }
      }
    });
  }

  private loadActivity(): void {
    this.progressService.getActivityHeatmap().subscribe({
      next: (response) => {
        if (response.success) {
          this.activity.set(response.data);
        }
      }
    });
  }

  formatTime(minutes: number): string {
    if (minutes < 60) {
      return `${minutes}m`;
    }
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  }

  getWeeks(): Array<Array<{ date: string; count: number; level: number }>> {
    const activities = this.activity()?.activities || [];
    const weeks: Array<Array<{ date: string; count: number; level: number }>> = [];

    // Group into weeks (7 days each), show last 12 weeks
    const last84Days = activities.slice(-84);
    for (let i = 0; i < last84Days.length; i += 7) {
      weeks.push(last84Days.slice(i, i + 7));
    }

    return weeks;
  }

  getActivityClass(level: number): string {
    const classes: Record<number, string> = {
      0: 'bg-gray-100',
      1: 'bg-green-200',
      2: 'bg-green-400',
      3: 'bg-green-600',
      4: 'bg-green-800'
    };
    return classes[level] || 'bg-gray-100';
  }

  getLevelTextClass(level: string): string {
    const classes: Record<string, string> = {
      A1: 'text-green-600',
      A2: 'text-blue-600',
      B1: 'text-yellow-600',
      B2: 'text-orange-600',
      C1: 'text-red-600',
      C2: 'text-purple-600'
    };
    return classes[level] || 'text-gray-600';
  }

  getLevelBgClass(level: string): string {
    const classes: Record<string, string> = {
      A1: 'bg-green-500',
      A2: 'bg-blue-500',
      B1: 'bg-yellow-500',
      B2: 'bg-orange-500',
      C1: 'bg-red-500',
      C2: 'bg-purple-500'
    };
    return classes[level] || 'bg-gray-500';
  }
}
