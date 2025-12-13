import { Injectable, computed, signal } from '@angular/core';
import { StreakStatus, DailyStats } from '../models';

export interface ProgressState {
  streakInfo: StreakStatus | null;
  dailyStats: DailyStats | null;
  loading: boolean;
  error: string | null;
  isLoaded: boolean;
}

/**
 * ProgressStore - Manages progress and statistics state using Angular Signals.
 *
 * Based on docs/ARCHITECTURE.md State Management.
 */
@Injectable({ providedIn: 'root' })
export class ProgressStore {
  // Private writable state
  private state = signal<ProgressState>({
    streakInfo: null,
    dailyStats: null,
    loading: false,
    error: null,
    isLoaded: false
  });

  // Public read-only selectors
  readonly streakInfo = computed(() => this.state().streakInfo);
  readonly dailyStats = computed(() => this.state().dailyStats);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
  readonly isLoaded = computed(() => this.state().isLoaded);

  // Computed derived state
  readonly currentStreak = computed(() => this.state().streakInfo?.currentStreak || 0);
  readonly longestStreak = computed(() => this.state().streakInfo?.longestStreak || 0);
  readonly todayCompleted = computed(() => this.state().streakInfo?.todayCompleted || false);
  readonly streakAtRisk = computed(() => this.state().streakInfo?.atRisk || false);

  readonly todayWordsReviewed = computed(() => this.state().dailyStats?.wordsReviewed || 0);
  readonly todayWordsCorrect = computed(() => this.state().dailyStats?.wordsCorrect || 0);
  readonly todayNewWords = computed(() => this.state().dailyStats?.newWordsLearned || 0);

  readonly dailyProgress = computed(() => {
    const stats = this.state().dailyStats;
    if (!stats) return { wordsLearned: 0, goal: 20, percentage: 0 };

    const wordsLearned = stats.wordsReviewed + stats.newWordsLearned;
    const goal = 20; // Default daily goal
    const percentage = Math.min(Math.round((wordsLearned / goal) * 100), 100);

    return { wordsLearned, goal, percentage };
  });

  /**
   * Set streak information.
   */
  setStreakInfo(streakInfo: StreakStatus): void {
    this.state.update(s => ({
      ...s,
      streakInfo,
      error: null
    }));
  }

  /**
   * Set daily statistics.
   */
  setDailyStats(dailyStats: DailyStats): void {
    this.state.update(s => ({
      ...s,
      dailyStats,
      error: null
    }));
  }

  /**
   * Set loading state.
   */
  setLoading(loading: boolean): void {
    this.state.update(s => ({ ...s, loading }));
  }

  /**
   * Set error state.
   */
  setError(error: string | null): void {
    this.state.update(s => ({ ...s, error, loading: false }));
  }

  /**
   * Mark progress as loaded.
   */
  setLoaded(isLoaded: boolean): void {
    this.state.update(s => ({ ...s, isLoaded }));
  }

  /**
   * Update streak after learning session.
   */
  updateStreakFromSession(maintained: boolean, newStreak: number): void {
    this.state.update(s => ({
      ...s,
      streakInfo: s.streakInfo ? {
        ...s.streakInfo,
        currentStreak: newStreak,
        todayCompleted: true,
        atRisk: false
      } : null
    }));
  }

  /**
   * Clear all progress state.
   */
  clearProgress(): void {
    this.state.set({
      streakInfo: null,
      dailyStats: null,
      loading: false,
      error: null,
      isLoaded: false
    });
  }
}
