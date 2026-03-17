package com.hackathon.finance.dto.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt,
        UserSummary user
) {
    public record UserSummary(UUID id, String email, String displayName) {
    }
}
