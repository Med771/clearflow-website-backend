package backend.website.clearflow.security;

import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.error.ForbiddenException;
import backend.website.clearflow.model.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthContextService {

    private final UserRepository userRepository;

    public UserEntity currentActorOrThrow() {
        UserEntity user = currentActiveActorOrThrow();
        if (user.isBlock()) {
            throw new ForbiddenException("Account is blocked. Functionality is unavailable, please contact administrators.");
        }
        return user;
    }

    public UserEntity currentActiveActorAllowBlockedOrThrow() {
        return currentActiveActorOrThrow();
    }

    private UserEntity currentActiveActorOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        if (!user.isActive()) {
            throw new UnauthorizedException("User is inactive");
        }
        return user;
    }


}
