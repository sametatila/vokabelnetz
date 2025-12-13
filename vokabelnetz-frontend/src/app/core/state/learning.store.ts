import { Injectable, computed, signal } from '@angular/core';
import {
  LearningSession,
  NextWordResponse,
  SessionSummary,
  Word,
  SessionProgress
} from '../models';

export interface LearningState {
  session: LearningSession | null;
  currentWord: NextWordResponse | null;
  sessionSummary: SessionSummary | null;
  loading: boolean;
  error: string | null;
  answerRevealed: boolean;
  startTime: number | null;
}

/**
 * LearningStore - Manages learning session state using Angular Signals.
 *
 * Based on docs/ARCHITECTURE.md State Management.
 */
@Injectable({ providedIn: 'root' })
export class LearningStore {
  // Private writable state
  private state = signal<LearningState>({
    session: null,
    currentWord: null,
    sessionSummary: null,
    loading: false,
    error: null,
    answerRevealed: false,
    startTime: null
  });

  // Public read-only selectors
  readonly session = computed(() => this.state().session);
  readonly currentWord = computed(() => this.state().currentWord);
  readonly sessionSummary = computed(() => this.state().sessionSummary);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
  readonly answerRevealed = computed(() => this.state().answerRevealed);

  // Computed derived state
  readonly hasActiveSession = computed(() => !!this.state().session);
  readonly word = computed(() => this.state().currentWord?.word || null);
  readonly progress = computed(() => this.state().currentWord?.sessionProgress || null);
  readonly isReview = computed(() => this.state().currentWord?.isReview || false);

  readonly progressPercentage = computed(() => {
    const progress = this.state().currentWord?.sessionProgress;
    if (!progress) return 0;
    return Math.round((progress.current / progress.total) * 100);
  });

  readonly accuracy = computed(() => {
    const progress = this.state().currentWord?.sessionProgress;
    if (!progress || progress.current === 0) return 0;
    return Math.round((progress.correctSoFar / progress.current) * 100);
  });

  /**
   * Set session data after starting a session.
   */
  setSession(session: LearningSession): void {
    this.state.update(s => ({
      ...s,
      session,
      sessionSummary: null,
      error: null
    }));
  }

  /**
   * Set current word data.
   */
  setCurrentWord(word: NextWordResponse): void {
    this.state.update(s => ({
      ...s,
      currentWord: word,
      answerRevealed: false,
      startTime: Date.now(),
      error: null
    }));
  }

  /**
   * Reveal the answer for current word.
   */
  revealAnswer(): void {
    this.state.update(s => ({ ...s, answerRevealed: true }));
  }

  /**
   * Get response time for current word.
   */
  getResponseTime(): number {
    const startTime = this.state().startTime;
    if (!startTime) return 0;
    return Date.now() - startTime;
  }

  /**
   * Set session summary after ending session.
   */
  setSessionSummary(summary: SessionSummary): void {
    this.state.update(s => ({
      ...s,
      sessionSummary: summary,
      session: null,
      currentWord: null
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
   * Clear all learning state.
   */
  clearSession(): void {
    this.state.set({
      session: null,
      currentWord: null,
      sessionSummary: null,
      loading: false,
      error: null,
      answerRevealed: false,
      startTime: null
    });
  }

  /**
   * Clear session summary (after viewing results).
   */
  clearSummary(): void {
    this.state.update(s => ({ ...s, sessionSummary: null }));
  }
}
