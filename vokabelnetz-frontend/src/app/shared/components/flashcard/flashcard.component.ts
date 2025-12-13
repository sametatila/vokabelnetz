import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Word } from '../../../core/models';
import { LanguageService } from '../../../core/services/language.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-flashcard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative w-full max-w-lg mx-auto perspective-1000">
      <div
        class="relative w-full h-80 transition-transform duration-500 transform-style-3d cursor-pointer"
        [class.rotate-y-180]="flipped"
        (click)="onFlip()"
      >
        <!-- Front of card (German word) -->
        <div class="absolute inset-0 backface-hidden">
          <div class="h-full bg-white rounded-2xl shadow-lg border border-gray-200 p-6 flex flex-col items-center justify-center">
            <!-- Word type badge -->
            @if (word?.wordType) {
              <span class="absolute top-4 left-4 text-xs font-medium px-2 py-1 rounded-full bg-gray-100 text-gray-600">
                {{ getWordTypeLabel(word!.wordType) }}
              </span>
            }

            <!-- CEFR Level badge -->
            @if (word?.cefrLevel) {
              <span class="absolute top-4 right-4 text-xs font-medium px-2 py-1 rounded-full"
                    [class]="getLevelClass(word!.cefrLevel)">
                {{ word!.cefrLevel }}
              </span>
            }

            <!-- Article + Word -->
            <div class="text-center">
              @if (word?.article) {
                <span class="text-lg text-gray-500">{{ word!.article }}</span>
              }
              <h2 class="text-4xl font-bold text-gray-900 mt-1">{{ word?.german }}</h2>
            </div>

            <!-- Audio button -->
            <button
              class="mt-6 p-3 rounded-full bg-gray-100 hover:bg-gray-200 transition-colors"
              (click)="onPlayAudio($event)"
            >
              <svg class="w-6 h-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
              </svg>
            </button>

            <!-- Flip hint -->
            <p class="absolute bottom-4 text-sm text-gray-400">
              Tap to reveal translation
            </p>
          </div>
        </div>

        <!-- Back of card (Translation) -->
        <div class="absolute inset-0 backface-hidden rotate-y-180">
          <div class="h-full bg-gradient-to-br from-primary-50 to-primary-100 rounded-2xl shadow-lg border border-primary-200 p-6 flex flex-col items-center justify-center">
            <!-- Translation -->
            <div class="text-center">
              <h2 class="text-3xl font-bold text-primary-900">{{ getTranslation() }}</h2>

              @if (showBothTranslations && word?.translations) {
                <p class="mt-2 text-lg text-primary-700">
                  {{ getSecondaryTranslation() }}
                </p>
              }
            </div>

            <!-- Example sentence -->
            @if (word?.exampleSentences) {
              <div class="mt-6 p-4 bg-white/50 rounded-lg max-w-sm">
                <p class="text-sm text-gray-700 italic">{{ word!.exampleSentences!.de }}</p>
                <p class="text-sm text-gray-500 mt-1">{{ getExampleTranslation() }}</p>
              </div>
            }

            <!-- Flip hint -->
            <p class="absolute bottom-4 text-sm text-primary-400">
              Tap to see word again
            </p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .perspective-1000 {
      perspective: 1000px;
    }
    .transform-style-3d {
      transform-style: preserve-3d;
    }
    .backface-hidden {
      backface-visibility: hidden;
    }
    .rotate-y-180 {
      transform: rotateY(180deg);
    }
  `]
})
export class FlashcardComponent {
  private readonly languageService = inject(LanguageService);

  @Input() word: Word | null = null;
  @Input() flipped = false;
  @Input() showBothTranslations = false;

  @Output() flip = new EventEmitter<void>();
  @Output() playAudio = new EventEmitter<string>();

  onFlip(): void {
    this.flip.emit();
  }

  onPlayAudio(event: Event): void {
    event.stopPropagation();
    if (this.word?.german) {
      this.playAudio.emit(this.word.german);
    }
  }

  getTranslation(): string {
    if (!this.word?.translations) return '';
    const lang = this.languageService.currentSourceLanguage();
    return lang === 'tr' ? this.word.translations.tr : this.word.translations.en;
  }

  getSecondaryTranslation(): string {
    if (!this.word?.translations) return '';
    const lang = this.languageService.currentSourceLanguage();
    return lang === 'tr' ? this.word.translations.en : this.word.translations.tr;
  }

  getExampleTranslation(): string {
    if (!this.word?.exampleSentences) return '';
    const lang = this.languageService.currentSourceLanguage();
    return lang === 'tr' ? this.word.exampleSentences.tr : this.word.exampleSentences.en;
  }

  getWordTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      NOUN: 'Noun',
      VERB: 'Verb',
      ADJECTIVE: 'Adj',
      ADVERB: 'Adv',
      PREPOSITION: 'Prep',
      CONJUNCTION: 'Conj',
      PRONOUN: 'Pron',
      ARTICLE: 'Art',
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
}
