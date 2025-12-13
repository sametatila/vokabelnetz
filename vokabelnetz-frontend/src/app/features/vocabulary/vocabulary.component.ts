import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VocabularyService, WordStats } from '../../core/services/vocabulary.service';
import { LanguageService } from '../../core/services/language.service';
import { Word, CefrLevel } from '../../core/models';

@Component({
  selector: 'app-vocabulary',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-6xl mx-auto">

        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Vocabulary</h1>
          <p class="mt-2 text-gray-600">Browse and search German words</p>
        </div>

        <!-- Stats Cards -->
        @if (stats()) {
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <div class="card text-center">
              <p class="text-2xl font-bold text-primary-600">{{ stats()!.totalWords }}</p>
              <p class="text-sm text-gray-600">Total Words</p>
            </div>
            @for (level of cefrLevels; track level) {
              <div class="card text-center cursor-pointer hover:shadow-lg transition-shadow"
                   [class.ring-2]="selectedLevel() === level"
                   [class.ring-primary-500]="selectedLevel() === level"
                   (click)="filterByLevel(level)">
                <p class="text-2xl font-bold" [class]="getLevelTextClass(level)">
                  {{ stats()!.byLevel[level] || 0 }}
                </p>
                <p class="text-sm text-gray-600">{{ level }}</p>
              </div>
            }
          </div>
        }

        <!-- Search and Filter -->
        <div class="card mb-6">
          <div class="flex flex-col md:flex-row gap-4">
            <!-- Search Input -->
            <div class="flex-1 relative">
              <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                type="text"
                [(ngModel)]="searchQuery"
                (input)="onSearch()"
                placeholder="Search words..."
                class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>

            <!-- Level Filter -->
            <div class="flex gap-2 flex-wrap">
              <button
                (click)="clearFilter()"
                class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                [class]="!selectedLevel() ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
              >
                All
              </button>
              @for (level of cefrLevels; track level) {
                <button
                  (click)="filterByLevel(level)"
                  class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                  [class]="selectedLevel() === level ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                >
                  {{ level }}
                </button>
              }
            </div>
          </div>
        </div>

        <!-- Word List -->
        @if (loading()) {
          <div class="text-center py-12">
            <svg class="animate-spin h-8 w-8 text-primary-600 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p class="mt-2 text-gray-600">Loading words...</p>
          </div>
        } @else if (words().length === 0) {
          <div class="text-center py-12 card">
            <svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p class="text-gray-600">No words found</p>
            @if (searchQuery || selectedLevel()) {
              <button (click)="clearFilter(); searchQuery = ''; loadWords()" class="mt-4 text-primary-600 hover:underline">
                Clear filters
              </button>
            }
          </div>
        } @else {
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            @for (word of words(); track word.id) {
              <div class="card hover:shadow-lg transition-shadow cursor-pointer" (click)="selectWord(word)">
                <!-- Header with badges -->
                <div class="flex justify-between items-start mb-3">
                  <span class="text-xs font-medium px-2 py-1 rounded-full bg-gray-100 text-gray-600">
                    {{ getWordTypeLabel(word.wordType) }}
                  </span>
                  <span class="text-xs font-medium px-2 py-1 rounded-full" [class]="getLevelClass(word.cefrLevel)">
                    {{ word.cefrLevel }}
                  </span>
                </div>

                <!-- German word -->
                <div class="mb-2">
                  @if (word.article) {
                    <span class="text-gray-500 text-sm">{{ word.article }} </span>
                  }
                  <span class="text-xl font-bold text-gray-900">{{ word.german }}</span>
                </div>

                <!-- Translation -->
                <p class="text-gray-600">{{ getTranslation(word) }}</p>
              </div>
            }
          </div>

          <!-- Pagination -->
          @if (totalPages() > 1) {
            <div class="mt-8 flex justify-center items-center gap-2">
              <button
                (click)="goToPage(currentPage() - 1)"
                [disabled]="currentPage() === 0"
                class="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-200"
              >
                Previous
              </button>

              <span class="px-4 py-2 text-gray-600">
                Page {{ currentPage() + 1 }} of {{ totalPages() }}
              </span>

              <button
                (click)="goToPage(currentPage() + 1)"
                [disabled]="currentPage() >= totalPages() - 1"
                class="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-200"
              >
                Next
              </button>
            </div>
          }
        }

        <!-- Word Detail Modal -->
        @if (selectedWord()) {
          <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" (click)="closeWordDetail()">
            <div class="bg-white rounded-2xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto" (click)="$event.stopPropagation()">
              <div class="p-6">
                <!-- Close button -->
                <div class="flex justify-end mb-4">
                  <button (click)="closeWordDetail()" class="text-gray-400 hover:text-gray-600">
                    <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>

                <!-- Badges -->
                <div class="flex gap-2 mb-4">
                  <span class="text-xs font-medium px-2 py-1 rounded-full bg-gray-100 text-gray-600">
                    {{ getWordTypeLabel(selectedWord()!.wordType) }}
                  </span>
                  <span class="text-xs font-medium px-2 py-1 rounded-full" [class]="getLevelClass(selectedWord()!.cefrLevel)">
                    {{ selectedWord()!.cefrLevel }}
                  </span>
                </div>

                <!-- German word -->
                <div class="text-center mb-6">
                  @if (selectedWord()!.article) {
                    <span class="text-xl text-gray-500">{{ selectedWord()!.article }}</span>
                  }
                  <h2 class="text-4xl font-bold text-gray-900">{{ selectedWord()!.german }}</h2>
                </div>

                <!-- Translations -->
                <div class="mb-6 p-4 bg-primary-50 rounded-lg">
                  <h3 class="text-sm font-medium text-primary-700 mb-2">Translations</h3>
                  <p class="text-lg text-primary-900">
                    <span class="font-medium">TR:</span> {{ selectedWord()!.translations.tr }}
                  </p>
                  <p class="text-lg text-primary-900">
                    <span class="font-medium">EN:</span> {{ selectedWord()!.translations.en }}
                  </p>
                </div>

                <!-- Example sentence -->
                @if (selectedWord()!.exampleSentences) {
                  <div class="p-4 bg-gray-50 rounded-lg">
                    <h3 class="text-sm font-medium text-gray-700 mb-2">Example Sentence</h3>
                    <p class="text-gray-900 italic mb-2">{{ selectedWord()!.exampleSentences!.de }}</p>
                    <p class="text-gray-600 text-sm">{{ getExampleTranslation(selectedWord()!) }}</p>
                  </div>
                }

                <!-- Audio button -->
                <div class="mt-6 text-center">
                  <button
                    (click)="playAudio(selectedWord()!.german)"
                    class="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                  >
                    <svg class="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                    </svg>
                    Play Audio
                  </button>
                </div>
              </div>
            </div>
          </div>
        }

      </div>
    </div>
  `
})
export class VocabularyComponent implements OnInit {
  private readonly vocabularyService = inject(VocabularyService);
  private readonly languageService = inject(LanguageService);

  // State
  readonly words = signal<Word[]>([]);
  readonly stats = signal<WordStats | null>(null);
  readonly loading = signal(false);
  readonly selectedLevel = signal<CefrLevel | null>(null);
  readonly selectedWord = signal<Word | null>(null);
  readonly currentPage = signal(0);
  readonly totalItems = signal(0);
  readonly pageSize = 12;

  searchQuery = '';
  private searchTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly cefrLevels: CefrLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];

  readonly totalPages = computed(() => Math.ceil(this.totalItems() / this.pageSize));

  ngOnInit(): void {
    this.loadStats();
    this.loadWords();
  }

  private loadStats(): void {
    this.vocabularyService.getWordStats().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats.set(response.data);
        }
      }
    });
  }

  loadWords(): void {
    this.loading.set(true);

    const level = this.selectedLevel();
    const query = this.searchQuery.trim();

    if (query) {
      this.vocabularyService.searchWords(query, this.currentPage(), this.pageSize).subscribe({
        next: (response) => this.handleWordsResponse(response),
        error: () => this.handleError()
      });
    } else if (level) {
      this.vocabularyService.getWordsByLevel(level, this.currentPage(), this.pageSize).subscribe({
        next: (response) => this.handleWordsResponse(response),
        error: () => this.handleError()
      });
    } else {
      this.vocabularyService.getWords(this.currentPage(), this.pageSize).subscribe({
        next: (response) => this.handleWordsResponse(response),
        error: () => this.handleError()
      });
    }
  }

  private handleWordsResponse(response: { success: boolean; data: Word[]; meta?: { total: number } }): void {
    if (response.success) {
      this.words.set(response.data);
      this.totalItems.set(response.meta?.total || 0);
    }
    this.loading.set(false);
  }

  private handleError(): void {
    this.words.set([]);
    this.loading.set(false);
  }

  onSearch(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.currentPage.set(0);
      this.loadWords();
    }, 300);
  }

  filterByLevel(level: CefrLevel): void {
    if (this.selectedLevel() === level) {
      this.selectedLevel.set(null);
    } else {
      this.selectedLevel.set(level);
    }
    this.currentPage.set(0);
    this.loadWords();
  }

  clearFilter(): void {
    this.selectedLevel.set(null);
    this.currentPage.set(0);
    this.loadWords();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadWords();
    }
  }

  selectWord(word: Word): void {
    this.selectedWord.set(word);
  }

  closeWordDetail(): void {
    this.selectedWord.set(null);
  }

  playAudio(word: string): void {
    // TODO: Implement audio playback
    console.log('Play audio for:', word);
  }

  getTranslation(word: Word): string {
    const lang = this.languageService.currentSourceLanguage();
    return lang === 'tr' ? word.translations.tr : word.translations.en;
  }

  getExampleTranslation(word: Word): string {
    if (!word.exampleSentences) return '';
    const lang = this.languageService.currentSourceLanguage();
    return lang === 'tr' ? word.exampleSentences.tr : word.exampleSentences.en;
  }

  getWordTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      NOUN: 'Noun',
      VERB: 'Verb',
      ADJECTIVE: 'Adjective',
      ADVERB: 'Adverb',
      PREPOSITION: 'Preposition',
      CONJUNCTION: 'Conjunction',
      PRONOUN: 'Pronoun',
      ARTICLE: 'Article',
      OTHER: 'Other'
    };
    return labels[type] || type;
  }

  getLevelClass(level: string): string {
    const classes: Record<string, string> = {
      A1: 'bg-green-100 text-green-700',
      A2: 'bg-blue-100 text-blue-700',
      B1: 'bg-yellow-100 text-yellow-700',
      B2: 'bg-orange-100 text-orange-700',
      C1: 'bg-red-100 text-red-700',
      C2: 'bg-purple-100 text-purple-700'
    };
    return classes[level] || 'bg-gray-100 text-gray-700';
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
}
