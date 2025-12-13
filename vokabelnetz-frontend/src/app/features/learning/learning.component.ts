import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LearningService } from '../../core/services/learning.service';
import { AudioService } from '../../core/services/audio.service';
import { LearningStore } from '../../core/state/learning.store';
import { FlashcardComponent } from '../../shared/components/flashcard/flashcard.component';
import { SessionType, CefrLevel } from '../../core/models';

@Component({
  selector: 'app-learning',
  standalone: true,
  imports: [CommonModule, FlashcardComponent],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-2xl mx-auto">

        <!-- Session not started - Show start options -->
        @if (!learningStore.hasActiveSession() && !learningStore.sessionSummary()) {
          <div class="text-center mb-8">
            <h1 class="text-3xl font-bold text-gray-900">Start Learning</h1>
            <p class="mt-2 text-gray-600">Choose your learning mode to begin</p>
          </div>

          <div class="grid gap-4">
            <!-- Review Mode -->
            <button
              (click)="startSession('REVIEW')"
              [disabled]="learningStore.loading()"
              class="card hover:shadow-lg transition-shadow text-left"
            >
              <div class="flex items-center space-x-4">
                <div class="p-3 bg-orange-100 rounded-lg">
                  <svg class="w-8 h-8 text-orange-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                </div>
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">Review Words</h3>
                  <p class="text-sm text-gray-600">Practice words you've learned before</p>
                </div>
              </div>
            </button>

            <!-- New Words Mode -->
            <button
              (click)="startSession('LEARN')"
              [disabled]="learningStore.loading()"
              class="card hover:shadow-lg transition-shadow text-left"
            >
              <div class="flex items-center space-x-4">
                <div class="p-3 bg-green-100 rounded-lg">
                  <svg class="w-8 h-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                  </svg>
                </div>
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">Learn New Words</h3>
                  <p class="text-sm text-gray-600">Discover new German vocabulary</p>
                </div>
              </div>
            </button>

            <!-- Mixed Mode -->
            <button
              (click)="startSession('MIXED')"
              [disabled]="learningStore.loading()"
              class="card hover:shadow-lg transition-shadow text-left"
            >
              <div class="flex items-center space-x-4">
                <div class="p-3 bg-primary-100 rounded-lg">
                  <svg class="w-8 h-8 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
                  </svg>
                </div>
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">Mixed Practice</h3>
                  <p class="text-sm text-gray-600">Review and learn new words together</p>
                </div>
              </div>
            </button>
          </div>

          @if (learningStore.error()) {
            <div class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ learningStore.error() }}</p>
            </div>
          }
        }

        <!-- Active Session -->
        @if (learningStore.hasActiveSession() && learningStore.word()) {
          <!-- Progress bar -->
          <div class="mb-6">
            <div class="flex justify-between text-sm text-gray-600 mb-2">
              <span>{{ learningStore.progress()?.current || 0 }} / {{ learningStore.progress()?.total || 0 }}</span>
              <span>{{ learningStore.accuracy() }}% accuracy</span>
            </div>
            <div class="h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                class="h-full bg-primary-600 rounded-full transition-all duration-300"
                [style.width.%]="learningStore.progressPercentage()"
              ></div>
            </div>
          </div>

          <!-- Review/New badge -->
          <div class="text-center mb-4">
            @if (learningStore.isReview()) {
              <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-orange-100 text-orange-700">
                <svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Review
              </span>
            } @else {
              <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-700">
                <svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                New Word
              </span>
            }
          </div>

          <!-- Flashcard -->
          <app-flashcard
            [word]="learningStore.word()"
            [flipped]="learningStore.answerRevealed()"
            (flip)="onFlipCard()"
            (playAudio)="onPlayAudio($event)"
          />

          <!-- Answer buttons -->
          @if (learningStore.answerRevealed()) {
            <div class="mt-8 space-y-4">
              <p class="text-center text-gray-600">How well did you know this?</p>
              <div class="grid grid-cols-2 gap-4">
                <button
                  (click)="submitAnswer(false, 1)"
                  class="py-4 px-6 bg-red-100 hover:bg-red-200 text-red-700 rounded-xl font-medium transition-colors"
                >
                  Didn't know
                </button>
                <button
                  (click)="submitAnswer(true, 3)"
                  class="py-4 px-6 bg-yellow-100 hover:bg-yellow-200 text-yellow-700 rounded-xl font-medium transition-colors"
                >
                  Hard
                </button>
                <button
                  (click)="submitAnswer(true, 4)"
                  class="py-4 px-6 bg-blue-100 hover:bg-blue-200 text-blue-700 rounded-xl font-medium transition-colors"
                >
                  Good
                </button>
                <button
                  (click)="submitAnswer(true, 5)"
                  class="py-4 px-6 bg-green-100 hover:bg-green-200 text-green-700 rounded-xl font-medium transition-colors"
                >
                  Easy
                </button>
              </div>
            </div>
          } @else {
            <div class="mt-8 text-center">
              <button
                (click)="onFlipCard()"
                class="btn btn-primary px-8 py-3"
              >
                Show Answer
              </button>
            </div>
          }

          <!-- End session button -->
          <div class="mt-8 text-center">
            <button
              (click)="endSession()"
              class="text-sm text-gray-500 hover:text-gray-700"
            >
              End Session Early
            </button>
          </div>
        }

        <!-- Session Summary -->
        @if (learningStore.sessionSummary()) {
          <div class="text-center">
            <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 mb-4">
              <svg class="w-8 h-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h1 class="text-3xl font-bold text-gray-900">Session Complete!</h1>
          </div>

          <div class="mt-8 grid grid-cols-2 gap-4">
            <div class="card text-center">
              <p class="text-3xl font-bold text-primary-600">{{ learningStore.sessionSummary()?.summary?.totalWords }}</p>
              <p class="text-sm text-gray-600">Words Practiced</p>
            </div>
            <div class="card text-center">
              <p class="text-3xl font-bold text-green-600">{{ learningStore.sessionSummary()?.summary?.accuracy | number:'1.0-0' }}%</p>
              <p class="text-sm text-gray-600">Accuracy</p>
            </div>
            <div class="card text-center">
              <p class="text-3xl font-bold text-blue-600">{{ learningStore.sessionSummary()?.summary?.correctAnswers }}</p>
              <p class="text-sm text-gray-600">Correct</p>
            </div>
            <div class="card text-center">
              <p class="text-3xl font-bold text-orange-600">
                @if ((learningStore.sessionSummary()?.eloChange?.change || 0) >= 0) { + }
                {{ learningStore.sessionSummary()?.eloChange?.change }}
              </p>
              <p class="text-sm text-gray-600">Elo Change</p>
            </div>
          </div>

          @if (learningStore.sessionSummary()?.streakMaintained) {
            <div class="mt-6 p-4 bg-orange-50 border border-orange-200 rounded-lg text-center">
              <p class="text-lg font-medium text-orange-700">Streak maintained!</p>
            </div>
          }

          @if (learningStore.sessionSummary()?.dailyGoalProgress?.completed) {
            <div class="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg text-center">
              <p class="text-lg font-medium text-green-700">Daily goal completed!</p>
            </div>
          }

          <div class="mt-8 flex gap-4">
            <button
              (click)="goToDashboard()"
              class="flex-1 btn bg-gray-100 hover:bg-gray-200 text-gray-700"
            >
              Back to Dashboard
            </button>
            <button
              (click)="startNewSession()"
              class="flex-1 btn btn-primary"
            >
              Practice Again
            </button>
          </div>
        }

        <!-- Loading state -->
        @if (learningStore.loading()) {
          <div class="fixed inset-0 bg-white/50 flex items-center justify-center z-50">
            <div class="text-center">
              <svg class="animate-spin h-8 w-8 text-primary-600 mx-auto" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <p class="mt-2 text-gray-600">Loading...</p>
            </div>
          </div>
        }
      </div>
    </div>
  `
})
export class LearningComponent implements OnInit, OnDestroy {
  private readonly learningService = inject(LearningService);
  private readonly audioService = inject(AudioService);
  private readonly router = inject(Router);
  protected readonly learningStore = inject(LearningStore);

  ngOnInit(): void {
    // Check if there's an active session
    this.checkActiveSession();
  }

  ngOnDestroy(): void {
    // Clean up if leaving mid-session
  }

  private checkActiveSession(): void {
    this.learningService.getCurrentSession().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.learningStore.setSession(response.data);
          this.loadNextWord();
        }
      },
      error: () => {
        // No active session, show start options
      }
    });
  }

  startSession(type: SessionType): void {
    this.learningStore.setLoading(true);

    this.learningService.startSession({
      sessionType: type,
      wordCount: 20
    }).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.learningStore.setSession(response.data);
          this.loadNextWord();
        }
      },
      error: (error) => {
        this.learningStore.setError(error?.error?.error?.message || 'Failed to start session');
      }
    });
  }

  private loadNextWord(): void {
    const session = this.learningStore.session();
    if (!session) return;

    this.learningStore.setLoading(true);

    this.learningService.getNextWord(session.sessionId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.learningStore.setCurrentWord(response.data);
        }
        this.learningStore.setLoading(false);
      },
      error: (error) => {
        // No more words - end session
        if (error?.status === 404) {
          this.endSession();
        } else {
          this.learningStore.setError(error?.error?.error?.message || 'Failed to load word');
        }
      }
    });
  }

  onFlipCard(): void {
    this.learningStore.revealAnswer();
  }

  onPlayAudio(word: string): void {
    const currentWord = this.learningStore.word();
    const level = currentWord?.cefrLevel || 'A1';
    this.audioService.playWord(word, level);
  }

  submitAnswer(correct: boolean, quality: number): void {
    const session = this.learningStore.session();
    const word = this.learningStore.word();

    if (!session || !word) return;

    this.learningStore.setLoading(true);

    this.learningService.submitAnswer({
      sessionId: session.sessionId,
      wordId: word.id,
      correct,
      quality,
      responseTimeMs: this.learningStore.getResponseTime()
    }).subscribe({
      next: (response) => {
        if (response.success) {
          // Check if session is complete
          if (response.data.sessionProgress.remainingWords === 0) {
            this.endSession();
          } else {
            this.loadNextWord();
          }
        }
      },
      error: (error) => {
        this.learningStore.setError(error?.error?.error?.message || 'Failed to submit answer');
      }
    });
  }

  endSession(): void {
    const session = this.learningStore.session();
    if (!session) return;

    this.learningStore.setLoading(true);

    this.learningService.endSession(session.sessionId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.learningStore.setSessionSummary(response.data);
        }
        this.learningStore.setLoading(false);
      },
      error: (error) => {
        this.learningStore.setError(error?.error?.error?.message || 'Failed to end session');
      }
    });
  }

  startNewSession(): void {
    this.learningStore.clearSummary();
  }

  goToDashboard(): void {
    this.learningStore.clearSession();
    this.router.navigate(['/dashboard']);
  }
}
