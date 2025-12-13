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
  uiLanguage?: string;
  sourceLanguage?: string;
  timezone?: string;
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

// User Role (per DATABASE.md user_role ENUM)
export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_SUPER';

// Achievement Type (per DATABASE.md achievement_type ENUM)
export type AchievementType =
  | 'FIRST_WORD'      // First word learned
  | 'WORDS_10'        // 10 words learned
  | 'WORDS_50'        // 50 words learned
  | 'WORDS_100'       // 100 words learned
  | 'WORDS_250'       // 250 words learned
  | 'WORDS_500'       // 500 words learned
  | 'WORDS_1000'      // 1000 words learned
  | 'STREAK_3'        // 3 day streak
  | 'STREAK_7'        // 7 day streak
  | 'STREAK_14'       // 14 day streak
  | 'STREAK_30'       // 30 day streak
  | 'STREAK_100'      // 100 day streak
  | 'PERFECT_SESSION' // 100% accuracy in a session
  | 'ACCURACY_90'     // 90% overall accuracy
  | 'COMPLETE_A1'     // Completed A1 level
  | 'COMPLETE_A2'     // Completed A2 level
  | 'COMPLETE_B1'     // Completed B1 level
  | 'EARLY_BIRD'      // Practiced before 8 AM
  | 'NIGHT_OWL'       // Practiced after 10 PM
  | 'COMEBACK';       // Returned after 7+ days

// User Achievement (per DATABASE.md JSONB storage format)
// Stored as JSON array in users.achievements column
export interface UserAchievement {
  type: AchievementType;
  earnedAt: string;
}

// User Profile (per DATABASE.md users table)
export interface UserProfile {
  id: number;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  role: UserRole;
  eloRating: number;
  currentStreak: number;
  longestStreak: number;
  streakFreezesAvailable: number;
  totalWordsLearned: number;
  dailyGoal: number;
  uiLanguage: string;
  sourceLanguage: string;
  timezone: string;
  isActive: boolean;
  emailVerified: boolean;
  lastActiveAt: string;
  createdAt: string;
  achievements?: UserAchievement[];
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

// Learning Types (per DATABASE.md session_type ENUM)
export type SessionType = 'LEARN' | 'REVIEW' | 'QUIZ' | 'MIXED';
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

// Word Category (per DATABASE.md word_category ENUM)
export type WordCategory =
  | 'ALLTAG'           // Daily life
  | 'ARBEIT_BERUF'     // Work & career
  | 'BILDUNG'          // Education
  | 'ESSEN_TRINKEN'    // Food & drink
  | 'FAMILIE_FREUNDE'  // Family & friends
  | 'FREIZEIT'         // Leisure
  | 'GESUNDHEIT'       // Health
  | 'REISEN_VERKEHR'   // Travel & transport
  | 'WOHNEN'           // Home & living
  | 'ANDERE';          // Other

// Word (per DATABASE.md words table)
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
  phoneticSpelling?: string;
  pluralForm?: string;
  category?: WordCategory;
  tags?: string;
  difficultyRating: number;
  // Global statistics
  timesShown?: number;
  timesCorrect?: number;
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

// Session Management (per API.md Auth Endpoints)
export interface SessionInfo {
  id: number;
  deviceInfo: string;
  ipAddress: string;
  createdAt: string;
  lastUsedAt: string;
  isCurrent: boolean;
}

export interface SessionsResponse {
  sessions: SessionInfo[];
  totalSessions: number;
}
