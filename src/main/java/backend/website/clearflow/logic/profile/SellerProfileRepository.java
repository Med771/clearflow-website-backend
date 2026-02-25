package backend.website.clearflow.logic.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SellerProfileRepository extends JpaRepository<SellerProfileEntity, UUID>, JpaSpecificationExecutor<SellerProfileEntity> {
    Optional<SellerProfileEntity> findByUserId(UUID userId);
}
