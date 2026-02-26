package backend.website.clearflow.logic.product;

import backend.website.clearflow.logic.product.dto.CreateProductRequest;
import backend.website.clearflow.logic.product.dto.ProductResponse;
import backend.website.clearflow.logic.product.dto.UpdateProductRequest;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final AuthContextService authContextService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(UUID sellerId, String search, boolean includeInactive, Pageable pageable) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        Specification<ProductEntity> specification = Specification.where(bySeller(targetSellerId))
                .and(searchByName(search))
                .and(activeFilter(includeInactive));
        return productRepository.findAll(specification, pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        UserEntity actor = authContextService.currentActorOrThrow();
        ProductEntity product = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        validateSellerScope(actor, product.getSellerId());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, request.sellerId());
        if (productRepository.existsBySellerIdAndOzonProductId(targetSellerId, request.ozonProductId())) {
            throw new BadRequestException("Product with this ozonProductId already exists for seller");
        }
        ProductEntity entity = new ProductEntity();
        entity.setSellerId(targetSellerId);
        entity.setName(request.name().trim());
        entity.setOzonProductId(request.ozonProductId());
        entity.setActive(true);
        return productMapper.toResponse(productRepository.save(entity));
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        ProductEntity entity = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        validateSellerScope(actor, entity.getSellerId());
        if (request.name() != null) {
            entity.setName(request.name().trim());
        }
        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }
        return productMapper.toResponse(productRepository.save(entity));
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
            if (actor.getRole() == UserRole.ADMIN && !requestedSellerId.equals(actor.getId()) && !requestedSellerId.equals(seller.getId())) {
                // Admin scope is validated by creator/parent chain in existing user policies.
                if (!(seller.getParentId() != null && seller.getParentId().equals(actor.getId()))) {
                    throw new ForbiddenException("Seller is outside of your scope");
                }
            }
            return requestedSellerId;
        }
        throw new ForbiddenException("Role cannot manage products");
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

    private Specification<ProductEntity> bySeller(UUID sellerId) {
        return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
    }

    private Specification<ProductEntity> searchByName(String search) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    private Specification<ProductEntity> activeFilter(boolean includeInactive) {
        if (includeInactive) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
