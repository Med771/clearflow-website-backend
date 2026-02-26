package backend.website.clearflow.logic.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {
    boolean existsBySellerIdAndOzonProductId(UUID sellerId, Long ozonProductId);
}
