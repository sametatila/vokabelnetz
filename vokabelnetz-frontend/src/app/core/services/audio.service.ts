import { Injectable, signal } from '@angular/core';
import { CefrLevel } from '../models';

export type AudioStatus = 'idle' | 'loading' | 'playing' | 'error';

/**
 * AudioService - Manages pronunciation audio playback.
 *
 * 3-Tier Fallback Strategy (from docs/ARCHITECTURE.md):
 * 1. Static audio files (A1 level only - to prevent repo bloat)
 * 2. Web Speech API (Browser TTS for A2/B1+)
 * 3. IPA display (last resort)
 *
 * Based on docs/ARCHITECTURE.md Audio & Pronunciation section.
 */
@Injectable({ providedIn: 'root' })
export class AudioService {
  // Static files only for A1 to prevent repo bloat
  private readonly AUDIO_BASE_URL = '/assets/audio/a1';
  private speechSynthesis: SpeechSynthesis | null = null;
  private germanVoice: SpeechSynthesisVoice | null = null;

  // Reactive status signal
  readonly status = signal<AudioStatus>('idle');
  readonly hasGermanVoice = signal<boolean>(false);

  constructor() {
    this.initWebSpeechAPI();
  }

  /**
   * Initialize Web Speech API and find German voice.
   */
  private initWebSpeechAPI(): void {
    if ('speechSynthesis' in window) {
      this.speechSynthesis = window.speechSynthesis;

      const loadVoices = () => {
        const voices = this.speechSynthesis!.getVoices();
        // Prefer de-DE, then any German voice
        this.germanVoice = voices.find(v => v.lang === 'de-DE') ||
                          voices.find(v => v.lang.startsWith('de')) ||
                          null;
        this.hasGermanVoice.set(!!this.germanVoice);
      };

      // Voices may load asynchronously
      loadVoices();
      this.speechSynthesis.onvoiceschanged = loadVoices;
    }
  }

  /**
   * Play pronunciation for a word.
   * A1 words: Try static file first, then Web Speech API
   * A2/B1+ words: Web Speech API directly (no static files)
   */
  async playWord(word: string, cefrLevel: CefrLevel = 'A1'): Promise<void> {
    this.status.set('loading');

    // Only A1 has static files (to prevent repo bloat)
    if (cefrLevel === 'A1') {
      const staticUrl = `${this.AUDIO_BASE_URL}/${this.normalizeFilename(word)}.mp3`;

      try {
        await this.playAudioFile(staticUrl);
        return;
      } catch {
        // Fall through to Web Speech API
      }
    }

    // A2/B1 or A1 fallback: Use Web Speech API
    if (this.germanVoice) {
      this.speakWithTTS(word);
      return;
    }

    // No audio available
    this.status.set('error');
  }

  /**
   * Play audio from a URL.
   */
  private playAudioFile(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const audio = new Audio(url);

      audio.onplay = () => this.status.set('playing');
      audio.onended = () => {
        this.status.set('idle');
        resolve();
      };
      audio.onerror = () => {
        this.status.set('error');
        reject(new Error('Audio file not found'));
      };

      audio.play().catch(reject);
    });
  }

  /**
   * Speak text using Web Speech API (TTS).
   */
  private speakWithTTS(text: string): void {
    if (!this.speechSynthesis || !this.germanVoice) {
      this.status.set('error');
      return;
    }

    // Cancel any ongoing speech
    this.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.voice = this.germanVoice;
    utterance.lang = 'de-DE';
    utterance.rate = 0.9; // Slightly slower for learning

    utterance.onstart = () => this.status.set('playing');
    utterance.onend = () => this.status.set('idle');
    utterance.onerror = () => this.status.set('error');

    this.speechSynthesis.speak(utterance);
  }

  /**
   * Stop any ongoing audio playback.
   */
  stop(): void {
    if (this.speechSynthesis) {
      this.speechSynthesis.cancel();
    }
    this.status.set('idle');
  }

  /**
   * Normalize word for filename (lowercase, remove umlauts for filename).
   */
  private normalizeFilename(word: string): string {
    return word
      .toLowerCase()
      .replace(/ä/g, 'ae')
      .replace(/ö/g, 'oe')
      .replace(/ü/g, 'ue')
      .replace(/ß/g, 'ss')
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9-]/g, '');
  }
}
