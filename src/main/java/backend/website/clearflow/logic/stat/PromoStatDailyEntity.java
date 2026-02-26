package backend.website.clearflow.logic.stat;

import backend.website.clearflow.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "promo_stats_daily")
public class PromoStatDailyEntity extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "promo_code_id", nullable = false)
    private UUID promoCodeId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "orders_count", nullable = false)
    private long ordersCount;

    @Column(name = "items_count", nullable = false)
    private long itemsCount;

    @Column(name = "revenue", nullable = false, precision = 18, scale = 2)
    private BigDecimal revenue;
}
