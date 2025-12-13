import { CefrLevel } from './user.model';
import { Word, UserWordProgress } from './word.model';

export interface LearningSession {
  id: number;
  sessionType: SessionType;
  cefrLevel: CefrLevel;
  startedAt: string;
  endedAt?: string;
  wordsReviewed: number;
  wordsCorrect: number;
  totalTimeSeconds?: number;
}

export type SessionType = 'REVIEW' | 'NEW' | 'MIXED' | 'QUIZ';

export interface StartSessionRequest {
  sessionType: SessionType;
  cefrLevel: CefrLevel;
  wordCount?: number;
}

export interface StartSessionResponse {
  sessionId: number;
  sessionType: SessionType;
  cefrLevel: CefrLevel;
  wordsToReview: number;
  newWordsAvailable: number;
  startedAt: string;
}

export interface NextWordResponse {
  word: Word;
  progress?: UserWordProgress;
  isReview: boolean;
  dueCount: number;
}

export interface AnswerRequest {
  sessionId?: number;
  wordId: number;
  correct: boolean;
  recognized?: boolean;
  usedHint?: boolean;
  responseTimeMs: number;
  userAnswer?: string;
}

export interface AnswerResponse {
  correct: boolean;
  quality: number;
  eloChange: number;
  newUserRating: number;
  newWordRating: number;
  expectedScore: number;
  newEaseFactor: number;
  newInterval: number;
  nextReviewAt: string;
  isLearned: boolean;
  streakStatus: StreakStatus;
}

export interface StreakStatus {
  currentStreak: number;
  longestStreak: number;
  todayCompleted: boolean;
  streakFreezes: number;
  lastActiveDate?: string;
}

export interface SessionSummary {
  duration: number;
  wordsReviewed: number;
  wordsNew: number;
  totalWords: number;
  correctAnswers: number;
  incorrectAnswers: number;
  accuracy: number;
  averageResponseTimeMs: number;
  eloChange: {
    startRating: number;
    endRating: number;
    change: number;
  };
  streakMaintained: boolean;
  dailyGoalProgress: {
    current: number;
    goal: number;
    completed: boolean;
  };
}
