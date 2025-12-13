import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, CefrLevel } from '../models';
import { environment } from '../../../environments/environment';

// Overall Progress
export interface OverallProgress {
  overview: {
    totalWordsLearned: number;
    totalWordsInProgress: number;
    totalReviews: number;
    correctAnswers: number;
    incorrectAnswers: number;
    overallAccuracy: number;
    totalTimeSpentMinutes: number;
    averageSessionMinutes: number;
  };
  elo: {
    currentRating: number;
    highestRating: number;
    change7Days: number;
    change30Days: number;
  };
  streak: {
    currentStreak: number;
    longestStreak: number;
    lastActiveDate: string;
  };
  levelProgress: Record<CefrLevel, {
    total: number;
    learned: number;
    inProgress: number;
    percentage: number;
  }>;
  achievements: {
    total: number;
    recent: Achievement[];
  };
  todayStats: {
    wordsLearned: number;
    wordsReviewed: number;
    correctAnswers: number;
    timeSpentMinutes: number;
    goalProgress: number;
  };
}

// Achievement
export interface Achievement {
  type: string;
  name: string;
  description: string;
  icon: string;
  earnedAt?: string;
  progress?: {
    current: number;
    target: number;
    percentage: number;
  };
}

export interface AchievementsResponse {
  earned: Achievement[];
  available: Achievement[];
  totalEarned: number;
  totalAvailable: number;
}

// Streak
export interface StreakInfo {
  currentStreak: number;
  longestStreak: number;
  streakStartDate: string | null;
  lastActiveDate: string | null;
  todayCompleted: boolean;
  atRisk: boolean;
  freezesAvailable: number;
  streakHistory: Array<{
    date: string;
    completed: boolean;
    wordsLearned: number;
  }>;
  milestones: {
    nextMilestone: number;
    daysUntilNext: number;
    achievedMilestones: number[];
  };
}

// Activity Heatmap
export interface ActivityData {
  year: number;
  activities: Array<{
    date: string;
    count: number;
    level: number;
  }>;
  totalActiveDays: number;
  maxDailyCount: number;
  legend: Record<string, string>;
}

// Weekly/Monthly Stats
export interface PeriodStats {
  period: string;
  wordsLearned: number;
  wordsReviewed: number;
  correctAnswers: number;
  incorrectAnswers: number;
  accuracy: number;
  timeSpentMinutes: number;
  sessionsCompleted: number;
}

/**
 * ProgressService - Handles progress and statistics API calls.
 *
 * Based on docs/API.md Progress Endpoints.
 */
@Injectable({ providedIn: 'root' })
export class ProgressService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Get overall statistics.
   */
  getOverallProgress(): Observable<ApiResponse<OverallProgress>> {
    return this.http.get<ApiResponse<OverallProgress>>(`${this.apiUrl}/progress/overall`);
  }

  /**
   * Get today's statistics.
   */
  getDailyStats(): Observable<ApiResponse<PeriodStats>> {
    return this.http.get<ApiResponse<PeriodStats>>(`${this.apiUrl}/progress/daily`);
  }

  /**
   * Get weekly statistics.
   */
  getWeeklyStats(): Observable<ApiResponse<PeriodStats[]>> {
    return this.http.get<ApiResponse<PeriodStats[]>>(`${this.apiUrl}/progress/weekly`);
  }

  /**
   * Get monthly statistics.
   */
  getMonthlyStats(): Observable<ApiResponse<PeriodStats[]>> {
    return this.http.get<ApiResponse<PeriodStats[]>>(`${this.apiUrl}/progress/monthly`);
  }

  /**
   * Get streak information.
   */
  getStreak(): Observable<ApiResponse<StreakInfo>> {
    return this.http.get<ApiResponse<StreakInfo>>(`${this.apiUrl}/progress/streak`);
  }

  /**
   * Get all achievements.
   */
  getAchievements(): Observable<ApiResponse<AchievementsResponse>> {
    return this.http.get<ApiResponse<AchievementsResponse>>(`${this.apiUrl}/progress/achievements`);
  }

  /**
   * Get word-specific progress.
   */
  getWordProgress(wordId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/progress/words/${wordId}`);
  }

  /**
   * Get level progress.
   */
  getLevelProgress(level: CefrLevel): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/progress/level/${level}`);
  }

  /**
   * Get activity heatmap data.
   */
  getActivityHeatmap(year?: number): Observable<ApiResponse<ActivityData>> {
    let params = new HttpParams();
    if (year) {
      params = params.set('year', year.toString());
    }
    return this.http.get<ApiResponse<ActivityData>>(`${this.apiUrl}/progress/charts/activity`, { params });
  }

  /**
   * Get accuracy chart data.
   */
  getAccuracyChart(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/progress/charts/accuracy`);
  }
}
