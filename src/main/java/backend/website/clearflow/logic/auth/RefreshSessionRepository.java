package backend.website.clearflow.logic.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> {
    List<RefreshSessionEntity> findByUserIdAndRevokedAtIsNull(UUID userId);
}
