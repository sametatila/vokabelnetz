import { CefrLevel } from './user.model';

export interface Word {
  id: number;
  german: string;
  translations: {
    tr: string;
    en: string;
  };
  article?: string;
  wordType: WordType;
  pluralForm?: string;
  cefrLevel: CefrLevel;
  exampleSentences?: {
    de: string;
    tr: string;
    en: string;
  };
  audioUrl?: string;
  phoneticSpelling?: string;
  difficultyRating: number;
  category: WordCategory;
  timesShown?: number;
  timesCorrect?: number;
}

export type WordType =
  | 'NOUN'
  | 'VERB'
  | 'ADJECTIVE'
  | 'ADVERB'
  | 'PREPOSITION'
  | 'CONJUNCTION'
  | 'PRONOUN'
  | 'ARTICLE'
  | 'INTERJECTION'
  | 'NUMERAL'
  | 'PHRASE';

export type WordCategory =
  | 'ALLTAG'
  | 'ARBEIT_BERUF'
  | 'BILDUNG'
  | 'EINKAUFEN'
  | 'ESSEN_TRINKEN'
  | 'FAMILIE_SOZIALES'
  | 'FREIZEIT_HOBBYS'
  | 'GESUNDHEIT'
  | 'KLEIDUNG'
  | 'KOMMUNIKATION'
  | 'KORPER'
  | 'KULTUR_MEDIEN'
  | 'NATUR_UMWELT'
  | 'ORTE_RAUME'
  | 'REISEN_VERKEHR'
  | 'SPORT'
  | 'TECHNIK'
  | 'WETTER_ZEIT'
  | 'WOHNEN'
  | 'ZAHLEN_MENGEN'
  | 'SONSTIGES';

export interface UserWordProgress {
  id: number;
  wordId: number;
  easeFactor: number;
  intervalDays: number;
  repetition: number;
  nextReviewAt: string;
  lastReviewedAt?: string;
  isLearned: boolean;
  timesCorrect: number;
  timesIncorrect: number;
  avgResponseTimeMs?: number;
}
