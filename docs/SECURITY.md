# 🔐 Security Documentation

This document describes the security architecture, policies, and best practices for Vokabelnetz.

## Table of Contents

- [Authentication](#authentication)
- [Token Management](#token-management)
- [Password Security](#password-security)
- [Authorization & Roles](#authorization--roles)
- [Rate Limiting & Brute Force Protection](#rate-limiting--brute-force-protection)
- [Content Security Policy](#content-security-policy)
- [Logging & Privacy](#logging--privacy)
- [Session Management](#session-management)
- [Data Retention Policy](#data-retention-policy)
- [Error Message Policy](#error-message-policy)
- [Security Checklist](#security-checklist)

---

## Authentication

### JWT Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AUTHENTICATION FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────┐      ┌──────────────┐      ┌──────────────┐                   │
│  │  Client  │      │   Backend    │      │  Database    │                   │
│  └────┬─────┘      └──────┬───────┘      └──────┬───────┘                   │
│       │                   │                     │                           │
│       │ 1. POST /login    │                     │                           │
│       │   (email, pass)   │                     │                           │
│       │──────────────────>│                     │                           │
│       │                   │ 2. Verify password  │                           │
│       │                   │────────────────────>│                           │
│       │                   │<────────────────────│                           │
│       │                   │                     │                           │
│       │                   │ 3. Generate tokens  │                           │
│       │                   │   - Access (15min)  │                           │
│       │                   │   - Refresh (7day)  │                           │
│       │                   │                     │                           │
│       │                   │ 4. Store refresh    │                           │
│       │                   │    token in DB      │                           │
│       │                   │────────────────────>│                           │
│       │                   │                     │                           │
│       │ 5. Response:      │                     │                           │
│       │   Access token    │                     │                           │
│       │   (in JSON body)  │                     │                           │
│       │   +               │                     │                           │
│       │   Refresh token   │                     │                           │
│       │   (HttpOnly cookie)                     │                           │
│       │<──────────────────│                     │                           │
│       │                   │                     │                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Token Specifications

| Token Type | Storage | Lifetime | Contains |
|------------|---------|----------|----------|
| Access Token | Memory (Angular service) | 15 minutes | user_id, email, roles, iat, exp |
| Refresh Token | HttpOnly Cookie | 7 days | Opaque token (random bytes) |

### Token Storage Strategy

> **CRITICAL SECURITY DECISION**

```typescript
// ✅ CORRECT: Access token in memory only
@Injectable({ providedIn: 'root' })
export class AuthStore {
  // Access token lives only in memory - cleared on page refresh
  private accessToken = signal<string | null>(null);
  
  // NEVER store tokens in localStorage or sessionStorage
  // ❌ localStorage.setItem('token', token);  // XSS vulnerable!
  // ❌ sessionStorage.setItem('token', token); // XSS vulnerable!
}
```

**Why this approach?**

| Storage Method | XSS Risk | CSRF Risk | Recommendation |
|----------------|----------|-----------|----------------|
| localStorage | 🔴 HIGH | 🟢 None | ❌ Never use for tokens |
| sessionStorage | 🔴 HIGH | 🟢 None | ❌ Never use for tokens |
| Memory (variable) | 🟢 LOW | 🟢 None | ✅ Use for access token |
| HttpOnly Cookie | 🟢 LOW | 🟡 Medium | ✅ Use for refresh token |

### Cookie Configuration

```java
// Refresh token cookie settings
ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", token)
    .httpOnly(true)          // JavaScript cannot access
    .secure(true)            // HTTPS only
    .sameSite("Strict")      // No cross-site requests
    .path("/api/auth")       // Only sent to auth endpoints
    .maxAge(Duration.ofDays(7))
    .build();
```

---

## Token Management

### Refresh Token Rotation

> **CRITICAL:** Every refresh request MUST rotate the token to limit exposure window.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      REFRESH TOKEN ROTATION                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Client sends refresh request with cookie                                 │
│                                                                              │
│  2. Server validates:                                                        │
│     □ Token exists in DB                                                     │
│     □ Token is not revoked (is_revoked = FALSE)                             │
│     □ Token is not expired (expires_at > NOW)                               │
│     □ Token family is valid (no reuse detection)                            │
│                                                                              │
│  3. Server actions:                                                          │
│     □ Mark OLD token as revoked (is_revoked = TRUE)                         │
│     □ Generate NEW refresh token                                             │
│     □ Store NEW token in DB                                                  │
│     □ Generate NEW access token                                              │
│                                                                              │
│  4. Response:                                                                │
│     □ New access token in body                                               │
│     □ New refresh token in HttpOnly cookie                                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Implementation

```java
@Service
@Transactional
public class TokenService {
    
    private static final int MAX_ACTIVE_SESSIONS = 5;
    
    /**
     * Refresh tokens with automatic rotation.
     * Old token is ALWAYS revoked, new token is ALWAYS generated.
     */
    public TokenPair refreshTokens(String oldRefreshToken, HttpServletRequest request) {
        RefreshToken storedToken = refreshTokenRepository
            .findByToken(oldRefreshToken)
            .orElseThrow(() -> new InvalidTokenException("Token not found"));
        
        // Validate token
        validateRefreshToken(storedToken);
        
        // CRITICAL: Revoke old token immediately
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        storedToken.setRevokedReason("ROTATION");
        refreshTokenRepository.save(storedToken);
        
        // Generate new tokens
        User user = storedToken.getUser();
        String newAccessToken = generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user, request);
        
        // Enforce session limit
        enforceSessionLimit(user);
        
        return new TokenPair(newAccessToken, newRefreshToken.getToken());
    }
    
    /**
     * Detect refresh token reuse (potential theft).
     * If a revoked token is used, revoke ALL tokens for that user.
     */
    private void validateRefreshToken(RefreshToken token) {
        if (token.isRevoked()) {
            // SECURITY: Revoked token reuse detected - possible theft!
            log.warn("SECURITY: Refresh token reuse detected for user {}", 
                token.getUser().getId());
            
            // Revoke ALL tokens for this user (nuclear option)
            refreshTokenRepository.revokeAllByUserId(token.getUser().getId());
            
            // Alert security team
            securityAlertService.sendAlert(
                "Refresh token reuse detected",
                token.getUser(),
                token.getIpAddress()
            );
            
            throw new TokenReusedException("Token has been revoked. All sessions terminated.");
        }
        
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh token expired");
        }
    }
    
    /**
     * Limit active sessions per user.
     */
    private void enforceSessionLimit(User user) {
        List<RefreshToken> activeTokens = refreshTokenRepository
            .findActiveByUserId(user.getId());
        
        if (activeTokens.size() > MAX_ACTIVE_SESSIONS) {
            // Revoke oldest tokens
            activeTokens.stream()
                .sorted(Comparator.comparing(RefreshToken::getCreatedAt))
                .limit(activeTokens.size() - MAX_ACTIVE_SESSIONS)
                .forEach(t -> {
                    t.setRevoked(true);
                    t.setRevokedReason("SESSION_LIMIT");
                });
            refreshTokenRepository.saveAll(activeTokens);
        }
    }
}
```

### Token Revocation Scenarios

| Event | Action |
|-------|--------|
| **Logout** | Revoke current refresh token |
| **Logout from all devices** | Revoke ALL user's refresh tokens |
| **Password change** | Revoke ALL user's refresh tokens |
| **Email change** | Revoke ALL user's refresh tokens |
| **Account deletion** | Revoke ALL user's refresh tokens |
| **Suspicious activity detected** | Revoke ALL user's refresh tokens |
| **Refresh token reuse** | Revoke ALL user's refresh tokens (theft detected) |

### Logout Implementation

```java
@PostMapping("/logout")
public ResponseEntity<?> logout(
    @CookieValue(name = "refresh_token", required = false) String refreshToken,
    HttpServletResponse response
) {
    if (refreshToken != null) {
        // Revoke token in database
        refreshTokenRepository.findByToken(refreshToken)
            .ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
                token.setRevokedReason("LOGOUT");
                refreshTokenRepository.save(token);
            });
    }
    
    // Clear cookie
    ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/api/auth")
        .maxAge(0)  // Immediate expiration
        .build();
    
    response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
    
    return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
}

@PostMapping("/logout-all")
public ResponseEntity<?> logoutAllDevices(
    @AuthenticationPrincipal UserDetails userDetails,
    HttpServletResponse response
) {
    // Revoke ALL refresh tokens for user
    refreshTokenRepository.revokeAllByUserId(userDetails.getId());
    
    // Clear current cookie
    // ... (same as above)
    
    return ResponseEntity.ok(ApiResponse.success("Logged out from all devices"));
}
```

---

## Password Security

### Hashing Algorithm

> **Algorithm:** Argon2id (winner of Password Hashing Competition 2015)

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id configuration
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        // Parameters:
        // - Memory: 16 MB
        // - Iterations: 2
        // - Parallelism: 1
        // - Salt: 16 bytes (auto-generated)
        // - Hash length: 32 bytes
    }
}
```

**Why Argon2id?**
- Memory-hard: Resists GPU/ASIC attacks
- Time-hard: Configurable iteration count
- Combines Argon2i (side-channel resistant) and Argon2d (GPU resistant)

### Password Policy

```java
public class PasswordPolicy {
    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;
    public static final boolean REQUIRE_UPPERCASE = true;
    public static final boolean REQUIRE_LOWERCASE = true;
    public static final boolean REQUIRE_DIGIT = true;
    public static final boolean REQUIRE_SPECIAL = true;
    public static final int MIN_UNIQUE_CHARS = 5;
    
    // Common password blacklist check
    public static final boolean CHECK_COMMON_PASSWORDS = true;
    
    // Prevent password reuse (last N passwords)
    public static final int PASSWORD_HISTORY_COUNT = 5;
}
```

### Password Validation

```java
@Service
public class PasswordValidationService {
    
    private final Set<String> commonPasswords;
    
    public PasswordValidationResult validate(String password, User user) {
        List<String> errors = new ArrayList<>();
        
        // Length check
        if (password.length() < PasswordPolicy.MIN_LENGTH) {
            errors.add("Password must be at least " + PasswordPolicy.MIN_LENGTH + " characters");
        }
        
        // Complexity checks
        if (!password.matches(".*[A-Z].*")) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            errors.add("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            errors.add("Password must contain at least one digit");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            errors.add("Password must contain at least one special character");
        }
        
        // Unique characters
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars < PasswordPolicy.MIN_UNIQUE_CHARS) {
            errors.add("Password must contain at least " + PasswordPolicy.MIN_UNIQUE_CHARS + " unique characters");
        }
        
        // Common password check
        if (commonPasswords.contains(password.toLowerCase())) {
            errors.add("Password is too common. Please choose a stronger password.");
        }
        
        // Personal info check
        if (user != null) {
            String lowerPass = password.toLowerCase();
            if (user.getEmail() != null && lowerPass.contains(user.getEmail().split("@")[0].toLowerCase())) {
                errors.add("Password cannot contain your email address");
            }
            if (user.getDisplayName() != null && lowerPass.contains(user.getDisplayName().toLowerCase())) {
                errors.add("Password cannot contain your name");
            }
        }
        
        return new PasswordValidationResult(errors.isEmpty(), errors);
    }
}
```

### Password Change Flow

```java
@PutMapping("/me/password")
public ResponseEntity<?> changePassword(
    @AuthenticationPrincipal UserDetails userDetails,
    @Valid @RequestBody PasswordChangeRequest request
) {
    User user = userService.findById(userDetails.getId());
    
    // 1. Verify current password
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
        throw new InvalidCredentialsException("Current password is incorrect");
    }
    
    // 2. Validate new password
    PasswordValidationResult validation = passwordValidationService.validate(
        request.getNewPassword(), user);
    if (!validation.isValid()) {
        throw new PasswordPolicyException(validation.getErrors());
    }
    
    // 3. Check password history (prevent reuse)
    if (passwordHistoryService.isPasswordUsedBefore(user, request.getNewPassword())) {
        throw new PasswordPolicyException("Cannot reuse recent passwords");
    }
    
    // 4. Update password
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    user.setPasswordChangedAt(Instant.now());
    userRepository.save(user);
    
    // 5. Save to password history
    passwordHistoryService.savePasswordHash(user, user.getPasswordHash());
    
    // 6. CRITICAL: Revoke ALL refresh tokens
    refreshTokenRepository.revokeAllByUserId(user.getId());
    
    // 7. Send notification email
    emailService.sendPasswordChangedNotification(user);
    
    return ResponseEntity.ok(ApiResponse.success(
        "Password changed successfully. Please log in again on all devices."
    ));
}
```

---

## Password Reset Tokens

### Database Schema

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Token: 32 random bytes, hex encoded (64 chars)
    -- Stored as hash to prevent DB leak exposure
    token_hash VARCHAR(64) NOT NULL,
    
    -- Short expiration: 1 hour
    expires_at TIMESTAMP NOT NULL,
    
    -- Single use
    used_at TIMESTAMP,
    
    -- Audit info
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    
    created_at TIMESTAMP DEFAULT NOW()
);

-- Index for cleanup job
CREATE INDEX idx_reset_tokens_expires ON password_reset_tokens(expires_at);

-- Cleanup: Delete expired tokens
-- Run via cron: DELETE FROM password_reset_tokens WHERE expires_at < NOW() - INTERVAL '24 hours';
```

### Token Generation & Validation

```java
@Service
public class PasswordResetService {
    
    private static final int TOKEN_BYTES = 32;
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(1);
    private static final int MAX_ACTIVE_TOKENS = 3;
    
    /**
     * Generate password reset token.
     * Token is returned to send via email; only hash is stored.
     */
    @Transactional
    public String createResetToken(String email, HttpServletRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElse(null);
        
        // SECURITY: Don't reveal if email exists
        // Always return success, but only send email if user exists
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", 
                maskEmail(email));
            return null; // Caller should still show success message
        }
        
        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());
        
        // Rate limit: Max 3 tokens per hour per user
        long recentTokens = passwordResetTokenRepository
            .countByUserIdAndCreatedAtAfter(user.getId(), Instant.now().minus(Duration.ofHours(1)));
        if (recentTokens >= MAX_ACTIVE_TOKENS) {
            log.warn("Password reset rate limit exceeded for user: {}", user.getId());
            throw new RateLimitException("Too many reset requests. Please try again later.");
        }
        
        // Generate secure random token
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);
        
        // Store HASH of token (not plain token)
        String tokenHash = DigestUtils.sha256Hex(token);
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(TOKEN_VALIDITY));
        resetToken.setIpAddress(getClientIp(request));
        resetToken.setUserAgent(request.getHeader("User-Agent"));
        passwordResetTokenRepository.save(resetToken);
        
        return token; // Return plain token to send via email
    }
    
    /**
     * Validate and consume reset token.
     */
    @Transactional
    public User validateAndConsumeToken(String token) {
        String tokenHash = DigestUtils.sha256Hex(token);
        
        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHashAndUsedAtIsNull(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));
        
        // Check expiration
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Reset token has expired");
        }
        
        // Mark as used (single use)
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
        
        return resetToken.getUser();
    }
}
```

### Email Content Security

```java
@Service
public class EmailService {
    
    /**
     * Send password reset email.
     * NEVER include the actual password or full token in logs.
     */
    public void sendPasswordResetEmail(User user, String token) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
        
        // Email content - minimal sensitive info
        String subject = "Vokabelnetz - Password Reset Request";
        String body = """
            Hello %s,
            
            We received a request to reset your password. Click the link below to set a new password:
            
            %s
            
            This link will expire in 1 hour.
            
            If you didn't request this, please ignore this email or contact support if you're concerned.
            
            - Vokabelnetz Team
            """.formatted(user.getDisplayName(), resetUrl);
        
        emailClient.send(user.getEmail(), subject, body);
        
        // Log without sensitive data
        log.info("Password reset email sent to user: {}", user.getId());
        // ❌ NEVER: log.info("Reset token: {}", token);
    }
}
```

---

## Authorization & Roles

### Role Model

```java
public enum Role {
    ROLE_USER,      // Standard user - can learn, track progress
    ROLE_ADMIN,     // Admin - can manage words, view analytics
    ROLE_SUPER      // Super admin - can manage users, system config
}
```

### Database Schema

```sql
-- Add role to users table
ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'ROLE_USER';

-- Or use separate roles table for flexibility
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

### Endpoint Authorization Matrix

| Endpoint | ROLE_USER | ROLE_ADMIN | ROLE_SUPER |
|----------|-----------|------------|------------|
| `GET /api/users/me` | ✅ | ✅ | ✅ |
| `PUT /api/users/me` | ✅ | ✅ | ✅ |
| `GET /api/words/*` | ✅ | ✅ | ✅ |
| `GET /api/learning/*` | ✅ | ✅ | ✅ |
| `GET /api/progress/*` | ✅ | ✅ | ✅ |
| `POST /api/admin/words` | ❌ | ✅ | ✅ |
| `PUT /api/admin/words/*` | ❌ | ✅ | ✅ |
| `DELETE /api/admin/words/*` | ❌ | ✅ | ✅ |
| `GET /api/admin/users` | ❌ | ✅ | ✅ |
| `GET /api/admin/analytics` | ❌ | ✅ | ✅ |
| `PUT /api/admin/users/*/role` | ❌ | ❌ | ✅ |
| `DELETE /api/admin/users/*` | ❌ | ❌ | ✅ |
| `GET /api/admin/system/*` | ❌ | ❌ | ✅ |

### Security Configuration

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                
                // User endpoints
                .requestMatchers("/api/users/me/**").hasRole("USER")
                .requestMatchers("/api/words/**").hasRole("USER")
                .requestMatchers("/api/learning/**").hasRole("USER")
                .requestMatchers("/api/progress/**").hasRole("USER")
                
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Super admin endpoints
                .requestMatchers("/api/admin/users/*/role").hasRole("SUPER")
                .requestMatchers("/api/admin/system/**").hasRole("SUPER")
                
                // Default: deny
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

---

## Rate Limiting & Brute Force Protection

### Rate Limiting Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RATE LIMITING LAYERS                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Layer 1: Nginx (IP-based)                                                   │
│  ├── Auth endpoints: 5 req/min per IP                                        │
│  ├── API endpoints: 60 req/min per IP                                        │
│  └── Learning endpoints: 100 req/min per IP                                  │
│                                                                              │
│  Layer 2: Application (User-based)                                           │
│  ├── Login attempts: 5 failures → 15 min lockout                            │
│  ├── Password reset: 3 req/hour per email                                    │
│  └── API calls: Based on user tier                                          │
│                                                                              │
│  Layer 3: Account Protection                                                 │
│  ├── Failed logins logged with IP                                           │
│  ├── Suspicious activity alerts                                              │
│  └── Automatic account lockout after N failures                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Login Brute Force Protection

```java
@Service
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10);
    
    // In-memory cache (use Redis in production)
    private final LoadingCache<String, AtomicInteger> attemptsCache;
    private final LoadingCache<String, Instant> lockoutCache;
    
    /**
     * Record failed login attempt.
     */
    public void recordFailedAttempt(String email, String ipAddress) {
        String key = email + ":" + ipAddress;
        
        int attempts = attemptsCache.get(key).incrementAndGet();
        
        if (attempts >= MAX_ATTEMPTS) {
            // Lock account
            lockoutCache.put(key, Instant.now().plus(LOCKOUT_DURATION));
            
            // Log security event
            log.warn("SECURITY: Account locked due to {} failed attempts. Email: {}, IP: {}", 
                attempts, maskEmail(email), ipAddress);
            
            // Alert if distributed attack detected
            if (isDistributedAttack(email)) {
                securityAlertService.sendDistributedAttackAlert(email);
            }
        }
    }
    
    /**
     * Check if login is allowed.
     */
    public boolean isLoginAllowed(String email, String ipAddress) {
        String key = email + ":" + ipAddress;
        
        Instant lockoutUntil = lockoutCache.getIfPresent(key);
        if (lockoutUntil != null && lockoutUntil.isAfter(Instant.now())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Clear attempts after successful login.
     */
    public void recordSuccessfulLogin(String email, String ipAddress) {
        String key = email + ":" + ipAddress;
        attemptsCache.invalidate(key);
        lockoutCache.invalidate(key);
    }
    
    /**
     * Detect distributed brute force (same email, multiple IPs).
     */
    private boolean isDistributedAttack(String email) {
        long uniqueIps = attemptsCache.asMap().keySet().stream()
            .filter(k -> k.startsWith(email + ":"))
            .count();
        return uniqueIps >= 5; // 5+ different IPs trying same email
    }
}
```

### Login Endpoint with Protection

```java
@PostMapping("/login")
public ResponseEntity<?> login(
    @Valid @RequestBody LoginRequest request,
    HttpServletRequest httpRequest,
    HttpServletResponse response
) {
    String ipAddress = getClientIp(httpRequest);
    
    // Check lockout
    if (!loginAttemptService.isLoginAllowed(request.getEmail(), ipAddress)) {
        // SECURITY: Don't reveal lockout details
        throw new AuthenticationException("Invalid email or password");
    }
    
    try {
        // Authenticate
        User user = authService.authenticate(request.getEmail(), request.getPassword());
        
        // Clear failed attempts
        loginAttemptService.recordSuccessfulLogin(request.getEmail(), ipAddress);
        
        // Generate tokens...
        return ResponseEntity.ok(/* tokens */);
        
    } catch (BadCredentialsException e) {
        // Record failed attempt
        loginAttemptService.recordFailedAttempt(request.getEmail(), ipAddress);
        
        // SECURITY: Generic message (don't reveal if email exists)
        throw new AuthenticationException("Invalid email or password");
    }
}
```

### Nginx Rate Limiting Configuration

```nginx
# Rate limiting zones
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=5r/m;
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=60r/m;
limit_req_zone $binary_remote_addr zone=learning_limit:10m rate=100r/m;

server {
    # Auth endpoints - strict limit
    location /api/auth/ {
        limit_req zone=auth_limit burst=3 nodelay;
        limit_req_status 429;
        
        proxy_pass http://backend;
    }
    
    # Learning endpoints - higher limit
    location /api/learning/ {
        limit_req zone=learning_limit burst=20 nodelay;
        limit_req_status 429;
        
        proxy_pass http://backend;
    }
    
    # Other API endpoints
    location /api/ {
        limit_req zone=api_limit burst=10 nodelay;
        limit_req_status 429;
        
        proxy_pass http://backend;
    }
}
```

---

## Content Security Policy

### Production CSP (Strict)

```nginx
# Content Security Policy - NO unsafe-inline
add_header Content-Security-Policy "
    default-src 'self';
    script-src 'self';
    style-src 'self' 'nonce-${CSP_NONCE}';
    img-src 'self' data: https://cdn.vokabelnetz.com;
    font-src 'self' https://fonts.gstatic.com;
    connect-src 'self' https://api.vokabelnetz.com;
    frame-ancestors 'none';
    base-uri 'self';
    form-action 'self';
    upgrade-insecure-requests;
" always;
```

### CSP with Nonce for Angular

```java
@Component
public class CspNonceFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) throws ServletException, IOException {
        // Generate unique nonce per request
        String nonce = generateSecureNonce();
        request.setAttribute("cspNonce", nonce);
        
        // Set CSP header with nonce
        String csp = String.format(
            "default-src 'self'; " +
            "script-src 'self' 'nonce-%s'; " +
            "style-src 'self' 'nonce-%s'; " +
            "img-src 'self' data:; " +
            "font-src 'self'; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none';",
            nonce, nonce
        );
        
        response.setHeader("Content-Security-Policy", csp);
        chain.doFilter(request, response);
    }
    
    private String generateSecureNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
```

### Other Security Headers

```nginx
# Full security headers
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

# HSTS - Enable after confirming HTTPS works
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
```

---

## Logging & Privacy

### What to Log

```java
@Aspect
@Component
public class SecurityAuditLogger {
    
    /**
     * Log security-relevant events.
     */
    public void logSecurityEvent(SecurityEvent event) {
        // ✅ Safe to log
        log.info("SECURITY_EVENT: type={}, userId={}, ip={}, userAgent={}, timestamp={}",
            event.getType(),
            event.getUserId(),
            event.getIpAddress(),
            truncate(event.getUserAgent(), 100),
            event.getTimestamp()
        );
    }
}
```

### What NEVER to Log

```java
public class LogSanitizer {
    
    // Fields that must NEVER appear in logs
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password",
        "passwordHash",
        "currentPassword",
        "newPassword",
        "accessToken",
        "refreshToken",
        "token",
        "resetToken",
        "verifyToken",
        "secret",
        "apiKey",
        "creditCard",
        "ssn"
    );
    
    /**
     * Sanitize object before logging.
     */
    public static Map<String, Object> sanitize(Object obj) {
        Map<String, Object> map = objectMapper.convertValue(obj, Map.class);
        
        for (String field : SENSITIVE_FIELDS) {
            if (map.containsKey(field)) {
                map.put(field, "[REDACTED]");
            }
        }
        
        // Mask email
        if (map.containsKey("email")) {
            map.put("email", maskEmail((String) map.get("email")));
        }
        
        return map;
    }
    
    /**
     * Mask email: john.doe@example.com → j***e@e***.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "[INVALID]";
        
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) +
               "@" + domain.charAt(0) + "***" + domain.substring(domain.lastIndexOf('.'));
    }
}
```

### Request/Response Logging

```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) throws ServletException, IOException {
        // Log request (sanitized)
        log.info("REQUEST: method={}, uri={}, ip={}, userAgent={}",
            request.getMethod(),
            request.getRequestURI(),
            getClientIp(request),
            truncate(request.getHeader("User-Agent"), 100)
        );
        
        // ❌ NEVER log request body for auth endpoints
        if (request.getRequestURI().contains("/auth/")) {
            // Skip body logging
        }
        
        chain.doFilter(request, response);
        
        // Log response
        log.info("RESPONSE: status={}, duration={}ms",
            response.getStatus(),
            System.currentTimeMillis() - startTime
        );
    }
}
```

---

## Session Management

### Active Sessions UI Data

```java
@GetMapping("/me/sessions")
public ResponseEntity<?> getActiveSessions(@AuthenticationPrincipal UserDetails user) {
    List<RefreshToken> tokens = refreshTokenRepository
        .findActiveByUserId(user.getId());
    
    List<SessionInfo> sessions = tokens.stream()
        .map(t -> new SessionInfo(
            t.getId(),
            t.getDeviceInfo(),
            maskIpAddress(t.getIpAddress()),  // Mask for privacy
            t.getCreatedAt(),
            t.getId().equals(currentSessionId) // Mark current session
        ))
        .toList();
    
    return ResponseEntity.ok(ApiResponse.success(sessions));
}

@DeleteMapping("/me/sessions/{sessionId}")
public ResponseEntity<?> revokeSession(
    @AuthenticationPrincipal UserDetails user,
    @PathVariable Long sessionId
) {
    RefreshToken token = refreshTokenRepository.findById(sessionId)
        .filter(t -> t.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new NotFoundException("Session not found"));
    
    token.setRevoked(true);
    token.setRevokedAt(Instant.now());
    token.setRevokedReason("USER_REVOKED");
    refreshTokenRepository.save(token);
    
    return ResponseEntity.ok(ApiResponse.success("Session revoked"));
}
```

### Re-authentication for Sensitive Actions

```java
@PostMapping("/me/email")
public ResponseEntity<?> changeEmail(
    @AuthenticationPrincipal UserDetails userDetails,
    @Valid @RequestBody EmailChangeRequest request
) {
    User user = userService.findById(userDetails.getId());
    
    // REQUIRE re-authentication for email change
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
        throw new InvalidCredentialsException("Password verification failed");
    }
    
    // Proceed with email change...
    
    // Send notification to OLD email
    emailService.sendEmailChangedNotification(user.getEmail(), request.getNewEmail());
    
    // Revoke all sessions
    refreshTokenRepository.revokeAllByUserId(user.getId());
    
    return ResponseEntity.ok(ApiResponse.success("Email changed. Please log in again."));
}
```

---

## Data Retention Policy

### Retention Periods (GDPR/KVKK Compliance)

| Data Type | Retention Period | Purge Method | Notes |
|-----------|------------------|--------------|-------|
| **Access Logs** | 90 days | Automated cron | IP addresses, endpoints, response times |
| **Security Audit Logs** | 180 days | Automated cron | Login attempts, password changes, suspicious activity |
| **Refresh Tokens (active)** | 7 days | Auto-expire | Token expiration |
| **Refresh Tokens (revoked)** | 30 days | Automated cron | Retained for forensic analysis |
| **Password Reset Tokens** | 24 hours | Automated cron | Expired + used tokens |
| **Email Verification Tokens** | 7 days | Automated cron | Expired + used tokens |
| **Soft-deleted Users** | 30 days | Automated cron | Grace period before hard delete |
| **Password History** | Indefinite* | On user delete | *Deleted when user account is purged |
| **Learning Progress** | Indefinite | On user delete | Core user data |
| **Daily Statistics** | 2 years | Automated cron | Aggregated analytics |

### Cleanup Jobs

```sql
-- Run daily via cron at 3:00 AM

-- 1. Purge old access logs (90 days)
DELETE FROM access_logs WHERE created_at < NOW() - INTERVAL '90 days';

-- 2. Purge old security audit logs (180 days)
DELETE FROM security_audit_logs WHERE created_at < NOW() - INTERVAL '180 days';

-- 3. Purge revoked refresh tokens (30 days)
DELETE FROM refresh_tokens 
WHERE is_revoked = TRUE AND revoked_at < NOW() - INTERVAL '30 days';

-- 4. Purge expired/used password reset tokens (24 hours)
DELETE FROM password_reset_tokens 
WHERE expires_at < NOW() - INTERVAL '24 hours';

-- 5. Hard delete soft-deleted users (30 days grace period)
DELETE FROM users 
WHERE deleted_at IS NOT NULL AND deleted_at < NOW() - INTERVAL '30 days';
```

### Cron Configuration

```bash
# /etc/cron.d/vokabelnetz-retention

# Data retention cleanup - daily at 3:00 AM
0 3 * * * vokabelnetz /opt/vokabelnetz/scripts/data-retention-cleanup.sh >> /var/log/vokabelnetz/retention.log 2>&1
```

---

## Error Message Policy

### Account Enumeration Prevention

> **CRITICAL:** Error messages MUST NOT reveal whether an email exists in the system.

#### Login Errors

```java
// ✅ CORRECT: Generic message for ALL login failures
throw new AuthenticationException("Invalid email or password");

// ❌ WRONG: Reveals email existence
throw new AuthenticationException("Password is incorrect");
throw new AuthenticationException("Email not found");
throw new AuthenticationException("Account is locked");  // Reveals account exists
```

#### Registration Errors

```java
// ✅ CORRECT: If email exists, silently send "account exists" email instead of error
if (userRepository.existsByEmail(email)) {
    // Don't return error - send email to existing user
    emailService.sendAccountExistsNotification(email);
    // Return same success response as new registration
    return ResponseEntity.ok(ApiResponse.success("Please check your email to verify your account"));
}

// ❌ WRONG: Reveals email is already registered
throw new ConflictException("Email already registered");
```

#### Password Reset

```java
// ✅ CORRECT: Always show success (email sent only if account exists)
@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    // This method handles non-existent emails internally
    passwordResetService.createResetToken(request.getEmail(), httpRequest);
    
    // ALWAYS return success - don't reveal if email exists
    return ResponseEntity.ok(ApiResponse.success(
        "If an account exists with this email, you will receive a password reset link."
    ));
}
```

### Rate Limit Errors

```java
// ✅ CORRECT: Vague message, no details about limits
{
  "success": false,
  "error": {
    "code": "TOO_MANY_REQUESTS",
    "message": "Please wait a moment before trying again"
  }
}

// ❌ WRONG: Reveals rate limit details (helps attackers calibrate)
{
  "error": {
    "message": "Rate limit exceeded: 5 requests per minute. Try again in 45 seconds."
  }
}
```

### Account Lockout Errors

```java
// ✅ CORRECT: Same error as invalid credentials
// Don't reveal that account is locked
throw new AuthenticationException("Invalid email or password");

// ❌ WRONG: Reveals account is locked (confirms email exists)
throw new AuthenticationException("Account locked due to too many failed attempts");
```

### Summary Table

| Scenario | Correct Response | Wrong Response |
|----------|------------------|----------------|
| Wrong password | "Invalid email or password" | "Password is incorrect" |
| Email not found | "Invalid email or password" | "Email not found" |
| Account locked | "Invalid email or password" | "Account is locked" |
| Email already registered | Success + email notification | "Email already exists" |
| Password reset (any email) | "Check your email if account exists" | "Email not found" |
| Rate limit hit | "Please wait before trying again" | "5 req/min limit exceeded" |

---

## Security Checklist

### Pre-Launch Checklist

```
Authentication & Authorization
□ JWT access token stored in memory only (not localStorage)
□ Refresh token in HttpOnly, Secure, SameSite=Strict cookie
□ Refresh token rotation on every use
□ Token reuse detection implemented
□ All refresh tokens revoked on password change
□ All refresh tokens revoked on logout-all
□ Role-based access control configured
□ Endpoint authorization matrix reviewed

Password Security  
□ Argon2id (or BCrypt cost≥12) configured
□ Password policy enforced (min 10 chars, complexity)
□ Common password blacklist active
□ Password history check prevents reuse
□ Reset tokens are hashed, single-use, short-lived

Rate Limiting & Brute Force
□ IP-based rate limits at Nginx level
□ User-based login attempt limits
□ Account lockout after N failures
□ Distributed attack detection
□ Password reset rate limited

Headers & CSP
□ CSP without unsafe-inline (use nonce if needed)
□ X-Frame-Options: DENY
□ X-Content-Type-Options: nosniff
□ HSTS enabled (after HTTPS confirmed)
□ Referrer-Policy configured

Logging & Privacy
□ Passwords NEVER logged
□ Tokens NEVER logged  
□ Emails masked in logs
□ Security events logged with context
□ GDPR-compliant data retention

Session Management
□ Active sessions visible to user
□ Single session revocation works
□ "Logout all" functionality works
□ Re-auth required for sensitive changes
```

---

## Reporting Security Issues

If you discover a security vulnerability, please report it responsibly:

1. **DO NOT** create a public GitHub issue
2. Email: security@vokabelnetz.com
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes

We will respond within 48 hours and work with you to address the issue.
