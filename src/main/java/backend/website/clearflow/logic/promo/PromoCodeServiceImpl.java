package backend.website.clearflow.logic.promo;

import backend.website.clearflow.logic.product.ProductEntity;
import backend.website.clearflow.logic.product.ProductRepository;
import backend.website.clearflow.logic.promo.dto.CreatePromoCodeRequest;
import backend.website.clearflow.logic.promo.dto.PromoCodeResponse;
import backend.website.clearflow.logic.promo.dto.UpdatePromoCodeRequest;
import backend.website.clearflow.logic.promo.product.PromoCodeProductEntity;
import backend.website.clearflow.logic.promo.product.PromoCodeProductId;
import backend.website.clearflow.logic.promo.product.PromoCodeProductRepository;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.ForbiddenException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromoCodeServiceImpl implements PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeProductRepository promoCodeProductRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthContextService authContextService;

    @Override
    @Transactional(readOnly = true)
    public Page<PromoCodeResponse> list(UUID sellerId, String search, boolean includeInactive, Pageable pageable) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        Specification<PromoCodeEntity> specification = Specification.where(bySeller(targetSellerId))
                .and(searchByName(search))
                .and(activeFilter(includeInactive));
        return promoCodeRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PromoCodeResponse getById(UUID id) {
        UserEntity actor = authContextService.currentActorOrThrow();
        PromoCodeEntity entity = promoCodeRepository.findById(id).orElseThrow(() -> new NotFoundException("Promo code not found"));
        validateSellerScope(actor, entity.getSellerId());
        return toResponse(entity);
    }

    @Override
    @Transactional
    public PromoCodeResponse create(CreatePromoCodeRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, request.sellerId());
        if (promoCodeRepository.existsBySellerIdAndActionId(targetSellerId, request.actionId())) {
            throw new BadRequestException("Promo code with this actionId already exists for seller");
        }
        PromoCodeEntity entity = new PromoCodeEntity();
        entity.setSellerId(targetSellerId);
        entity.setName(request.name().trim());
        entity.setActionId(request.actionId());
        entity.setActive(true);
        PromoCodeEntity saved = promoCodeRepository.save(entity);
        replaceProducts(saved.getId(), saved.getSellerId(), request.productIds());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PromoCodeResponse update(UUID id, UpdatePromoCodeRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        PromoCodeEntity entity = promoCodeRepository.findById(id).orElseThrow(() -> new NotFoundException("Promo code not found"));
        validateSellerScope(actor, entity.getSellerId());
        if (request.name() != null) {
            entity.setName(request.name().trim());
        }
        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }
        PromoCodeEntity saved = promoCodeRepository.save(entity);
        if (request.productIds() != null) {
            replaceProducts(saved.getId(), saved.getSellerId(), request.productIds());
        }
        return toResponse(saved);
    }

    private PromoCodeResponse toResponse(PromoCodeEntity entity) {
        List<UUID> productIds = promoCodeProductRepository.findAllByPromoCodeId(entity.getId())
                .stream()
                .map(PromoCodeProductEntity::getProductId)
                .toList();
        return new PromoCodeResponse(
                entity.getId(),
                entity.getSellerId(),
                entity.getName(),
                entity.getActionId(),
                entity.isActive(),
                productIds,
                entity.getCreatorId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void replaceProducts(UUID promoCodeId, UUID sellerId, List<UUID> productIds) {
        promoCodeProductRepository.deleteAllByPromoCodeId(promoCodeId);
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<PromoCodeProductEntity> links = new ArrayList<>();
        for (UUID productId : productIds) {
            ProductEntity product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            if (!product.getSellerId().equals(sellerId)) {
                throw new BadRequestException("Product does not belong to promo seller");
            }
            PromoCodeProductEntity link = new PromoCodeProductEntity();
            link.setId(new PromoCodeProductId(promoCodeId, productId));
            links.add(link);
        }
        promoCodeProductRepository.saveAll(links);
    }

    private UUID resolveTargetSellerId(UserEntity actor, UUID requestedSellerId) {
        if (actor.getRole() == UserRole.SELLER) {
            return actor.getId();
        }
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.OWNER) {
            if (requestedSellerId == null) {
                throw new BadRequestException("sellerId is required for admin/owner");
            }
            UserEntity seller = userRepository.findById(requestedSellerId).orElseThrow(() -> new NotFoundException("Seller not found"));
            if (seller.getRole() != UserRole.SELLER) {
                throw new BadRequestException("sellerId must refer to seller role");
            }
            if (actor.getRole() == UserRole.ADMIN
                    && (seller.getParentId() == null || !seller.getParentId().equals(actor.getId()))) {
                throw new ForbiddenException("Seller is outside of your scope");
            }
            return requestedSellerId;
        }
        throw new ForbiddenException("Role cannot manage promo codes");
    }

    private void validateSellerScope(UserEntity actor, UUID sellerId) {
        if (actor.getRole() == UserRole.OWNER) {
            return;
        }
        if (actor.getRole() == UserRole.SELLER && actor.getId().equals(sellerId)) {
            return;
        }
        if (actor.getRole() == UserRole.ADMIN) {
            UserEntity seller = userRepository.findById(sellerId).orElseThrow(() -> new NotFoundException("Seller not found"));
            if (seller.getParentId() != null && seller.getParentId().equals(actor.getId())) {
                return;
            }
        }
        throw new ForbiddenException("Seller is outside of your scope");
    }

    private Specification<PromoCodeEntity> bySeller(UUID sellerId) {
        return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
    }

    private Specification<PromoCodeEntity> searchByName(String search) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    private Specification<PromoCodeEntity> activeFilter(boolean includeInactive) {
        if (includeInactive) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
