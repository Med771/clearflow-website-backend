package backend.website.clearflow.logic.user;

import backend.website.clearflow.model.UserRole;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserAccessPolicy {

    public boolean canViewAny(UserRole actorRole) {
        return actorRole != UserRole.MANAGER;
    }

    public boolean canCreate(UserRole actorRole, UserRole targetRole) {
        if (targetRole == UserRole.OWNER) {
            return false;
        }
        return switch (actorRole) {
            case OWNER -> targetRole == UserRole.ADMIN || targetRole == UserRole.SELLER || targetRole == UserRole.MANAGER;
            case ADMIN -> targetRole == UserRole.SELLER || targetRole == UserRole.MANAGER;
            case SELLER -> targetRole == UserRole.MANAGER;
            case MANAGER -> false;
        };
    }

    public boolean canManage(UserRole actorRole, UserRole targetRole) {
        if (actorRole == UserRole.MANAGER || targetRole == UserRole.OWNER) {
            return false;
        }
        return actorRole.isHigherThan(targetRole);
    }

    public boolean isInScope(UserEntity actor, UserEntity target) {
        if (actor.getRole() == UserRole.OWNER) {
            return false;
        }
        if (actor.getRole() == UserRole.ADMIN) {
            return (target.getRole() != UserRole.SELLER || !Objects.equals(target.getParentId(), actor.getId()))
                    && (target.getRole() != UserRole.MANAGER || !hasAdminScopeForManager(actor.getId(), target));
        }
        if (actor.getRole() == UserRole.SELLER) {
            return target.getRole() != UserRole.MANAGER || !Objects.equals(target.getParentId(), actor.getId());
        }
        return true;
    }

    private boolean hasAdminScopeForManager(java.util.UUID adminId, UserEntity manager) {
        return Objects.equals(manager.getCreatorId(), adminId);
    }
}
