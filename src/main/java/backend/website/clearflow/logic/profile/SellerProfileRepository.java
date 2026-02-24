package backend.website.clearflow.logic.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerProfileRepository extends JpaRepository<SellerProfileEntity, UUID> {
    Optional<SellerProfileEntity> findByUserId(UUID userId);
}
