package backend.website.clearflow.logic.promo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromoCodeProductRepository extends JpaRepository<PromoCodeProductEntity, PromoCodeProductId> {
    List<PromoCodeProductEntity> findAllByPromoCodeId(UUID promoCodeId);

    void deleteAllByPromoCodeId(UUID promoCodeId);
}
