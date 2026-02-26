package backend.website.clearflow.model;

import java.util.Set;

public enum UserRole {
    OWNER,
    ADMIN,
    SELLER,
    MANAGER;

    public int weight() {
        return switch (this) {
            case OWNER -> 400;
            case ADMIN -> 300;
            case SELLER -> 200;
            case MANAGER -> 100;
        };
    }

    public boolean isHigherThan(UserRole other) {
        return this.weight() > other.weight();
    }

    public static Set<UserRole> visibleRoles(UserRole role) {
        return switch (role) {
            case OWNER -> Set.of(OWNER, ADMIN, SELLER, MANAGER);
            case ADMIN -> Set.of(SELLER, MANAGER);
            case SELLER -> Set.of(MANAGER);
            case MANAGER -> Set.of();
        };
    }
}
