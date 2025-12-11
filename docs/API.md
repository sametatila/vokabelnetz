# 📚 API Documentation

Complete REST API reference for Vokabelnetz backend.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Auth Endpoints](#auth-endpoints)
- [User Endpoints](#user-endpoints)
- [Word Endpoints](#word-endpoints)
- [Learning Endpoints](#learning-endpoints)
- [Progress Endpoints](#progress-endpoints)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)

---

## Overview

### Base URL

```
Development: http://localhost:8080/api
Production:  https://api.vokabelnetz.com/api
```

### Response Format

All responses follow this structure:

```json
{
  "success": true,
  "message": "Optional message",
  "data": { ... },
  "timestamp": "2025-01-09T14:30:00Z"
}
```

### Content Type

```
Content-Type: application/json
Accept: application/json
```

---

## Authentication

### JWT Token Authentication

All endpoints except `/api/auth/**` require JWT authentication.

```
Authorization: Bearer <access_token>
```

### Token Types

| Token | Expiry | Purpose |
|-------|--------|---------|
| Access Token | 15 minutes | API authentication |
| Refresh Token | 7 days | Obtain new access token |

---

## Auth Endpoints

### Endpoints Overview

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Register new user | ❌ |
| POST | `/api/auth/login` | User login | ❌ |
| POST | `/api/auth/refresh` | Refresh tokens (with rotation) | 🍪 |
| POST | `/api/auth/logout` | Logout current session | 🍪 |
| POST | `/api/auth/logout-all` | Logout from all devices | ✅ |
| POST | `/api/auth/forgot-password` | Request password reset | ❌ |
| POST | `/api/auth/reset-password` | Reset password with token | ❌ |
| GET | `/api/auth/verify-email` | Verify email address | ❌ |
| GET | `/api/auth/sessions` | List active sessions | ✅ |
| DELETE | `/api/auth/sessions/{id}` | Revoke specific session | ✅ |

> **Auth Legend:** ✅ = Bearer token required, 🍪 = HttpOnly cookie required, ❌ = No auth
> 
> **Security Note:** See [SECURITY.md](SECURITY.md) for detailed token management, rotation, and storage strategies.

### Register

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "displayName": "Ahmet Yılmaz",
  "uiLanguage": "tr",
  "sourceLanguage": "tr",
  "timezone": "Europe/Istanbul"
}
```

> **Note:** `uiLanguage`, `sourceLanguage`, and `timezone` are optional.
> - Language defaults: Determined by frontend from system language. Falls back to `en` if system language is not TR or EN.
> - Timezone default: `Europe/Istanbul` (UTC+2/+3) if system timezone is unavailable.

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful. Please verify your email.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "displayName": "Ahmet Yılmaz",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "displayName": "Ahmet Yılmaz",
    "eloRating": 1150,
    "currentStreak": 7,
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Response Headers:**
```
Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

> **Token Storage:** Access token is returned in response body (store in memory only). Refresh token is set as HttpOnly cookie (cannot be accessed by JavaScript). See [SECURITY.md](SECURITY.md) for details.

**Error Response (401 Unauthorized):**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password"
  }
}
```

> **Security Note:** Error message is intentionally vague to prevent user enumeration attacks.

### Refresh Token

Refreshes access token using the HttpOnly cookie. **Implements token rotation** - old refresh token is invalidated and new one is issued.

```http
POST /api/auth/refresh
Cookie: refresh_token=<current_refresh_token>
```

> **No request body needed** - refresh token is read from HttpOnly cookie.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Response Headers:**
```
Set-Cookie: refresh_token=<NEW_token>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

> **Token Rotation:** Each refresh request invalidates the old token and issues a new one. If a revoked token is reused (potential theft), ALL user sessions are terminated.

### Logout

Revokes the current refresh token.

```http
POST /api/auth/logout
Cookie: refresh_token=<refresh_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

**Response Headers:**
```
Set-Cookie: refresh_token=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0
```

### Logout from All Devices

Revokes ALL refresh tokens for the user.

```http
POST /api/auth/logout-all
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out from all devices",
  "data": {
    "sessionsRevoked": 3
  }
}
```

### List Active Sessions

```http
GET /api/auth/sessions
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "sessions": [
      {
        "id": 1,
        "deviceInfo": "Chrome on Windows",
        "ipAddress": "192.168.1.***",
        "createdAt": "2025-01-09T10:00:00Z",
        "lastUsedAt": "2025-01-09T14:30:00Z",
        "isCurrent": true
      },
      {
        "id": 2,
        "deviceInfo": "Safari on iPhone",
        "ipAddress": "10.0.0.***",
        "createdAt": "2025-01-08T09:00:00Z",
        "lastUsedAt": "2025-01-08T18:00:00Z",
        "isCurrent": false
      }
    ],
    "totalSessions": 2
  }
}
```

### Revoke Session

```http
DELETE /api/auth/sessions/2
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Session revoked successfully"
}
```

---

## User Endpoints

### Endpoints Overview

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/users/me` | Get current user profile | ✅ |
| PUT | `/api/users/me` | Update user profile | ✅ |
| PUT | `/api/users/me/password` | Change password | ✅ |
| DELETE | `/api/users/me` | Delete account (soft delete) | ✅ |
| GET | `/api/users/me/preferences` | Get user preferences | ✅ |
| PUT | `/api/users/me/preferences` | Update user preferences | ✅ |
| GET | `/api/users/me/language` | Get language settings | ✅ |
| PUT | `/api/users/me/language` | Update language settings | ✅ |
| PATCH | `/api/users/me/language/source` | Quick switch source language | ✅ |

### Delete Account (Soft Delete)

Initiates account deletion. User data is retained for 30 days (GDPR grace period) before permanent deletion.

```http
DELETE /api/users/me
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "reason": "No longer learning German",
  "confirmEmail": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Account scheduled for deletion",
  "data": {
    "deletedAt": "2025-01-09T14:30:00Z",
    "permanentDeletionAt": "2025-02-08T14:30:00Z",
    "canRecover": true,
    "recoveryDeadline": "2025-02-08T14:30:00Z"
  }
}
```

> **Note:** User can recover their account by logging in before `permanentDeletionAt`. After 30 days, all data is permanently deleted via scheduled cleanup job.

### Get Current User

```http
GET /api/users/me
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "Ahmet Yılmaz",
    "avatarUrl": null,
    "eloRating": 1150,
    "currentStreak": 7,
    "longestStreak": 14,
    "totalWordsLearned": 245,
    "dailyGoal": 20,
    "uiLanguage": "tr",
    "sourceLanguage": "tr",
    "timezone": "Europe/Istanbul",
    "isActive": true,
    "emailVerified": true,
    "lastActiveAt": "2025-01-09T10:30:00Z",
    "createdAt": "2024-06-15T08:00:00Z"
  }
}
```

### Update Preferences

```http
PUT /api/users/me/preferences
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "uiLanguage": "tr",
  "sourceLanguage": "tr",
  "showBothTranslations": false,
  "primaryTranslation": "tr",
  "dailyWordGoal": 30,
  "sessionDurationMin": 20,
  "newWordsPerSession": 5,
  "notificationEnabled": true,
  "notificationTime": "09:00:00",
  "soundEnabled": true,
  "darkMode": false,
  "showPronunciation": true,
  "autoPlayAudio": false,
  "showExampleSentences": true,
  "showWordType": true,
  "showArticleHints": true
}
```

### Get Language Settings

```http
GET /api/users/me/language
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "uiLanguage": "tr",
    "sourceLanguage": "tr",
    "targetLanguage": "de",
    "showBothTranslations": false,
    "primaryTranslation": "tr"
  }
}
```

### Quick Switch Source Language

```http
PATCH /api/users/me/language/source
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "sourceLanguage": "en"
}
```

---

## Word Endpoints

### Endpoints Overview

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/words` | Get all words (paginated) | ✅ |
| GET | `/api/words/{id}` | Get word by ID | ✅ |
| GET | `/api/words/level/{cefr}` | Get words by CEFR level | ✅ |
| GET | `/api/words/category/{category}` | Get words by category | ✅ |
| GET | `/api/words/search` | Search words | ✅ |
| GET | `/api/words/random` | Get random word | ✅ |
| GET | `/api/words/stats` | Get word statistics | ✅ |

### Get Words (Paginated)

```http
GET /api/words?page=0&size=20&level=A1&category=ALLTAG&sort=difficulty,asc
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "german": "arbeiten",
        "turkish": "çalışmak",
        "english": "to work",
        "article": null,
        "wordType": "VERB",
        "cefrLevel": "A1",
        "exampleSentenceDe": "Ich arbeite in einem Büro.",
        "exampleSentenceTr": "Bir ofiste çalışıyorum.",
        "exampleSentenceEn": "I work in an office.",
        "pronunciationUrl": "/assets/audio/arbeiten.mp3",
        "phoneticSpelling": "ˈaʁbaɪ̯tn̩",
        "difficultyRating": 920,
        "category": "ARBEIT_BERUF"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 650,
    "totalPages": 33,
    "first": true,
    "last": false
  }
}
```

### Get Word by ID

```http
GET /api/words/1
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "german": "das Haus",
    "turkish": "ev",
    "english": "house",
    "article": "das",
    "wordType": "NOUN",
    "pluralForm": "Häuser",
    "cefrLevel": "A1",
    "exampleSentenceDe": "Wir wohnen in einem großen Haus.",
    "exampleSentenceTr": "Büyük bir evde yaşıyoruz.",
    "exampleSentenceEn": "We live in a big house.",
    "pronunciationUrl": "/assets/audio/haus.mp3",
    "phoneticSpelling": "haʊ̯s",
    "difficultyRating": 850,
    "category": "WOHNEN",
    "timesShown": 15420,
    "timesCorrect": 12850
  }
}
```

### Search Words

Supports fuzzy search using PostgreSQL's `pg_trgm` extension. Handles typos and partial matches.

```http
GET /api/words/search?q=Hau&lang=german&fuzzy=true
Authorization: Bearer <access_token>
```

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `q` | string | required | Search query |
| `lang` | string | `german` | Field to search: `german`, `turkish`, `english` |
| `fuzzy` | boolean | `true` | Enable fuzzy matching (tolerates typos) |
| `threshold` | float | `0.3` | Similarity threshold for fuzzy search (0.0-1.0) |
| `limit` | int | `20` | Maximum results |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "query": "Hau",
    "fuzzy": true,
    "results": [
      {
        "id": 42,
        "german": "Haus",
        "turkish": "ev",
        "english": "house",
        "similarity": 0.75,
        "cefrLevel": "A1"
      },
      {
        "id": 156,
        "german": "Haupt",
        "turkish": "baş, ana",
        "english": "main, head",
        "similarity": 0.60,
        "cefrLevel": "A2"
      }
    ],
    "totalResults": 2
  }
}
```

---

## Learning Endpoints

### Endpoints Overview

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/learning/session/start` | Start learning session | ✅ |
| GET | `/api/learning/session/current` | Get current session | ✅ |
| POST | `/api/learning/session/end` | End learning session | ✅ |
| GET | `/api/learning/next` | Get next word | ✅ |
| POST | `/api/learning/answer` | Submit answer | ✅ |
| GET | `/api/learning/review` | Get words due for review | ✅ |
| GET | `/api/learning/review/count` | Get review count | ✅ |
| GET | `/api/learning/new` | Get new words | ✅ |
| GET | `/api/learning/quiz` | Get quiz words | ✅ |

### Start Session

```http
POST /api/learning/session/start
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "sessionType": "MIXED",
  "cefrLevel": "A1",
  "wordCount": 20
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "sessionId": 1234,
    "sessionType": "MIXED",
    "cefrLevel": "A1",
    "wordsToReview": 8,
    "newWordsAvailable": 12,
    "startedAt": "2025-01-09T14:30:00Z"
  }
}
```

### Get Next Word

```http
GET /api/learning/next?sessionId=1234
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "word": {
      "id": 42,
      "german": "verstehen",
      "turkish": "anlamak",
      "english": "to understand",
      "article": null,
      "wordType": "VERB",
      "cefrLevel": "A1",
      "exampleSentenceDe": "Ich verstehe dich nicht.",
      "exampleSentenceTr": "Seni anlamıyorum.",
      "exampleSentenceEn": "I don't understand you.",
      "pronunciationUrl": "/assets/audio/verstehen.mp3",
      "difficultyRating": 1050
    },
    "isReview": true,
    "lastSeenAt": "2025-01-07T10:15:00Z",
    "timesCorrect": 3,
    "timesIncorrect": 1,
    "currentInterval": 6,
    "sessionProgress": {
      "current": 5,
      "total": 20,
      "correctSoFar": 4
    }
  }
}
```

### Submit Answer

```http
POST /api/learning/answer
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "sessionId": 1234,
  "wordId": 42,
  "correct": true,
  "quality": 4,
  "responseTimeMs": 2350,
  "userAnswer": "anlamak"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "correct": true,
    "eloChange": {
      "userOldRating": 1150,
      "userNewRating": 1164,
      "userChange": 14,
      "wordOldRating": 1050,
      "wordNewRating": 1036,
      "wordChange": -14
    },
    "sm2Update": {
      "newEaseFactor": 2.5,
      "newInterval": 15,
      "newRepetition": 4,
      "nextReviewAt": "2025-01-24T00:00:00Z"
    },
    "streakInfo": {
      "currentStreak": 7,
      "isNewRecord": false,
      "longestStreak": 14
    },
    "sessionProgress": {
      "wordsReviewed": 6,
      "correctAnswers": 5,
      "accuracy": 83.33,
      "remainingWords": 14
    },
    "achievements": []
  }
}
```

### End Session

```http
POST /api/learning/session/end
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "sessionId": 1234
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "summary": {
      "duration": 845,
      "wordsReviewed": 15,
      "wordsNew": 5,
      "totalWords": 20,
      "correctAnswers": 17,
      "incorrectAnswers": 3,
      "accuracy": 85.0,
      "averageResponseTimeMs": 2150
    },
    "eloChange": {
      "startRating": 1150,
      "endRating": 1175,
      "change": 25
    },
    "streakMaintained": true,
    "dailyGoalProgress": {
      "current": 20,
      "goal": 20,
      "completed": true
    },
    "wordsToReviewTomorrow": 12
  }
}
```

### Get Review Words

```http
GET /api/learning/review?limit=20
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "totalDue": 23,
    "words": [
      {
        "id": 42,
        "german": "verstehen",
        "dueAt": "2025-01-09T00:00:00Z",
        "overdueDays": 0,
        "lastReviewedAt": "2025-01-07T10:15:00Z"
      }
    ],
    "overdueCount": 5,
    "dueTodayCount": 18
  }
}
```

---

## Progress Endpoints

### Endpoints Overview

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/progress/overall` | Get overall statistics | ✅ |
| GET | `/api/progress/daily` | Get today's statistics | ✅ |
| GET | `/api/progress/weekly` | Get weekly statistics | ✅ |
| GET | `/api/progress/monthly` | Get monthly statistics | ✅ |
| GET | `/api/progress/streak` | Get streak information | ✅ |
| GET | `/api/progress/achievements` | Get all achievements | ✅ |
| GET | `/api/progress/words/{wordId}` | Get word-specific progress | ✅ |
| GET | `/api/progress/level/{cefr}` | Get level progress | ✅ |
| GET | `/api/progress/charts/accuracy` | Get accuracy chart data | ✅ |
| GET | `/api/progress/charts/activity` | Get activity heatmap data | ✅ |

### Get Overall Statistics

```http
GET /api/progress/overall
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "overview": {
      "totalWordsLearned": 245,
      "totalWordsInProgress": 89,
      "totalReviews": 3420,
      "correctAnswers": 2890,
      "incorrectAnswers": 530,
      "overallAccuracy": 84.5,
      "totalTimeSpentMinutes": 2150,
      "averageSessionMinutes": 18
    },
    "elo": {
      "currentRating": 1175,
      "highestRating": 1195,
      "change7Days": 45,
      "change30Days": 125
    },
    "streak": {
      "currentStreak": 7,
      "longestStreak": 14,
      "lastActiveDate": "2025-01-09"
    },
    "levelProgress": {
      "A1": {
        "total": 650,
        "learned": 180,
        "inProgress": 45,
        "percentage": 27.7
      },
      "A2": {
        "total": 1300,
        "learned": 55,
        "inProgress": 35,
        "percentage": 4.2
      },
      "B1": {
        "total": 2400,
        "learned": 10,
        "inProgress": 9,
        "percentage": 0.4
      }
    },
    "achievements": {
      "total": 8,
      "recent": [
        {
          "type": "WORDS_100",
          "name": "Century",
          "description": "Learn 100 words",
          "icon": "💯",
          "earnedAt": "2025-01-08T14:30:00Z"
        }
      ]
    },
    "todayStats": {
      "wordsLearned": 5,
      "wordsReviewed": 15,
      "correctAnswers": 17,
      "timeSpentMinutes": 18,
      "goalProgress": 100
    }
  }
}
```

### Get Achievements

```http
GET /api/progress/achievements
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "earned": [
      {
        "type": "FIRST_WORD",
        "name": "First Word",
        "description": "Learn your first word",
        "icon": "🎉",
        "earnedAt": "2024-06-15T10:30:00Z"
      },
      {
        "type": "WORDS_100",
        "name": "Century",
        "description": "Learn 100 words",
        "icon": "💯",
        "earnedAt": "2025-01-08T14:30:00Z"
      },
      {
        "type": "STREAK_7",
        "name": "Week Warrior",
        "description": "Maintain a 7-day streak",
        "icon": "📅",
        "earnedAt": "2025-01-07T23:59:00Z"
      }
    ],
    "available": [
      {
        "type": "WORDS_250",
        "name": "Vocabulary Builder",
        "description": "Learn 250 words",
        "icon": "🏗️",
        "progress": {
          "current": 245,
          "target": 250,
          "percentage": 98
        }
      },
      {
        "type": "STREAK_14",
        "name": "Fortnight Fighter",
        "description": "Maintain a 14-day streak",
        "icon": "⚔️",
        "progress": {
          "current": 7,
          "target": 14,
          "percentage": 50
        }
      }
    ],
    "totalEarned": 8,
    "totalAvailable": 18
  }
}
```

### Get Streak Information

```http
GET /api/progress/streak
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "currentStreak": 7,
    "longestStreak": 14,
    "streakStartDate": "2025-01-03",
    "lastActiveDate": "2025-01-09",
    "todayCompleted": true,
    "atRisk": false,
    "freezesAvailable": 2,
    "streakHistory": [
      { "date": "2025-01-09", "completed": true, "wordsLearned": 20 },
      { "date": "2025-01-08", "completed": true, "wordsLearned": 25 },
      { "date": "2025-01-07", "completed": true, "wordsLearned": 18 }
    ],
    "milestones": {
      "nextMilestone": 14,
      "daysUntilNext": 7,
      "achievedMilestones": [7]
    }
  }
}
```

### Get Activity Heatmap

```http
GET /api/progress/charts/activity?year=2025
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "year": 2025,
    "activities": [
      { "date": "2025-01-01", "count": 15, "level": 2 },
      { "date": "2025-01-02", "count": 25, "level": 3 },
      { "date": "2025-01-03", "count": 0, "level": 0 }
    ],
    "totalActiveDays": 9,
    "maxDailyCount": 45,
    "legend": {
      "0": "No activity",
      "1": "1-10 words",
      "2": "11-20 words",
      "3": "21-30 words",
      "4": "31+ words"
    }
  }
}
```

---

## Error Handling

### Error Response Format

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message",
    "details": []
  },
  "timestamp": "2025-01-09T14:30:00Z"
}
```

### Error Codes

| HTTP Status | Code | Description |
|-------------|------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request data |
| 401 | `UNAUTHORIZED` | Invalid or expired token |
| 403 | `FORBIDDEN` | Access denied |
| 404 | `RESOURCE_NOT_FOUND` | Resource not found |
| 409 | `DUPLICATE_RESOURCE` | Resource already exists |
| 429 | `RATE_LIMIT_EXCEEDED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Server error |

### Validation Error Example

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "email", "message": "Invalid email format" },
      { "field": "password", "message": "Password must be at least 8 characters" }
    ]
  },
  "timestamp": "2025-01-09T14:30:00Z"
}
```

---

## Rate Limiting

### Limits

| Endpoint Type | Rate Limit |
|--------------|------------|
| Authentication | 10 requests/minute |
| Learning | 100 requests/minute |
| Other | 60 requests/minute |

### Headers

All responses include rate limit headers:

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1704812400
```

### Rate Limit Exceeded

```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "retryAfter": 45
  },
  "timestamp": "2025-01-09T14:30:00Z"
}
```

---

## OpenAPI / Swagger

Interactive API documentation is available at:

- **Development:** `http://localhost:8080/swagger-ui.html`
- **Production:** `https://api.vokabelnetz.com/swagger-ui.html`

OpenAPI JSON specification: `/v3/api-docs`
