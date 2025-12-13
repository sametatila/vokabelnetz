import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, UserProfile, DailyStats, StreakStatus } from '../models';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  /**
   * Get current user profile.
   */
  getUserProfile(): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.apiUrl}/users/me`);
  }

  /**
   * Get today's statistics.
   */
  getDailyStats(): Observable<ApiResponse<DailyStats>> {
    return this.http.get<ApiResponse<DailyStats>>(`${this.apiUrl}/progress/daily`);
  }

  /**
   * Get streak status.
   */
  getStreakStatus(): Observable<ApiResponse<StreakStatus>> {
    return this.http.get<ApiResponse<StreakStatus>>(`${this.apiUrl}/progress/streak`);
  }

  /**
   * Get weekly statistics.
   */
  getWeeklyStats(): Observable<ApiResponse<DailyStats[]>> {
    return this.http.get<ApiResponse<DailyStats[]>>(`${this.apiUrl}/progress/weekly`);
  }
}
