package backend.website.clearflow.logic.product;

import backend.website.clearflow.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "products")
public class ProductEntity extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "ozon_product_id", nullable = false)
    private Long ozonProductId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
