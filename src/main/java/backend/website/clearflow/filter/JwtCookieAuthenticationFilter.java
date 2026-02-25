package backend.website.clearflow.filter;

import backend.website.clearflow.config.property.JwtProperties;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.security.AuthenticatedUser;
import backend.website.clearflow.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    public JwtCookieAuthenticationFilter(
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request)
                    .flatMap(this::resolveUser)
                    .ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> jwtProperties.accessCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private Optional<AuthenticatedUser> resolveUser(String token) {
        try {
            Claims claims = jwtService.parseAccessClaims(token);
            jwtService.validateTokenType(claims, JwtService.TOKEN_TYPE_ACCESS);
            UUID userId = jwtService.extractUserId(claims);
            long sessionVersion = jwtService.extractSessionVersion(claims);
            return userRepository.findById(userId)
                    .filter(UserEntity::isActive)
                    .filter(user -> user.getSessionVersion() == sessionVersion)
                    .map(user -> new AuthenticatedUser(user.getId(), user.getRole(), user.isBlock(), user.isActive()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void authenticate(AuthenticatedUser principal) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
