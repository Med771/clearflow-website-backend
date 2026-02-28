package backend.website.clearflow.logic.user;

import backend.website.clearflow.helper.AesGcmCipherHelper;
import backend.website.clearflow.logic.profile.SellerProfileEntity;
import backend.website.clearflow.logic.profile.SellerProfileRepository;
import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import backend.website.clearflow.logic.user.dto.CreateUserRequest;
import backend.website.clearflow.logic.user.dto.UpdateUserRequest;
import backend.website.clearflow.logic.user.dto.UserResponse;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.ForbiddenException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserAccessPolicy userAccessPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AuthContextService authContextService;
    private final AesGcmCipherHelper aesGcmCipherHelper;
    private final SellerProfileRepository sellerProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String search, Set<UserRole> roles, UUID parentId, boolean includeInactive, Pageable pageable) {
        UserEntity actor = authContextService.currentActorOrThrow();
        ensureSellerApproved(actor);
        if (!userAccessPolicy.canViewAny(actor.getRole())) {
            return Page.empty(pageable);
        }
        Specification<UserEntity> specification = Specification.allOf(
                UserSpecification.visibleFor(actor),
                UserSpecification.search(search),
                UserSpecification.roleIn(roles),
                UserSpecification.parent(parentId),
                UserSpecification.activeOnly(includeInactive)
        );
        return userRepository.findAll(specification, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        ensureSellerApproved(actor);
        validateCreatePermission(actor, request.role());

        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        UserEntity entity = new UserEntity();
        entity.setEmail(request.email().trim().toLowerCase());
        entity.setPassword(passwordEncoder.encode(request.password()));
        entity.setRole(request.role());
        entity.setBlock(false);
        entity.setActive(true);
        entity.setSessionVersion(0);
        entity.setParentId(resolveParentId(actor, request));
        applyOzonCredentials(entity, request.role(), request.ozonClientId(), request.ozonApiKey());
        UserEntity saved = userRepository.save(entity);
        initializeSellerProfileForManagedCreation(saved, actor);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        ensureSellerApproved(actor);
        UserEntity target = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        validateManagePermission(actor, target);
        validateOwnerMutation(target);

        if (request.email() != null && !request.email().isBlank()) {
            target.setEmail(request.email().trim().toLowerCase());
        }
        if (request.password() != null && !request.password().isBlank()) {
            target.setPassword(passwordEncoder.encode(request.password()));
            target.setSessionVersion(target.getSessionVersion() + 1);
        }
        if (request.isBlock() != null) {
            target.setBlock(request.isBlock());
            target.setSessionVersion(target.getSessionVersion() + 1);
        }
        if (request.isActive() != null) {
            target.setActive(request.isActive());
            if (!request.isActive()) {
                target.setSessionVersion(target.getSessionVersion() + 1);
            }
        }
        return userMapper.toResponse(userRepository.save(target));
    }

    @Override
    @Transactional
    public UserResponse softDeleteUser(UUID userId) {
        UserEntity actor = authContextService.currentActorOrThrow();
        ensureSellerApproved(actor);
        UserEntity target = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        validateManagePermission(actor, target);
        validateOwnerMutation(target);

        target.setActive(false);
        target.setSessionVersion(target.getSessionVersion() + 1);
        return userMapper.toResponse(userRepository.save(target));
    }

    private void validateCreatePermission(UserEntity actor, UserRole targetRole) {
        if (!userAccessPolicy.canCreate(actor.getRole(), targetRole)) {
            throw new ForbiddenException("Role is not allowed to create this user");
        }
    }

    private void validateManagePermission(UserEntity actor, UserEntity target) {
        if (!userAccessPolicy.canManage(actor.getRole(), target.getRole())) {
            throw new ForbiddenException("You cannot manage this role");
        }
        if (userAccessPolicy.isInScope(actor, target)) {
            throw new ForbiddenException("Target user is outside of your scope");
        }
    }

    private void validateOwnerMutation(UserEntity target) {
        if (target.getRole() == UserRole.OWNER) {
            throw new ForbiddenException("Owner cannot be modified from this endpoint");
        }
    }

    private UUID resolveParentId(UserEntity actor, CreateUserRequest request) {
        if (request.role() == UserRole.OWNER) {
            return null;
        }
        UUID parentId = request.parentId() != null ? request.parentId() : actor.getId();
        UserEntity parent = userRepository.findById(parentId).orElseThrow(() -> new BadRequestException("Parent user not found"));
        if (!isValidParentForRole(parent.getRole(), request.role())) {
            throw new BadRequestException("Invalid parent for role " + request.role());
        }
        if (!Objects.equals(parent.getId(), actor.getId()) && userAccessPolicy.isInScope(actor, parent) && actor.getRole() != UserRole.OWNER) {
            throw new ForbiddenException("Parent user is outside of your scope");
        }
        return parent.getId();
    }

    private boolean isValidParentForRole(UserRole parentRole, UserRole targetRole) {
        return switch (targetRole) {
            case OWNER -> false;
            case ADMIN -> parentRole == UserRole.OWNER;
            case SELLER -> parentRole == UserRole.ADMIN || parentRole == UserRole.OWNER;
            case MANAGER -> parentRole == UserRole.SELLER || parentRole == UserRole.ADMIN;
        };
    }

    private void applyOzonCredentials(UserEntity entity, UserRole role, String ozonClientId, String ozonApiKey) {
        if (role != UserRole.SELLER) {
            entity.setOzonClientId(null);
            entity.setOzonApiKeyCiphertext(null);
            entity.setOzonApiKeyKeyVersion(null);
            return;
        }
        if (ozonClientId != null && !ozonClientId.isBlank()) {
            entity.setOzonClientId(ozonClientId.trim());
        }
        if (ozonApiKey == null || ozonApiKey.isBlank()) {
            return;
        }
        entity.setOzonApiKeyCiphertext(aesGcmCipherHelper.encrypt(ozonApiKey.trim()));
        entity.setOzonApiKeyKeyVersion(aesGcmCipherHelper.currentKeyVersion());
    }

    private void ensureSellerApproved(UserEntity actor) {
        if (actor.getRole() != UserRole.SELLER) {
            return;
        }
        SellerProfileEntity profile = sellerProfileRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new ForbiddenException("Seller profile is not initialized"));
        if (profile.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new ForbiddenException("Seller profile is not verified yet");
        }
    }

    private void initializeSellerProfileForManagedCreation(UserEntity user, UserEntity actor) {
        if (user.getRole() != UserRole.SELLER) {
            return;
        }
        SellerProfileEntity profile = sellerProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            SellerProfileEntity newProfile = new SellerProfileEntity();
            newProfile.setUserId(user.getId());
            return newProfile;
        });
        profile.setVerificationStatus(SellerVerificationStatus.APPROVED);
        profile.setVerificationComment("Profile approved on managed creation");
        profile.setVerificationSubmittedAt(java.time.Instant.now());
        profile.setVerifiedAt(java.time.Instant.now());
        profile.setVerifiedBy(actor.getId());
        sellerProfileRepository.save(profile);
    }
}
