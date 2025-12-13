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
