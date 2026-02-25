package backend.website.clearflow.logic.profile;

import backend.website.clearflow.logic.profile.dto.MyProfileResponse;
import backend.website.clearflow.logic.profile.dto.UpdateMyProfileRequest;
import backend.website.clearflow.logic.profile.photo.UserPhotoRepository;
import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import backend.website.clearflow.helper.FileStorageService;
import backend.website.clearflow.logic.auth.RefreshSessionEntity;
import backend.website.clearflow.logic.auth.RefreshSessionRepository;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.ForbiddenException;
import backend.website.clearflow.security.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final AuthContextService authContextService;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final FileStorageService fileStorageService;
    private final RefreshSessionRepository refreshSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile() {
        UserEntity actor = authContextService.currentActiveActorAllowBlockedOrThrow();
        SellerProfileEntity sellerProfile = actor.getRole() == UserRole.SELLER
                ? sellerProfileRepository.findByUserId(actor.getId()).orElse(null)
                : null;
        return toResponse(actor, sellerProfile);
    }

    @Override
    @Transactional
    public MyProfileResponse updateMyProfile(UpdateMyProfileRequest request) {
        UserEntity actor = authContextService.currentActiveActorAllowBlockedOrThrow();
        if (actor.getRole() != UserRole.SELLER) {
            throw new BadRequestException("Bank details are available only for seller profile");
        }

        SellerProfileEntity profile = sellerProfileRepository.findByUserId(actor.getId()).orElseGet(() -> {
            SellerProfileEntity newProfile = new SellerProfileEntity();
            newProfile.setUserId(actor.getId());
            newProfile.setVerificationStatus(SellerVerificationStatus.PENDING);
            newProfile.setVerificationComment("Profile is waiting for admin review");
            newProfile.setVerificationSubmittedAt(Instant.now());
            return newProfile;
        });

        if (request.fullName() != null) {
            profile.setFullName(normalizeText(request.fullName()));
        }
        if (request.contactPhone() != null) {
            profile.setContactPhone(normalizeText(request.contactPhone()));
        }
        if (request.companyName() != null) {
            profile.setCompanyName(normalizeText(request.companyName()));
        }
        if (request.bankName() != null) {
            profile.setBankName(normalizeText(request.bankName()));
        }
        if (request.inn() != null) {
            profile.setInn(normalizeDigits(request.inn()));
        }
        if (request.bik() != null) {
            profile.setBik(normalizeDigits(request.bik()));
        }
        if (request.settlementAccount() != null) {
            profile.setSettlementAccount(normalizeDigits(request.settlementAccount()));
        }
        if (request.corporateAccount() != null) {
            profile.setCorporateAccount(normalizeDigits(request.corporateAccount()));
        }
        if (request.address() != null) {
            profile.setAddress(normalizeText(request.address()));
        }
        if (request.ozonSellerLink() != null) {
            profile.setOzonSellerLink(normalizeText(request.ozonSellerLink()));
        }

        if (profile.getVerificationStatus() == SellerVerificationStatus.REJECTED) {
            profile.setVerificationStatus(SellerVerificationStatus.PENDING);
            profile.setVerificationComment("Profile was resubmitted and is waiting for admin review");
            profile.setVerificationSubmittedAt(Instant.now());
            profile.setVerifiedAt(null);
            profile.setVerifiedBy(null);
        }

        SellerProfileEntity saved = sellerProfileRepository.save(profile);
        return toResponse(actor, saved);
    }

    @Override
    @Transactional
    public void deleteMyProfile() {
        UserEntity actor = authContextService.currentActiveActorAllowBlockedOrThrow();
        if (actor.getRole() == UserRole.OWNER) {
            throw new ForbiddenException("Owner cannot delete own profile");
        }

        actor.setActive(false);
        actor.setSessionVersion(actor.getSessionVersion() + 1);
        userRepository.save(actor);

        List<RefreshSessionEntity> sessions = refreshSessionRepository.findByUserIdAndRevokedAtIsNull(actor.getId());
        Instant now = Instant.now();
        sessions.forEach(session -> session.setRevokedAt(now));
        refreshSessionRepository.saveAll(sessions);

        userPhotoRepository.findByUserId(actor.getId()).ifPresent(photo -> {
            fileStorageService.delete(photo.getRelativePath());
            userPhotoRepository.delete(photo);
        });
    }

    private MyProfileResponse toResponse(UserEntity actor, SellerProfileEntity profile) {
        boolean hasPhoto = userPhotoRepository.findByUserId(actor.getId()).isPresent();
        return new MyProfileResponse(
                actor.getId(),
                actor.getEmail(),
                actor.getRole(),
                actor.isBlock(),
                actor.isActive(),
                actor.getOzonApiKeyCiphertext() != null && !actor.getOzonApiKeyCiphertext().isBlank(),
                hasPhoto,
                actor.getParentId(),
                actor.getCreatorId(),
                actor.getCreatedAt(),
                actor.getUpdatedAt(),
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getContactPhone() : null,
                profile != null ? profile.getCompanyName() : null,
                profile != null ? profile.getBankName() : null,
                profile != null ? profile.getInn() : null,
                profile != null ? profile.getBik() : null,
                profile != null ? profile.getSettlementAccount() : null,
                profile != null ? profile.getCorporateAccount() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getOzonSellerLink() : null,
                profile != null ? profile.getVerificationStatus() : null,
                profile != null ? profile.getVerificationComment() : null
        );
    }

    private String normalizeText(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDigits(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
