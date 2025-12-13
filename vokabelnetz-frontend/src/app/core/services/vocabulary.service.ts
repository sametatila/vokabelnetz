import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Word, CefrLevel } from '../models';

export interface WordsResponse {
  data: Word[];
  meta: {
    page: number;
    size: number;
    total: number;
  };
}

export interface WordStats {
  totalWords: number;
  activeWords: number;
  byLevel: Record<CefrLevel, number>;
}

/**
 * VocabularyService - Handles word-related API calls.
 *
 * Based on docs/API.md Word Endpoints.
 */
@Injectable({ providedIn: 'root' })
export class VocabularyService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  /**
   * Get all words (paginated).
   */
  getWords(page = 0, size = 20, sort = 'german'): Observable<ApiResponse<Word[]> & { meta: WordsResponse['meta'] }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<ApiResponse<Word[]> & { meta: WordsResponse['meta'] }>(
      `${this.apiUrl}/words`,
      { params }
    );
  }

  /**
   * Get word by ID.
   */
  getWord(id: number): Observable<ApiResponse<Word>> {
    return this.http.get<ApiResponse<Word>>(`${this.apiUrl}/words/${id}`);
  }

  /**
   * Get words by CEFR level.
   */
  getWordsByLevel(level: CefrLevel, page = 0, size = 20): Observable<ApiResponse<Word[]> & { meta: WordsResponse['meta'] }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<Word[]> & { meta: WordsResponse['meta'] }>(
      `${this.apiUrl}/words/level/${level}`,
      { params }
    );
  }

  /**
   * Search words by German term.
   */
  searchWords(query: string, page = 0, size = 20): Observable<ApiResponse<Word[]> & { meta: WordsResponse['meta'] }> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<Word[]> & { meta: WordsResponse['meta'] }>(
      `${this.apiUrl}/words/search`,
      { params }
    );
  }

  /**
   * Get random word.
   */
  getRandomWord(level?: CefrLevel): Observable<ApiResponse<Word>> {
    let params = new HttpParams();
    if (level) {
      params = params.set('level', level);
    }
    return this.http.get<ApiResponse<Word>>(`${this.apiUrl}/words/random`, { params });
  }

  /**
   * Get word statistics.
   */
  getWordStats(): Observable<ApiResponse<WordStats>> {
    return this.http.get<ApiResponse<WordStats>>(`${this.apiUrl}/words/stats`);
  }
}
