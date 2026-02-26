package backend.website.clearflow.logic.stat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PromoStatDailyRepository extends JpaRepository<PromoStatDailyEntity, UUID>, JpaSpecificationExecutor<PromoStatDailyEntity> {
    Optional<PromoStatDailyEntity> findBySellerIdAndPromoCodeIdAndProductIdAndStatDate(
            UUID sellerId,
            UUID promoCodeId,
            UUID productId,
            LocalDate statDate
    );
}
