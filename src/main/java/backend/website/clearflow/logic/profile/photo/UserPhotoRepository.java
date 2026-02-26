package backend.website.clearflow.logic.profile.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPhotoRepository extends JpaRepository<UserPhotoEntity, UUID> {
    Optional<UserPhotoEntity> findByUserId(UUID userId);
}
