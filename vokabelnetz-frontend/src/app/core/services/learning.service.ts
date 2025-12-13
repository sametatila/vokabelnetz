import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  StartSessionRequest,
  LearningSession,
  NextWordResponse,
  SubmitAnswerRequest,
  AnswerResponse,
  SessionSummary,
  ReviewWordsResponse
} from '../models';

/**
 * LearningService - Handles learning session API calls.
 *
 * Based on docs/API.md Learning Endpoints.
 */
@Injectable({ providedIn: 'root' })
export class LearningService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  /**
   * Start a new learning session.
   */
  startSession(request: StartSessionRequest): Observable<ApiResponse<LearningSession>> {
    return this.http.post<ApiResponse<LearningSession>>(
      `${this.apiUrl}/learning/session/start`,
      request
    );
  }

  /**
   * Get current active session.
   */
  getCurrentSession(): Observable<ApiResponse<LearningSession>> {
    return this.http.get<ApiResponse<LearningSession>>(
      `${this.apiUrl}/learning/session/current`
    );
  }

  /**
   * End the current learning session.
   */
  endSession(sessionId: number): Observable<ApiResponse<SessionSummary>> {
    return this.http.post<ApiResponse<SessionSummary>>(
      `${this.apiUrl}/learning/session/end`,
      { sessionId }
    );
  }

  /**
   * Get next word in the session.
   */
  getNextWord(sessionId: number): Observable<ApiResponse<NextWordResponse>> {
    return this.http.get<ApiResponse<NextWordResponse>>(
      `${this.apiUrl}/learning/next`,
      { params: { sessionId: sessionId.toString() } }
    );
  }

  /**
   * Submit an answer for a word.
   */
  submitAnswer(request: SubmitAnswerRequest): Observable<ApiResponse<AnswerResponse>> {
    return this.http.post<ApiResponse<AnswerResponse>>(
      `${this.apiUrl}/learning/answer`,
      request
    );
  }

  /**
   * Get words due for review.
   */
  getReviewWords(limit: number = 20): Observable<ApiResponse<ReviewWordsResponse>> {
    return this.http.get<ApiResponse<ReviewWordsResponse>>(
      `${this.apiUrl}/learning/review`,
      { params: { limit: limit.toString() } }
    );
  }

  /**
   * Get count of words due for review.
   */
  getReviewCount(): Observable<ApiResponse<{ count: number }>> {
    return this.http.get<ApiResponse<{ count: number }>>(
      `${this.apiUrl}/learning/review/count`
    );
  }
}
