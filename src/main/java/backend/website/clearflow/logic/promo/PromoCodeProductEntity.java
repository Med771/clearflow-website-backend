package backend.website.clearflow.logic.promo;

import backend.website.clearflow.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promo_code_products")
public class PromoCodeProductEntity extends BaseEntity {

    @EmbeddedId
    private PromoCodeProductId id;

    @Column(name = "promo_code_id", insertable = false, updatable = false, nullable = false)
    private java.util.UUID promoCodeId;

    @Column(name = "product_id", insertable = false, updatable = false, nullable = false)
    private java.util.UUID productId;
}
