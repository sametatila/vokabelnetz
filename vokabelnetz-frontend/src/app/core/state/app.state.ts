import { Injectable, computed, inject } from '@angular/core';
import { AuthStore } from './auth.store';
import { LearningStore } from './learning.store';
import { ProgressStore } from './progress.store';

/**
 * AppState - Provides cross-store computed signals.
 *
 * This service aggregates state from multiple stores and provides
 * computed signals that depend on multiple stores.
 *
 * Based on docs/ARCHITECTURE.md State Management section.
 */
@Injectable({ providedIn: 'root' })
export class AppState {
  private readonly authStore = inject(AuthStore);
  private readonly learningStore = inject(LearningStore);
  private readonly progressStore = inject(ProgressStore);

  /**
   * App is ready when auth is initialized and progress is loaded.
   */
  readonly isReady = computed(() =>
    this.authStore.isInitialized() &&
    (!this.authStore.isAuthenticated() || this.progressStore.isLoaded())
  );

  /**
   * User can start a new learning session.
   */
  readonly canStartSession = computed(() =>
    this.authStore.isAuthenticated() &&
    !this.learningStore.hasActiveSession()
  );

  /**
   * Today's learning goal is complete.
   */
  readonly todayComplete = computed(() =>
    this.progressStore.todayCompleted()
  );

  /**
   * User's current streak is at risk (not completed today).
   */
  readonly streakAtRisk = computed(() =>
    this.authStore.isAuthenticated() &&
    this.progressStore.streakAtRisk() &&
    !this.progressStore.todayCompleted()
  );

  /**
   * Current user display name or 'Guest'.
   */
  readonly displayName = computed(() =>
    this.authStore.displayName()
  );

  /**
   * Is any loading operation in progress.
   */
  readonly isLoading = computed(() =>
    this.authStore.loading() ||
    this.learningStore.loading() ||
    this.progressStore.loading()
  );

  /**
   * Current daily progress percentage.
   */
  readonly dailyProgressPercentage = computed(() =>
    this.progressStore.dailyProgress().percentage
  );

  /**
   * Words learned today.
   */
  readonly wordsLearnedToday = computed(() =>
    this.progressStore.dailyProgress().wordsLearned
  );

  /**
   * Daily word goal.
   */
  readonly dailyGoal = computed(() =>
    this.progressStore.dailyProgress().goal
  );
}
