# 🧠 Algorithms

This document describes the core algorithms that power Vokabelnetz's adaptive learning system.

## Table of Contents

- [Overview](#overview)
- [SM-2 Spaced Repetition](#sm-2-spaced-repetition)
- [Elo Rating System](#elo-rating-system)
- [Streak System](#streak-system)
- [Algorithm Integration](#algorithm-integration)
- [Quality Score Mapping](#quality-score-mapping)

---

## Overview

Vokabelnetz combines three algorithmic systems to create an adaptive, personalized learning experience:

| Algorithm | Purpose | Answers |
|-----------|---------|---------|
| **SM-2** | Spaced Repetition | *When* should you review a word? |
| **Elo Rating** | Difficulty Matching | *Which* word should you learn next? |
| **Streak System** | Motivation | How do we keep you learning daily? |

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ADAPTIVE LEARNING ENGINE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  User answers a word                                                         │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  1. ELO RATING UPDATE                                               │    │
│  │     - Calculate expected score based on ratings                      │    │
│  │     - Update user rating (↑ if correct, ↓ if wrong)                 │    │
│  │     - Update word difficulty (inverse of user change)               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  2. SM-2 SCHEDULING                                                 │    │
│  │     - Determine quality score (0-5) from answer                     │    │
│  │     - Update ease factor                                            │    │
│  │     - Calculate next interval                                       │    │
│  │     - Set next review date                                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  3. STREAK & STATS UPDATE                                           │    │
│  │     - Update daily statistics                                       │    │
│  │     - Check streak maintenance                                      │    │
│  │     - Award streak freezes if milestone reached                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  4. NEXT WORD SELECTION                                             │    │
│  │     - Prioritize: Due reviews > New words > Future reviews          │    │
│  │     - Filter by user's current CEFR level                           │    │
│  │     - Match word difficulty to user rating (±200 Elo)               │    │
│  │     - Apply randomization for variety                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## SM-2 Spaced Repetition

### Background

The SM-2 algorithm, developed by Piotr Wozniak in 1987, is the foundation of modern spaced repetition systems including Anki. It optimizes review timing based on memory retention curves.

### Core Concept

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SM-2 ALGORITHM OVERVIEW                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Memory Retention Over Time (without review):                               │
│                                                                              │
│  100% ┤████                                                                  │
│       │    ███                                                               │
│   75% ┤       ███                                                            │
│       │          ████                                                        │
│   50% ┤              █████                                                   │
│       │                   ███████                                            │
│   25% ┤                          ████████████                                │
│       │                                      ████████████████                │
│    0% ┼────────────────────────────────────────────────────────────          │
│       0    1    2    3    4    5    6    7    8    9    10  (days)           │
│                                                                              │
│  SM-2 Strategy: Review just before you forget                               │
│                                                                              │
│  Review 1 (Day 1) ───▶ Review 2 (Day 2) ───▶ Review 3 (Day 8)               │
│         ↓                      ↓                      ↓                      │
│    Interval: 1 day       Interval: 6 days      Interval: 15 days            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Quality Ratings

| Quality | Description | Effect on Interval |
|---------|-------------|-------------------|
| 0 | Complete blackout | Reset to beginning |
| 1 | Incorrect, recognized answer | Reset to beginning |
| 2 | Incorrect, but seemed easy | Reset to beginning |
| 3 | Correct with serious difficulty | Keep interval, decrease EF |
| 4 | Correct with some hesitation | Increase interval normally |
| 5 | Perfect instant recall | Increase interval, increase EF |

### Key Variables

| Variable | Range | Default | Description |
|----------|-------|---------|-------------|
| EF (Ease Factor) | 1.3 - 5.0 | 2.5 | How easy the word is for this user |
| n (Repetition) | 0+ | 0 | Consecutive correct answers |
| I (Interval) | 1+ days | 1 | Days until next review |

### Formulas

**Interval Calculation:**
```
if n = 0:  I = 1 day
if n = 1:  I = 6 days
if n > 1:  I = I(n-1) × EF
```

**Ease Factor Update:**
```
EF' = EF + (0.1 - (5 - q) × (0.08 + (5 - q) × 0.02))

where:
  q = quality rating (0-5)
  EF is never less than 1.3
```

### Implementation

```java
@Service
public class SpacedRepetitionService {
    
    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double MAX_EASE_FACTOR = 5.0;
    private static final double DEFAULT_EASE_FACTOR = 2.5;
    
    /**
     * Calculate next review based on SM-2 algorithm.
     * 
     * @param progress Current user progress on word
     * @param quality  Quality of response (0-5)
     * @return Updated progress with new interval and next review date
     */
    public UserWordProgress calculateNextReview(
            UserWordProgress progress, 
            int quality) {
        
        // Validate quality
        quality = Math.max(0, Math.min(5, quality));
        
        // If quality < 3, reset repetitions (incorrect answer)
        if (quality < 3) {
            progress.setRepetition(0);
            progress.setIntervalDays(1);
        } else {
            // Correct answer - calculate new interval
            int repetition = progress.getRepetition();
            
            if (repetition == 0) {
                progress.setIntervalDays(1);
            } else if (repetition == 1) {
                progress.setIntervalDays(6);
            } else {
                int newInterval = (int) Math.round(
                    progress.getIntervalDays() * progress.getEaseFactor()
                );
                // Cap maximum interval at 365 days
                progress.setIntervalDays(Math.min(newInterval, 365));
            }
            
            progress.setRepetition(repetition + 1);
        }
        
        // Update ease factor
        double ef = progress.getEaseFactor();
        double newEf = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        newEf = Math.max(MIN_EASE_FACTOR, Math.min(MAX_EASE_FACTOR, newEf));
        progress.setEaseFactor(newEf);
        
        // Set next review date
        LocalDateTime nextReview = LocalDateTime.now()
            .plusDays(progress.getIntervalDays());
        progress.setNextReviewAt(nextReview);
        progress.setLastQuality(quality);
        progress.setLastReviewedAt(LocalDateTime.now());
        
        // Check if word is "learned" (interval > 21 days)
        if (progress.getIntervalDays() > 21 && !progress.isLearned()) {
            progress.setLearned(true);
            progress.setLearnedAt(LocalDateTime.now());
        }
        
        return progress;
    }
    
    /**
     * Get words due for review.
     */
    public List<UserWordProgress> getWordsForReview(Long userId, int limit) {
        return progressRepository.findDueForReview(
            userId, 
            LocalDateTime.now(),
            PageRequest.of(0, limit, Sort.by("nextReviewAt").ascending())
        );
    }
    
    /**
     * Get count of overdue words.
     */
    public int getOverdueCount(Long userId) {
        return progressRepository.countOverdue(userId, LocalDateTime.now());
    }
}
```

### Example Flow

```
Word: "arbeiten" (to work)
User answers correctly with slight hesitation (quality = 4)

Before:
  - Repetition: 2
  - Interval: 6 days
  - EF: 2.5
  - Last reviewed: Jan 1

Calculation:
  - New interval: 6 × 2.5 = 15 days
  - New EF: 2.5 + (0.1 - 1 × 0.1) = 2.5 (unchanged for q=4)
  - New repetition: 3

After:
  - Repetition: 3
  - Interval: 15 days
  - EF: 2.5
  - Next review: Jan 16
```

---

## Elo Rating System

### Background

The Elo rating system, developed by Arpad Elo for chess, measures relative skill levels. Vokabelnetz adapts this to match word difficulty with user skill.

### Core Concept

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        ELO RATING SYSTEM                                 │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Both users AND words have ratings:                                      │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                    RATING SCALE                                    │  │
│  │                                                                    │  │
│  │  User Ratings:                    Word Difficulty:                 │  │
│  │  ├─ 0-800    : Beginner          ├─ 0-800    : Very Easy (A1)      │  │
│  │  ├─ 800-1200 : Elementary        ├─ 800-1200 : Easy (A1-A2)        │  │
│  │  ├─ 1200-1600: Intermediate      ├─ 1200-1600: Medium (A2-B1)      │  │
│  │  ├─ 1600-2000: Advanced          ├─ 1600-2000: Hard (B1-B2)        │  │
│  │  └─ 2000+    : Expert            └─ 2000+    : Very Hard (B2+)     │  │
│  │                                                                    │  │
│  │  Default: 1000 (both users and words)                              │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  Matching Example:                                                       │
│                                                                          │
│  User (1200) vs Word (1150):                                             │
│                                                                          │
│  User:  ████████████░░░░░░░░ (1200)                                      │
│  Word:  ███████████░░░░░░░░░ (1150)                                      │
│                                                                          │
│  Expected success rate: ~57% (user slightly stronger)                    │
│  This is the "optimal challenge zone" for learning!                      │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Formulas

**Expected Score:**
```
E = 1 / (1 + 10^((Rw - Ru) / 400))

where:
  E  = Expected probability of correct answer (0.0 to 1.0)
  Rw = Word difficulty rating
  Ru = User rating
```

**Rating Updates:**
```
Ru' = Ru + K × (S - E)    (User rating)
Rw' = Rw + K × (E - S)    (Word rating - inverse direction)

where:
  K = 32 (K-factor, determines update magnitude)
  S = Actual score (1 for correct, 0 for incorrect)
  E = Expected score
```

### Implementation

```java
@Service
public class EloRatingService {
    
    private static final int K_FACTOR = 32;
    private static final int MIN_RATING = 100;
    private static final int MAX_RATING = 3000;
    private static final int DEFAULT_RATING = 1000;
    private static final int MATCH_TOLERANCE = 200;
    
    /**
     * Calculate expected probability of correct answer.
     */
    public double calculateExpectedScore(int userRating, int wordDifficulty) {
        return 1.0 / (1 + Math.pow(10, (wordDifficulty - userRating) / 400.0));
    }
    
    /**
     * Update both user and word ratings based on answer.
     */
    @Transactional
    public EloUpdateResult updateRatings(User user, Word word, boolean correct) {
        int userRating = user.getEloRating();
        int wordRating = word.getDifficultyRating();
        
        // Calculate expected score
        double expected = calculateExpectedScore(userRating, wordRating);
        int actual = correct ? 1 : 0;
        
        // Calculate rating changes
        int userChange = (int) Math.round(K_FACTOR * (actual - expected));
        int wordChange = (int) Math.round(K_FACTOR * (expected - actual));
        
        // Apply changes with bounds
        int newUserRating = clamp(userRating + userChange, MIN_RATING, MAX_RATING);
        int newWordRating = clamp(wordRating + wordChange, MIN_RATING, MAX_RATING);
        
        // Update entities
        user.setEloRating(newUserRating);
        word.setDifficultyRating(newWordRating);
        
        return new EloUpdateResult(
            userRating, newUserRating, userChange,
            wordRating, newWordRating, wordChange,
            expected
        );
    }
    
    /**
     * Select optimal word for user based on Elo matching.
     * Words within ±200 rating points are considered optimal.
     */
    public Word selectNextWord(User user, List<Word> availableWords) {
        int userRating = user.getEloRating();
        
        // Filter words within skill range
        List<Word> matchedWords = availableWords.stream()
            .filter(w -> Math.abs(w.getDifficultyRating() - userRating) <= MATCH_TOLERANCE)
            .toList();
        
        if (matchedWords.isEmpty()) {
            // Fall back to closest word if no matches
            return availableWords.stream()
                .min(Comparator.comparingInt(w -> 
                    Math.abs(w.getDifficultyRating() - userRating)))
                .orElse(null);
        }
        
        // Weighted random selection (prefer closer matches)
        return weightedRandomSelect(matchedWords, userRating);
    }
    
    private Word weightedRandomSelect(List<Word> words, int userRating) {
        // Calculate weights (closer = higher weight)
        double[] weights = words.stream()
            .mapToDouble(w -> 1.0 / (1 + Math.abs(w.getDifficultyRating() - userRating)))
            .toArray();
        
        double totalWeight = Arrays.stream(weights).sum();
        double random = Math.random() * totalWeight;
        
        double cumulative = 0;
        for (int i = 0; i < words.size(); i++) {
            cumulative += weights[i];
            if (random <= cumulative) {
                return words.get(i);
            }
        }
        
        return words.get(words.size() - 1);
    }
    
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

// Result DTO
public record EloUpdateResult(
    int oldUserRating,
    int newUserRating,
    int userChange,
    int oldWordRating,
    int newWordRating,
    int wordChange,
    double expectedScore
) {}
```

### Example Calculations

```
Scenario 1: User (1200) answers easy word (900) correctly
─────────────────────────────────────────────────────────
Expected score: 1 / (1 + 10^((900-1200)/400)) = 0.85 (85%)
User was expected to succeed → small reward

User change:  32 × (1 - 0.85) = +5 points  → 1205
Word change:  32 × (0.85 - 1) = -5 points  → 895


Scenario 2: User (1200) answers hard word (1500) correctly
─────────────────────────────────────────────────────────
Expected score: 1 / (1 + 10^((1500-1200)/400)) = 0.36 (36%)
User beat the odds → big reward

User change:  32 × (1 - 0.36) = +20 points → 1220
Word change:  32 × (0.36 - 1) = -20 points → 1480


Scenario 3: User (1200) answers matched word (1150) incorrectly
─────────────────────────────────────────────────────────
Expected score: 1 / (1 + 10^((1150-1200)/400)) = 0.57 (57%)
User was expected to succeed → moderate penalty

User change:  32 × (0 - 0.57) = -18 points → 1182
Word change:  32 × (0.57 - 0) = +18 points → 1168
```

---

## Streak System

### Overview

The streak system motivates daily learning by tracking consecutive days of activity with timezone-aware calculations and optional "freeze" protection.

**Timezone Detection Logic:**
```
1. Try to detect system/browser timezone (Intl.DateTimeFormat().resolvedOptions().timeZone)
2. If detected and valid → Use system timezone
3. If not available or invalid → Default to 'Europe/Istanbul' (UTC+2/+3)
```

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         STREAK SYSTEM ARCHITECTURE                        │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                        STREAK CALCULATION                           │  │
│  │                                                                     │  │
│  │   User Timezone: Europe/Istanbul (UTC+3)                            │  │
│  │   Day Boundary: 00:00 local time                                    │  │
│  │   Minimum Activity: 1 word reviewed                                 │  │
│  │                                                                     │  │
│  │   ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐     │  │
│  │   │ Mon │──│ Tue │──│ Wed │──│ Thu │──│ Fri │──│ Sat │──│ Sun │     │  │
│  │   │  ✓  │  │  ✓  │  │  ✓  │  │  ✗  │  │  ✓  │  │  ✓  │  │  ?  │     │  │
│  │   └─────┘  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘     │  │
│  │                           │                                         │  │
│  │                     Streak broken!                                  │  │
│  │                     (unless freeze used)                            │  │
│  │                                                                     │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                     STREAK FREEZE SYSTEM                            │  │
│  │                                                                     │  │
│  │   • Users earn 1 freeze for every 7-day streak milestone            │  │
│  │   • Maximum 3 freezes can be stored                                 │  │
│  │   • Freeze auto-activates if day ends without activity              │  │
│  │   • Manual freeze activation available in settings                  │  │
│  │                                                                     │  │
│  │   Milestones:                                                       │  │
│  │   ├─ 7 days   → +1 freeze                                           │  │
│  │   ├─ 14 days  → +1 freeze                                           │  │
│  │   ├─ 21 days  → +1 freeze                                           │  │
│  │   ├─ 30 days  → +1 freeze (+ badge)                                 │  │
│  │   └─ (continues every 7 days)                                       │  │
│  │                                                                     │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### Streak States

| State | Description | Action |
|-------|-------------|--------|
| `ACTIVE` | User completed today's goal | Show celebration |
| `AT_RISK` | Today not yet completed | Show warning |
| `BROKEN` | Missed a day, streak reset | Show recovery message |
| `FROZEN` | Freeze used, streak preserved | Show freeze icon |
| `NEW` | First day of new streak | Show encouragement |

### Implementation

```java
@Service
public class StreakService {
    
    private static final int FREEZE_MILESTONE_DAYS = 7;
    private static final int MAX_FREEZES = 3;
    private static final String DEFAULT_TIMEZONE = "Europe/Istanbul"; // UTC+2/+3
    
    /**
     * Get user's timezone with fallback to default.
     * Frontend sends detected system timezone during registration/settings update.
     */
    private ZoneId getUserTimezone(User user) {
        String timezone = user.getTimezone();
        
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("Invalid timezone '{}' for user {}, using default", 
                timezone, user.getId());
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }
    
    /**
     * Check and update streak status at end of day.
     * Called by scheduled job at midnight in each timezone.
     */
    @Transactional
    public StreakResult processEndOfDay(User user) {
        ZoneId userZone = getUserTimezone(user);
        LocalDate today = LocalDate.now(userZone);
        LocalDate yesterday = today.minusDays(1);
        
        // Check if user was active yesterday
        boolean wasActiveYesterday = dailyStatsRepository
            .existsByUserIdAndStatDateAndWordsReviewedGreaterThan(
                user.getId(), yesterday, 0
            );
        
        if (wasActiveYesterday) {
            // Streak continues
            return maintainStreak(user, today);
        } else {
            // Check for freeze
            return handleMissedDay(user, today);
        }
    }
    
    private StreakResult maintainStreak(User user, LocalDate today) {
        int newStreak = user.getCurrentStreak() + 1;
        user.setCurrentStreak(newStreak);
        
        // Update longest streak
        if (newStreak > user.getLongestStreak()) {
            user.setLongestStreak(newStreak);
        }
        
        // Award freeze at milestones
        if (newStreak % FREEZE_MILESTONE_DAYS == 0 
                && user.getStreakFreezesAvailable() < MAX_FREEZES) {
            user.setStreakFreezesAvailable(
                user.getStreakFreezesAvailable() + 1
            );
            return StreakResult.milestoneReached(newStreak);
        }
        
        // Record history
        saveStreakHistory(user.getId(), today, newStreak, true, false);
        
        return StreakResult.maintained(newStreak);
    }
    
    private StreakResult handleMissedDay(User user, LocalDate today) {
        // Try to use freeze
        if (user.getStreakFreezesAvailable() > 0) {
            user.setStreakFreezesAvailable(
                user.getStreakFreezesAvailable() - 1
            );
            user.setStreakFreezeUsedAt(today.minusDays(1));
            
            saveStreakHistory(
                user.getId(), today.minusDays(1), 
                user.getCurrentStreak(), false, true
            );
            
            return StreakResult.frozen(user.getCurrentStreak());
        }
        
        // Streak broken
        int lostStreak = user.getCurrentStreak();
        user.setCurrentStreak(0);
        
        saveStreakHistory(
            user.getId(), today.minusDays(1), 
            0, false, false
        );
        
        return StreakResult.broken(lostStreak);
    }
    
    /**
     * Get current streak status for display.
     */
    public StreakStatus getStreakStatus(User user) {
        ZoneId userZone = getUserTimezone(user);
        LocalDate today = LocalDate.now(userZone);
        
        boolean completedToday = dailyStatsRepository
            .existsByUserIdAndStatDateAndWordsReviewedGreaterThan(
                user.getId(), today, 0
            );
        
        LocalTime now = LocalTime.now(userZone);
        LocalTime endOfDay = LocalTime.of(23, 59);
        long minutesRemaining = now.until(endOfDay, ChronoUnit.MINUTES);
        
        return new StreakStatus(
            user.getCurrentStreak(),
            user.getLongestStreak(),
            completedToday,
            !completedToday && minutesRemaining < 120, // at risk if <2 hours left
            user.getStreakFreezesAvailable(),
            minutesRemaining
        );
    }
    
    /**
     * Manually activate freeze for today.
     */
    @Transactional
    public boolean activateFreeze(User user) {
        if (user.getStreakFreezesAvailable() <= 0) {
            return false;
        }
        
        ZoneId userZone = getUserTimezone(user);
        LocalDate today = LocalDate.now(userZone);
        
        user.setStreakFreezesAvailable(
            user.getStreakFreezesAvailable() - 1
        );
        user.setStreakFreezeUsedAt(today);
        
        saveStreakHistory(
            user.getId(), today, 
            user.getCurrentStreak(), false, true
        );
        
        return true;
    }
}

// DTOs
public record StreakResult(
    StreakResultType type,
    int currentStreak,
    int lostStreak,
    boolean freezeEarned
) {
    public static StreakResult maintained(int streak) {
        return new StreakResult(MAINTAINED, streak, 0, false);
    }
    
    public static StreakResult broken(int lost) {
        return new StreakResult(BROKEN, 0, lost, false);
    }
    
    public static StreakResult frozen(int streak) {
        return new StreakResult(FROZEN, streak, 0, false);
    }
    
    public static StreakResult milestoneReached(int streak) {
        return new StreakResult(MILESTONE, streak, 0, true);
    }
}

public record StreakStatus(
    int currentStreak,
    int longestStreak,
    boolean completedToday,
    boolean atRisk,
    int freezesAvailable,
    long minutesUntilReset
) {}
```

### Scheduled Job

```java
@Component
public class StreakScheduler {
    
    private final StreakService streakService;
    private final UserRepository userRepository;
    
    /**
     * Run every hour to process users whose day just ended.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at :00
    @Transactional
    public void processStreaks() {
        // Get current hour in UTC
        int currentHourUtc = LocalTime.now(ZoneOffset.UTC).getHour();
        
        // Find users whose local midnight just passed
        // (e.g., UTC+3 users at 21:00 UTC = their midnight)
        List<User> users = userRepository.findUsersWithMidnightAt(currentHourUtc);
        
        for (User user : users) {
            try {
                StreakResult result = streakService.processEndOfDay(user);
                
                if (result.type() == StreakResultType.BROKEN) {
                    notificationService.sendStreakBrokenNotification(user);
                } else if (result.type() == StreakResultType.MILESTONE) {
                    notificationService.sendMilestoneNotification(user, result.currentStreak());
                }
            } catch (Exception e) {
                log.error("Failed to process streak for user {}: {}", 
                    user.getId(), e.getMessage());
            }
        }
    }
}
```

---

## Algorithm Integration

### LearningService: Orchestrating All Algorithms

```java
@Service
@Transactional
public class LearningService {
    
    private final SpacedRepetitionService sm2Service;
    private final EloRatingService eloService;
    private final StreakService streakService;
    private final DailyStatsService statsService;
    
    /**
     * Process a user's answer to a word.
     * Integrates SM-2, Elo, and Streak systems.
     */
    public AnswerResult processAnswer(
            User user, 
            Word word, 
            UserWordProgress progress,
            AnswerRequest request) {
        
        boolean correct = request.isCorrect();
        int quality = mapToQuality(request);
        
        // 1. Update Elo ratings
        EloUpdateResult eloResult = eloService.updateRatings(user, word, correct);
        
        // 2. Update SM-2 scheduling
        progress = sm2Service.calculateNextReview(progress, quality);
        progress.setTimesCorrect(progress.getTimesCorrect() + (correct ? 1 : 0));
        progress.setTimesIncorrect(progress.getTimesIncorrect() + (correct ? 0 : 1));
        progress.setLastResponseTimeMs(request.getResponseTimeMs());
        progressRepository.save(progress);
        
        // 3. Update daily stats
        statsService.recordAnswer(user.getId(), correct, request.getResponseTimeMs());
        
        // 4. Check streak status
        StreakStatus streakStatus = streakService.getStreakStatus(user);
        
        // 5. Update word global stats
        word.setTimesShown(word.getTimesShown() + 1);
        if (correct) {
            word.setTimesCorrect(word.getTimesCorrect() + 1);
        }
        wordRepository.save(word);
        
        // 6. Check for achievements
        List<Achievement> achievements = checkAchievements(user, progress, eloResult);
        
        return new AnswerResult(
            correct,
            eloResult,
            new SM2Result(
                progress.getEaseFactor(),
                progress.getIntervalDays(),
                progress.getRepetition(),
                progress.getNextReviewAt()
            ),
            streakStatus,
            achievements
        );
    }
    
    /**
     * Select the next word for the user.
     */
    public NextWordResult getNextWord(User user, LearningSession session) {
        // Priority 1: Due review words
        List<UserWordProgress> dueWords = sm2Service.getWordsForReview(user.getId(), 10);
        
        if (!dueWords.isEmpty()) {
            // Use Elo to pick the best match from due words
            List<Word> words = dueWords.stream()
                .map(p -> wordRepository.findById(p.getWordId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
            
            Word selected = eloService.selectNextWord(user, words);
            UserWordProgress progress = dueWords.stream()
                .filter(p -> p.getWordId().equals(selected.getId()))
                .findFirst()
                .orElse(null);
            
            return new NextWordResult(selected, progress, true);
        }
        
        // Priority 2: New words
        List<Word> newWords = wordRepository.findNewWordsForUser(
            user.getId(),
            session.getCefrLevel(),
            PageRequest.of(0, 20)
        );
        
        if (!newWords.isEmpty()) {
            Word selected = eloService.selectNextWord(user, newWords);
            return new NextWordResult(selected, null, false);
        }
        
        // Priority 3: Future review words (early review)
        return getEarlyReviewWord(user, session);
    }
    
    private int mapToQuality(AnswerRequest request) {
        if (!request.isCorrect()) {
            return request.isRecognized() ? 1 : 0;
        }
        
        if (request.isUsedHint()) {
            return 2;
        }
        
        // Map response time to quality
        int responseMs = request.getResponseTimeMs();
        if (responseMs < 2000) return 5;      // < 2s = perfect
        if (responseMs < 5000) return 4;      // 2-5s = good
        return 3;                              // > 5s = difficult
    }
}
```

---

## Quality Score Mapping

### User Actions to SM-2 Quality

| User Action | Response Time | Quality | Description |
|-------------|---------------|---------|-------------|
| Correct | < 2 seconds | 5 | Perfect, instant recall |
| Correct | 2-5 seconds | 4 | Correct with minor hesitation |
| Correct | > 5 seconds | 3 | Correct with difficulty |
| "Show Hint" used | - | 2 | Needed help, but got it right |
| Incorrect, recognized | - | 1 | Wrong, but recognized answer |
| Skip / Timeout / Wrong | - | 0 | Complete failure |

### Quality Effect Summary

| Quality | Repetition | Interval | Ease Factor |
|---------|------------|----------|-------------|
| 0 | Reset to 0 | 1 day | Decrease |
| 1 | Reset to 0 | 1 day | Decrease |
| 2 | Reset to 0 | 1 day | Decrease |
| 3 | +1 | Keep | Decrease slightly |
| 4 | +1 | × EF | No change |
| 5 | +1 | × EF | Increase |
