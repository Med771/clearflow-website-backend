package backend.website.clearflow.logic.profile.verification;

import backend.website.clearflow.logic.profile.verification.dto.SellerVerificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SellerVerificationService {
    Page<SellerVerificationResponse> list(SellerVerificationStatus status, String search, Pageable pageable);

    SellerVerificationResponse getByUserId(UUID userId);

    SellerVerificationResponse approve(UUID userId);

    SellerVerificationResponse reject(UUID userId, String comment);
}
