// Auth models
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  nativeLanguage: 'TURKISH' | 'ENGLISH';
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface AuthResponse {
  success: boolean;
  data: {
    accessToken: string;
    refreshToken: string;
    user: AuthUser;
  };
}

export interface AuthUser {
  id: number;
  email: string;
  displayName: string;
  role: string;
  emailVerified: boolean;
  nativeLanguage: 'TURKISH' | 'ENGLISH';
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
  };
}

// User Profile
export interface UserProfile {
  id: number;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  eloRating: number;
  currentStreak: number;
  longestStreak: number;
  totalWordsLearned: number;
  dailyGoal: number;
  uiLanguage: string;
  sourceLanguage: string;
  timezone: string;
  isActive: boolean;
  emailVerified: boolean;
  lastActiveAt: string;
  createdAt: string;
}

// Daily Stats
export interface DailyStats {
  statDate: string;
  wordsReviewed: number;
  wordsCorrect: number;
  newWordsLearned: number;
  sessionsCompleted: number;
  totalTimeSeconds: number;
  streakMaintained: boolean;
}

// Streak Status
export interface StreakStatus {
  currentStreak: number;
  longestStreak: number;
  streakStartDate: string | null;
  lastActiveDate: string | null;
  todayCompleted: boolean;
  atRisk: boolean;
  freezesAvailable: number;
}

// Learning Types
export type SessionType = 'REVIEW' | 'NEW' | 'MIXED';
export type CefrLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2';
export type WordType = 'NOUN' | 'VERB' | 'ADJECTIVE' | 'ADVERB' | 'PREPOSITION' | 'CONJUNCTION' | 'PRONOUN' | 'ARTICLE' | 'OTHER';

// Learning Session
export interface StartSessionRequest {
  sessionType: SessionType;
  cefrLevel?: CefrLevel;
  wordCount?: number;
}

export interface LearningSession {
  sessionId: number;
  sessionType: SessionType;
  cefrLevel: CefrLevel;
  wordsToReview: number;
  newWordsAvailable: number;
  startedAt: string;
}

// Word
export interface Word {
  id: number;
  german: string;
  translations: {
    tr: string;
    en: string;
  };
  article: string | null;
  wordType: WordType;
  cefrLevel: CefrLevel;
  exampleSentences?: {
    de: string;
    tr: string;
    en: string;
  };
  audioUrl?: string;
  difficultyRating: number;
}

// Next Word Response
export interface NextWordResponse {
  word: Word;
  isReview: boolean;
  lastSeenAt: string | null;
  timesCorrect: number;
  timesIncorrect: number;
  currentInterval: number;
  sessionProgress: SessionProgress;
}

export interface SessionProgress {
  current: number;
  total: number;
  correctSoFar: number;
}

// Answer Submission
export interface SubmitAnswerRequest {
  sessionId: number;
  wordId: number;
  correct: boolean;
  quality: number; // 0-5 for SM-2
  responseTimeMs: number;
  userAnswer?: string;
}

export interface AnswerResponse {
  correct: boolean;
  eloChange: {
    userOldRating: number;
    userNewRating: number;
    userChange: number;
    wordOldRating: number;
    wordNewRating: number;
    wordChange: number;
  };
  sm2Update: {
    newEaseFactor: number;
    newInterval: number;
    newRepetition: number;
    nextReviewAt: string;
  };
  streakInfo: {
    currentStreak: number;
    isNewRecord: boolean;
    longestStreak: number;
  };
  sessionProgress: {
    wordsReviewed: number;
    correctAnswers: number;
    accuracy: number;
    remainingWords: number;
  };
  achievements: string[];
}

// Session Summary
export interface SessionSummary {
  summary: {
    duration: number;
    wordsReviewed: number;
    wordsNew: number;
    totalWords: number;
    correctAnswers: number;
    incorrectAnswers: number;
    accuracy: number;
    averageResponseTimeMs: number;
  };
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
  wordsToReviewTomorrow: number;
}

// Review Words
export interface ReviewWord {
  id: number;
  german: string;
  dueAt: string;
  overdueDays: number;
  lastReviewedAt: string;
}

export interface ReviewWordsResponse {
  totalDue: number;
  words: ReviewWord[];
  overdueCount: number;
  dueTodayCount: number;
}
