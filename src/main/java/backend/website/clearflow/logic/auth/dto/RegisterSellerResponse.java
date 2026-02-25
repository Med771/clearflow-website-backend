package backend.website.clearflow.logic.auth.dto;

import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import backend.website.clearflow.model.UserRole;

import java.util.UUID;

public record RegisterSellerResponse(
        UUID userId,
        String email,
        UserRole role,
        SellerVerificationStatus verificationStatus,
        String message
) {
}
