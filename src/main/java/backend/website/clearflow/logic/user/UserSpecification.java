package backend.website.clearflow.logic.user;

import backend.website.clearflow.model.UserRole;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;
import java.util.UUID;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<UserEntity> visibleFor(UserEntity actor) {
        return (root, query, cb) -> switch (actor.getRole()) {
            case OWNER -> cb.conjunction();
            case ADMIN -> cb.and(
                    root.get("role").in(Set.of(UserRole.SELLER, UserRole.MANAGER)),
                    cb.or(
                            cb.equal(root.get("parentId"), actor.getId()),
                            cb.equal(root.get("creatorId"), actor.getId())
                    )
            );
            case SELLER -> cb.and(
                    cb.equal(root.get("role"), UserRole.MANAGER),
                    cb.equal(root.get("parentId"), actor.getId())
            );
            case MANAGER -> cb.disjunction();
        };
    }

    public static Specification<UserEntity> search(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + queryText.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), pattern);
    }

    public static Specification<UserEntity> roleIn(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("role").in(roles);
    }

    public static Specification<UserEntity> parent(UUID parentId) {
        if (parentId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("parentId"), parentId);
    }

    public static Specification<UserEntity> activeOnly(boolean includeInactive) {
        if (includeInactive) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
