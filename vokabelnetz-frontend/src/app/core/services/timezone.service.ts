import { Injectable } from '@angular/core';

/**
 * TimezoneService - Handles timezone detection and date calculations.
 *
 * Used for:
 * - Detecting user's system timezone
 * - Calculating streak-related dates in user's local time
 * - Converting between UTC and local time
 *
 * Based on docs/ARCHITECTURE.md Timezone Service.
 */
@Injectable({ providedIn: 'root' })
export class TimezoneService {
  private static readonly DEFAULT_TIMEZONE = 'Europe/Istanbul'; // UTC+2/+3

  /**
   * Detect user's system timezone.
   * Falls back to Europe/Istanbul if detection fails.
   */
  detectTimezone(): string {
    try {
      const detected = Intl.DateTimeFormat().resolvedOptions().timeZone;

      if (detected && this.isValidTimezone(detected)) {
        return detected;
      }
    } catch (e) {
      console.warn('Timezone detection failed:', e);
    }

    return TimezoneService.DEFAULT_TIMEZONE;
  }

  /**
   * Check if a timezone string is valid.
   */
  isValidTimezone(tz: string): boolean {
    try {
      Intl.DateTimeFormat(undefined, { timeZone: tz });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get current date in user's timezone.
   */
  getCurrentDate(timezone?: string): Date {
    const tz = timezone || this.detectTimezone();
    return new Date(new Date().toLocaleString('en-US', { timeZone: tz }));
  }

  /**
   * Get current date string (YYYY-MM-DD) in user's timezone.
   */
  getCurrentDateString(timezone?: string): string {
    const date = this.getCurrentDate(timezone);
    return date.toISOString().split('T')[0];
  }

  /**
   * Check if it's a new day in user's timezone.
   */
  isNewDay(lastActiveAt: Date, timezone?: string): boolean {
    const tz = timezone || this.detectTimezone();
    const now = this.getCurrentDate(tz);
    const last = new Date(lastActiveAt.toLocaleString('en-US', { timeZone: tz }));

    return now.toDateString() !== last.toDateString();
  }

  /**
   * Get start of day in user's timezone.
   */
  getStartOfDay(date?: Date, timezone?: string): Date {
    const tz = timezone || this.detectTimezone();
    const d = date ? new Date(date.toLocaleString('en-US', { timeZone: tz })) : this.getCurrentDate(tz);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  /**
   * Get end of day in user's timezone.
   */
  getEndOfDay(date?: Date, timezone?: string): Date {
    const tz = timezone || this.detectTimezone();
    const d = date ? new Date(date.toLocaleString('en-US', { timeZone: tz })) : this.getCurrentDate(tz);
    d.setHours(23, 59, 59, 999);
    return d;
  }

  /**
   * Format date for display based on locale.
   */
  formatDate(date: Date, locale: string = 'en', timezone?: string): string {
    const tz = timezone || this.detectTimezone();
    return date.toLocaleDateString(locale, {
      timeZone: tz,
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  /**
   * Format time for display based on locale.
   */
  formatTime(date: Date, locale: string = 'en', timezone?: string): string {
    const tz = timezone || this.detectTimezone();
    return date.toLocaleTimeString(locale, {
      timeZone: tz,
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
