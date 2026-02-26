package backend.website.clearflow.logic.stat;

import backend.website.clearflow.logic.product.ProductEntity;
import backend.website.clearflow.logic.product.ProductRepository;
import backend.website.clearflow.logic.promo.PromoCodeEntity;
import backend.website.clearflow.logic.promo.PromoCodeRepository;
import backend.website.clearflow.logic.stat.dto.PromoStatDailyAggregateResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatDailyResponse;
import backend.website.clearflow.logic.stat.dto.ProductStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.UpsertPromoStatDailyRequest;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.ForbiddenException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromoStatServiceImpl implements PromoStatService {

    private final PromoStatDailyRepository promoStatDailyRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthContextService authContextService;
    private final PromoStatsDashboardDao promoStatsDashboardDao;
    private final ProductStatsDashboardDao productStatsDashboardDao;

    @Override
    @Transactional
    public PromoStatDailyResponse upsert(UpsertPromoStatDailyRequest request) {
        UserEntity actor = authContextService.currentActorOrThrow();
        validateSellerScope(actor, request.sellerId());
        validateReferences(request.sellerId(), request.promoCodeId(), request.productId());

        PromoStatDailyEntity entity = promoStatDailyRepository
                .findBySellerIdAndPromoCodeIdAndProductIdAndStatDate(
                        request.sellerId(),
                        request.promoCodeId(),
                        request.productId(),
                        request.statDate()
                )
                .orElseGet(PromoStatDailyEntity::new);

        entity.setSellerId(request.sellerId());
        entity.setPromoCodeId(request.promoCodeId());
        entity.setProductId(request.productId());
        entity.setStatDate(request.statDate());
        entity.setOrdersCount(request.ordersCount());
        entity.setItemsCount(request.itemsCount());
        entity.setRevenue(request.revenue());
        PromoStatDailyEntity saved = promoStatDailyRepository.save(entity);
        return new PromoStatDailyResponse(
                saved.getId(),
                saved.getSellerId(),
                saved.getPromoCodeId(),
                saved.getProductId(),
                saved.getStatDate(),
                saved.getOrdersCount(),
                saved.getItemsCount(),
                saved.getRevenue()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromoStatDailyAggregateResponse> getDaily(UUID sellerId, UUID promoCodeId, UUID productId, LocalDate from, LocalDate to) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("from date must be before or equal to to date");
        }

        Specification<PromoStatDailyEntity> specification = Specification.where(bySeller(targetSellerId))
                .and(byDateRange(fromDate, toDate))
                .and(byPromoCode(promoCodeId))
                .and(byProduct(productId));

        return promoStatDailyRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "statDate")).stream()
                .map(row -> new PromoStatDailyAggregateResponse(
                        row.getSellerId(),
                        row.getPromoCodeId(),
                        row.getProductId(),
                        row.getStatDate(),
                        row.getOrdersCount(),
                        row.getItemsCount(),
                        row.getRevenue()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromoStatsDashboardResponse getDashboard(UUID sellerId, YearMonth month, Integer topLimit) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        int resolvedTopLimit = (topLimit == null || topLimit < 1) ? 10 : Math.min(topLimit, 100);
        return promoStatsDashboardDao.load(targetSellerId, targetMonth, resolvedTopLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductStatsDashboardResponse getProductDashboard(UUID sellerId, UUID productId, YearMonth month, Integer topLimit) {
        if (productId == null) {
            throw new BadRequestException("productId is required");
        }
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.getSellerId().equals(targetSellerId)) {
            throw new BadRequestException("Product does not belong to target seller");
        }
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        int resolvedTopLimit = (topLimit == null || topLimit < 1) ? 10 : Math.min(topLimit, 100);
        return productStatsDashboardDao.load(targetSellerId, productId, targetMonth, resolvedTopLimit);
    }

    private void validateReferences(UUID sellerId, UUID promoCodeId, UUID productId) {
        PromoCodeEntity promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new NotFoundException("Promo code not found"));
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!promoCode.getSellerId().equals(sellerId) || !product.getSellerId().equals(sellerId)) {
            throw new BadRequestException("Seller does not match promo code/product");
        }
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
        throw new ForbiddenException("Role cannot access stats");
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

    private Specification<PromoStatDailyEntity> bySeller(UUID sellerId) {
        return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
    }

    private Specification<PromoStatDailyEntity> byPromoCode(UUID promoCodeId) {
        if (promoCodeId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("promoCodeId"), promoCodeId);
    }

    private Specification<PromoStatDailyEntity> byProduct(UUID productId) {
        if (productId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("productId"), productId);
    }

    private Specification<PromoStatDailyEntity> byDateRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.between(root.get("statDate"), from, to);
    }
}
