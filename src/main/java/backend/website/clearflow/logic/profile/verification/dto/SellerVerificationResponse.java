package backend.website.clearflow.logic.profile.verification.dto;

import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record SellerVerificationResponse(
        UUID userId,
        String email,
        String fullName,
        String contactPhone,
        String companyName,
        String inn,
        String ozonSellerLink,
        SellerVerificationStatus verificationStatus,
        String verificationComment,
        Instant verificationSubmittedAt,
        Instant verifiedAt,
        UUID verifiedBy
) {
}
