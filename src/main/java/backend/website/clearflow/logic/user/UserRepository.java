package backend.website.clearflow.logic.user;

import backend.website.clearflow.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    Optional<UserEntity> findFirstByRoleOrderByCreatedAtAsc(UserRole role);

    boolean existsByRole(UserRole role);
}
