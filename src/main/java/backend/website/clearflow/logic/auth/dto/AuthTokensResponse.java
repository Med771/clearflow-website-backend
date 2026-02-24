package backend.website.clearflow.logic.auth.dto;

import backend.website.clearflow.model.UserRole;

import java.util.UUID;

public record AuthTokensResponse(
        UUID userId,
        String email,
        UserRole role,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds
) {
}
