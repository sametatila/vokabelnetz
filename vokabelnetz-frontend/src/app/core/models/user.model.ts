export interface User {
  id: number;
  email: string;
  displayName?: string;
  avatarUrl?: string;
  eloRating: number;
  currentStreak: number;
  longestStreak: number;
  totalWordsLearned: number;
  dailyGoal: number;
  uiLanguage: Language;
  sourceLanguage: Language;
  timezone: string;
  isActive: boolean;
  emailVerified: boolean;
  lastActiveAt?: string;
  createdAt: string;
}

export interface UserPreferences {
  uiLanguage: Language;
  sourceLanguage: Language;
  showBothTranslations: boolean;
  primaryTranslation: Language;
  dailyWordGoal: number;
  sessionDurationMin: number;
  newWordsPerSession: number;
  notificationEnabled: boolean;
  notificationTime?: string;
  soundEnabled: boolean;
  darkMode: boolean;
  showPronunciation: boolean;
  autoPlayAudio: boolean;
  showExampleSentences: boolean;
  showWordType: boolean;
  showArticleHints: boolean;
  streakReminders: boolean;
  weeklyReports: boolean;
}

export type Language = 'tr' | 'en' | 'de';

export type CefrLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2';
