package backend.website.clearflow.logic.profile.verification;

import backend.website.clearflow.logic.profile.SellerProfileEntity;
import backend.website.clearflow.logic.profile.SellerProfileRepository;
import backend.website.clearflow.logic.profile.verification.dto.SellerVerificationResponse;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.ForbiddenException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerVerificationServiceImpl implements SellerVerificationService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final AuthContextService authContextService;
    private final SellerVerificationMapper sellerVerificationMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<SellerVerificationResponse> list(SellerVerificationStatus status, String search, Pageable pageable) {
        requireReviewer();
        Page<SellerProfileEntity> page = sellerProfileRepository.findAll(Specification.where(statusFilter(status)).and(searchFilter(search)), pageable);
        Map<UUID, UserEntity> usersById = userRepository.findAllById(
                        page.getContent().stream().map(SellerProfileEntity::getUserId).toList()
                ).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return new PageImpl<>(
                page.getContent().stream().map(profile -> toResponse(profile, usersById)).toList(),
                pageable,
                page.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SellerVerificationResponse getByUserId(UUID userId) {
        requireReviewer();
        SellerProfileEntity profile = findProfileOrThrow(userId);
        UserEntity user = findUserOrThrow(userId);
        return sellerVerificationMapper.toResponse(user, profile);
    }

    @Override
    @Transactional
    public SellerVerificationResponse approve(UUID userId) {
        UserEntity reviewer = requireReviewer();
        SellerProfileEntity profile = findProfileOrThrow(userId);
        UserEntity seller = ensureSellerRole(userId);
        profile.setVerificationStatus(SellerVerificationStatus.APPROVED);
        profile.setVerificationComment("Profile approved");
        profile.setVerifiedAt(Instant.now());
        profile.setVerifiedBy(reviewer.getId());
        return sellerVerificationMapper.toResponse(seller, sellerProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public SellerVerificationResponse reject(UUID userId, String comment) {
        UserEntity reviewer = requireReviewer();
        SellerProfileEntity profile = findProfileOrThrow(userId);
        UserEntity seller = ensureSellerRole(userId);
        profile.setVerificationStatus(SellerVerificationStatus.REJECTED);
        profile.setVerificationComment(comment.trim());
        profile.setVerifiedAt(Instant.now());
        profile.setVerifiedBy(reviewer.getId());
        return sellerVerificationMapper.toResponse(seller, sellerProfileRepository.save(profile));
    }

    private UserEntity requireReviewer() {
        UserEntity actor = authContextService.currentActorOrThrow();
        if (actor.getRole() != UserRole.OWNER && actor.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only owner/admin can verify seller profiles");
        }
        return actor;
    }

    private UserEntity ensureSellerRole(UUID userId) {
        UserEntity user = findUserOrThrow(userId);
        if (user.getRole() != UserRole.SELLER) {
            throw new ForbiddenException("Verification is available only for seller role");
        }
        return user;
    }

    private SellerProfileEntity findProfileOrThrow(UUID userId) {
        return sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Seller profile not found"));
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private SellerVerificationResponse toResponse(SellerProfileEntity profile, Map<UUID, UserEntity> usersById) {
        UserEntity user = usersById.get(profile.getUserId());
        if (user == null) {
            throw new NotFoundException("Seller user not found");
        }
        return sellerVerificationMapper.toResponse(user, profile);
    }

    private Specification<SellerProfileEntity> statusFilter(SellerVerificationStatus status) {
        if (status == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("verificationStatus"), status);
    }

    private Specification<SellerProfileEntity> searchFilter(String search) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(cb.lower(root.get("companyName")), pattern),
                cb.like(cb.lower(root.get("inn")), pattern),
                cb.like(cb.lower(root.get("contactPhone")), pattern)
        );
    }
}
