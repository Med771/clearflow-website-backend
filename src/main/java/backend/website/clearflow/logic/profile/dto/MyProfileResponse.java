package backend.website.clearflow.logic.profile.dto;

import backend.website.clearflow.model.UserRole;

import java.time.Instant;
import java.util.UUID;

public record MyProfileResponse(
        UUID id,
        String email,
        UserRole role,
        boolean isBlock,
        boolean isActive,
        boolean hasOzonApiKey,
        boolean hasPhoto,
        UUID parentId,
        UUID creatorId,
        Instant createdAt,
        Instant updatedAt,
        String companyName,
        String bankName,
        String inn,
        String bik,
        String settlementAccount,
        String corporateAccount,
        String address
) {
}
