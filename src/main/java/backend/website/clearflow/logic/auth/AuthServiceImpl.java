package backend.website.clearflow.logic.auth;

import backend.website.clearflow.config.property.JwtProperties;
import backend.website.clearflow.helper.AuthTokenHashHelper;
import backend.website.clearflow.logic.auth.dto.AuthLoginRequest;
import backend.website.clearflow.logic.auth.dto.AuthTokensResponse;
import backend.website.clearflow.logic.auth.dto.RegisterSellerRequest;
import backend.website.clearflow.logic.auth.dto.RegisterSellerResponse;
import backend.website.clearflow.logic.profile.SellerProfileEntity;
import backend.website.clearflow.logic.profile.SellerProfileRepository;
import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.UnauthorizedException;
import backend.website.clearflow.security.AuthenticatedUser;
import backend.website.clearflow.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final JwtService jwtService;
    private final AuthCookieService authCookieService;
    private final AuthTokenHashHelper authTokenHashHelper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final SellerProfileRepository sellerProfileRepository;

    @Override
    @Transactional
    public RegisterSellerResponse registerSeller(RegisterSellerRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("User with this email already exists");
        }

        UserEntity sellerUser = new UserEntity();
        sellerUser.setEmail(email);
        sellerUser.setPassword(passwordEncoder.encode(request.password()));
        sellerUser.setRole(UserRole.SELLER);
        sellerUser.setActive(true);
        sellerUser.setBlock(false);
        sellerUser.setSessionVersion(0);
        sellerUser.setOzonClientId(normalizeNullableText(request.ozonClientId()));
        sellerUser = userRepository.save(sellerUser);

        SellerProfileEntity profile = new SellerProfileEntity();
        profile.setUserId(sellerUser.getId());
        profile.setFullName(normalizeText(request.fullName()));
        profile.setContactPhone(normalizeText(request.contactPhone()));
        profile.setCompanyName(normalizeText(request.companyName()));
        profile.setInn(normalizeText(request.inn()));
        profile.setBankName(normalizeNullableText(request.bankName()));
        profile.setBik(normalizeNullableText(request.bik()));
        profile.setSettlementAccount(normalizeNullableText(request.settlementAccount()));
        profile.setCorporateAccount(normalizeNullableText(request.corporateAccount()));
        profile.setAddress(normalizeNullableText(request.address()));
        profile.setOzonSellerLink(normalizeNullableText(request.ozonSellerLink()));
        profile.setVerificationStatus(SellerVerificationStatus.PENDING);
        profile.setVerificationComment("Profile is waiting for admin review");
        profile.setVerificationSubmittedAt(Instant.now());
        sellerProfileRepository.save(profile);

        return new RegisterSellerResponse(
                sellerUser.getId(),
                sellerUser.getEmail(),
                sellerUser.getRole(),
                SellerVerificationStatus.PENDING,
                "Profile is created and waiting for admin verification"
        );
    }

    @Override
    @Transactional
    public AuthTokensResponse login(AuthLoginRequest request, HttpServletResponse response) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!user.isActive() || user.isBlock()) {
            throw new UnauthorizedException("User is blocked or inactive");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        TokenPair pair = issueTokenPair(user);
        writeCookies(response, pair);
        return new AuthTokensResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtProperties.accessTtlSeconds(),
                jwtProperties.refreshTtlSeconds()
        );
    }

    @Override
    @Transactional
    public AuthTokensResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request).orElseThrow(() -> new UnauthorizedException("Refresh token is missing"));
        Claims claims = parseRefreshClaims(refreshToken);

        UUID userId = jwtService.extractUserId(claims);
        long sessionVersion = jwtService.extractSessionVersion(claims);
        UUID refreshSessionId = jwtService.extractRefreshSessionId(claims);

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!user.isActive() || user.isBlock()) {
            throw new UnauthorizedException("User is blocked or inactive");
        }
        if (user.getSessionVersion() != sessionVersion) {
            throw new UnauthorizedException("Session version mismatch");
        }

        RefreshSessionEntity currentSession = refreshSessionRepository.findById(refreshSessionId)
                .orElseThrow(() -> new UnauthorizedException("Refresh session not found"));
        validateRefreshSession(currentSession, userId, refreshToken);

        currentSession.setRevokedAt(Instant.now());
        refreshSessionRepository.save(currentSession);

        TokenPair pair = issueTokenPair(user);
        writeCookies(response, pair);
        return new AuthTokensResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtProperties.accessTtlSeconds(),
                jwtProperties.refreshTtlSeconds()
        );
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        extractRefreshToken(request).ifPresent(token -> {
            try {
                Claims claims = parseRefreshClaims(token);
                UUID refreshSessionId = jwtService.extractRefreshSessionId(claims);
                refreshSessionRepository.findById(refreshSessionId).ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(Instant.now());
                        refreshSessionRepository.save(session);
                    }
                });
            } catch (Exception ignored) {
                // Logout must be idempotent and safe even for invalid tokens.
            }
        });
        authCookieService.clearAuthCookies(response);
    }

    private Claims parseRefreshClaims(String refreshToken) {
        try {
            Claims claims = jwtService.parseRefreshClaims(refreshToken);
            jwtService.validateTokenType(claims, JwtService.TOKEN_TYPE_REFRESH);
            return claims;
        } catch (Exception exception) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    private TokenPair issueTokenPair(UserEntity user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getRole(), user.isBlock(), user.isActive());

        RefreshSessionEntity refreshSession = new RefreshSessionEntity();
        refreshSession.setUserId(user.getId());
        refreshSession.setCreatedAt(Instant.now());
        refreshSession.setExpiresAt(Instant.now().plusSeconds(jwtProperties.refreshTtlSeconds()));
        refreshSession.setTokenHash("-");
        refreshSession = refreshSessionRepository.save(refreshSession);

        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getSessionVersion(), refreshSession.getId());
        refreshSession.setTokenHash(authTokenHashHelper.sha256(refreshToken));
        refreshSessionRepository.save(refreshSession);

        String accessToken = jwtService.generateAccessToken(principal, user.getSessionVersion());
        return new TokenPair(accessToken, refreshToken);
    }

    private void validateRefreshSession(RefreshSessionEntity session, UUID userId, String refreshToken) {
        if (!session.getUserId().equals(userId)) {
            throw new UnauthorizedException("Refresh session does not belong to user");
        }
        if (session.getRevokedAt() != null) {
            throw new UnauthorizedException("Refresh session is revoked");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh session is expired");
        }
        String hash = authTokenHashHelper.sha256(refreshToken);
        if (!hash.equals(session.getTokenHash())) {
            throw new UnauthorizedException("Refresh token mismatch");
        }
    }

    private Optional<String> extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> jwtProperties.refreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private void writeCookies(HttpServletResponse response, TokenPair pair) {
        authCookieService.writeAccessCookie(response, pair.accessToken());
        authCookieService.writeRefreshCookie(response, pair.refreshToken());
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
