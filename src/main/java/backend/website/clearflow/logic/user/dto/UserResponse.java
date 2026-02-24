package backend.website.clearflow.logic.user.dto;

import backend.website.clearflow.model.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        UserRole role,
        boolean isBlock,
        boolean isActive,
        boolean hasOzonApiKey,
        UUID parentId,
        UUID creatorId,
        Instant createdAt,
        Instant updatedAt
) {
}
