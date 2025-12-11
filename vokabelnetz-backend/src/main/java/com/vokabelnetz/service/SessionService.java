package com.vokabelnetz.service;

import com.vokabelnetz.dto.response.SessionResponse;
import com.vokabelnetz.entity.RefreshToken;
import com.vokabelnetz.exception.ResourceNotFoundException;
import com.vokabelnetz.repository.RefreshTokenRepository;
import com.vokabelnetz.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Session management service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    /**
     * Get all active sessions for a user.
     */
    public List<SessionResponse> getActiveSessions(Long userId, String currentAccessToken) {
        List<RefreshToken> activeSessions = refreshTokenRepository.findActiveByUserId(userId, Instant.now());

        // Get current user ID from token to mark current session
        Long currentUserId = null;
        if (currentAccessToken != null) {
            try {
                currentUserId = jwtService.extractUserId(currentAccessToken);
            } catch (Exception e) {
                log.debug("Could not extract user ID from token");
            }
        }

        final Long finalCurrentUserId = currentUserId;

        return activeSessions.stream()
            .map(token -> mapToSessionResponse(token, finalCurrentUserId))
            .toList();
    }

    /**
     * Revoke a specific session.
     */
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        // Verify the session belongs to the user
        if (!token.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Session", sessionId);
        }

        // Verify it's not already revoked
        if (Boolean.TRUE.equals(token.getIsRevoked())) {
            return; // Already revoked
        }

        token.setIsRevoked(true);
        token.setRevokedAt(Instant.now());
        token.setRevokedReason("USER_REVOKED");
        refreshTokenRepository.save(token);

        log.info("Session {} revoked by user {}", sessionId, userId);
    }

    private SessionResponse mapToSessionResponse(RefreshToken token, Long currentUserId) {
        // Parse user agent to get device info
        String deviceInfo = parseUserAgent(token.getUserAgent());

        // Mask IP address for privacy (show only first two octets)
        String maskedIp = maskIpAddress(token.getIpAddress());

        // Determine if this is the current session
        boolean isCurrent = token.getUser().getId().equals(currentUserId);

        return SessionResponse.builder()
            .id(token.getId())
            .deviceInfo(deviceInfo)
            .ipAddress(maskedIp)
            .createdAt(token.getCreatedAt())
            .lastUsedAt(token.getCreatedAt()) // Using createdAt since we don't track lastUsed
            .isCurrent(isCurrent)
            .build();
    }

    private String parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }

        // Simple parsing - could be enhanced with a library
        String browser = "Unknown";
        String os = "Unknown";

        if (userAgent.contains("Chrome")) browser = "Chrome";
        else if (userAgent.contains("Firefox")) browser = "Firefox";
        else if (userAgent.contains("Safari")) browser = "Safari";
        else if (userAgent.contains("Edge")) browser = "Edge";

        if (userAgent.contains("Windows")) os = "Windows";
        else if (userAgent.contains("Mac")) os = "Mac";
        else if (userAgent.contains("Linux")) os = "Linux";
        else if (userAgent.contains("iPhone")) os = "iPhone";
        else if (userAgent.contains("Android")) os = "Android";

        return browser + " on " + os;
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null) return "***";

        // For IPv4, show first two octets
        if (ipAddress.contains(".")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }

        // For IPv6, mask most of it
        return ipAddress.substring(0, Math.min(6, ipAddress.length())) + "***";
    }
}
