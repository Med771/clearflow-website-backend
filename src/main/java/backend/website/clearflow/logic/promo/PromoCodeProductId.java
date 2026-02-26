package backend.website.clearflow.logic.promo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PromoCodeProductId implements Serializable {

    @Column(name = "promo_code_id", nullable = false)
    private UUID promoCodeId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;
}
