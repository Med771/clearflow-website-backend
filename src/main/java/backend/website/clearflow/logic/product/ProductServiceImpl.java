package backend.website.clearflow.logic.product;

import backend.website.clearflow.helper.AesGcmCipherHelper;
import backend.website.clearflow.logic.product.dto.CreateProductRequest;
import backend.website.clearflow.logic.product.dto.ProductResponse;
import backend.website.clearflow.logic.product.dto.UpdateProductRequest;
import backend.website.clearflow.logic.product.ozon.OzonProductPicturesClient;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final AuthContextService authContextService;
    private final UserRepository userRepository;
    private final AesGcmCipherHelper aesGcmCipherHelper;
    private final OzonProductPicturesClient ozonProductPicturesClient;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(UUID sellerId, String search, boolean includeInactive, Pageable pageable) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        Specification<ProductEntity> specification = Specification.where(bySeller(targetSellerId))
                .and(searchByName(search))
                .and(activeFilter(includeInactive));
        Page<ProductEntity> page = productRepository.findAll(specification, pageable);
        Map<Long, String> photoLinks = resolvePhotoLinks(targetSellerId, page.getContent());
        List<ProductResponse> responses = page.getContent().stream()
                .map(product -> withPhoto(productMapper.toResponse(product), photoLinks.get(product.getOzonProductId())))
                .toList();
        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        UserEntity actor = authContextService.currentActorOrThrow();
        ProductEntity product = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        validateSellerScope(actor, product.getSellerId());
        Map<Long, String> photoLinks = resolvePhotoLinks(product.getSellerId(), List.of(product));
        return withPhoto(productMapper.toResponse(product), photoLinks.get(product.getOzonProductId()));
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, request.sellerId());
        if (productRepository.existsBySellerIdAndOzonProductId(targetSellerId, request.ozonProductId())) {
            throw new BadRequestException("Product with this ozonProductId already exists for seller");
        }
        String normalizedName = request.name().trim();
        if (normalizedName.isEmpty()) {
            throw new BadRequestException("name cannot be blank");
        }
        ProductEntity entity = new ProductEntity();
        entity.setSellerId(targetSellerId);
        entity.setName(normalizedName);
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
            String normalized = request.name().trim();
            if (normalized.isEmpty()) {
                throw new BadRequestException("name cannot be blank");
            }
            entity.setName(normalized);
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
            return;
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

    private ProductResponse withPhoto(ProductResponse response, String photoUrl) {
        return new ProductResponse(
                response.id(),
                response.sellerId(),
                response.name(),
                response.ozonProductId(),
                response.isActive(),
                photoUrl,
                response.creatorId(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private Map<Long, String> resolvePhotoLinks(UUID sellerId, List<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        UserEntity seller = userRepository.findById(sellerId).orElse(null);
        if (seller == null || seller.getOzonApiKeyCiphertext() == null || seller.getOzonApiKeyCiphertext().isBlank()) {
            return Map.of();
        }
        String clientId = seller.getOzonClientId();
        if (clientId == null || clientId.isBlank()) {
            return Map.of();
        }
        String apiKey;
        try {
            apiKey = aesGcmCipherHelper.decrypt(seller.getOzonApiKeyCiphertext());
        } catch (Exception exception) {
            return Map.of();
        }
        Set<Long> ozonProductIds = products.stream()
                .map(ProductEntity::getOzonProductId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (ozonProductIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> links = ozonProductPicturesClient.fetchPrimaryPhotoLinks(ozonProductIds, clientId, apiKey);
        return links == null ? new HashMap<>() : links;
    }
}
