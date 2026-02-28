package backend.website.clearflow.security;

import backend.website.clearflow.config.property.JwtProperties;
import backend.website.clearflow.model.error.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String CLAIM_SESSION_VERSION = "session_version";
    public static final String CLAIM_REFRESH_SESSION_ID = "refresh_session_id";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.accessSecret()));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.refreshSecret()));
    }

    public String generateAccessToken(AuthenticatedUser user, long sessionVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.accessTtlSeconds());
        return Jwts.builder()
                .subject(user.userId().toString())
                .issuer(properties.issuer())
                .claim("role", user.role().name())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_SESSION_VERSION, sessionVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(accessKey)
                .compact();
    }

    public Claims parseAccessClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateRefreshToken(UUID userId, long sessionVersion, UUID refreshSessionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.refreshTtlSeconds());
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .claim(CLAIM_SESSION_VERSION, sessionVersion)
                .claim(CLAIM_REFRESH_SESSION_ID, refreshSessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(refreshKey)
                .compact();
    }

    public Claims parseRefreshClaims(String token) {
        return Jwts.parser()
                .verifyWith(refreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void validateTokenType(Claims claims, String tokenType) {
        Object claim = claims.get(CLAIM_TOKEN_TYPE);
        if (!(claim instanceof String value) || !tokenType.equals(value)) {
            throw new IllegalArgumentException("Invalid token type");
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public long extractSessionVersion(Claims claims) {
        Number value = claims.get(CLAIM_SESSION_VERSION, Number.class);
        return value == null ? 0 : value.longValue();
    }

    public UUID extractRefreshSessionId(Claims claims) {
        String value = claims.get(CLAIM_REFRESH_SESSION_ID, String.class);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("refresh_session_id claim is missing");
        }
        return UUID.fromString(value);
    }

    public Claims getRefreshClaims(String refreshToken) {
        try {
            Claims claims = parseRefreshClaims(refreshToken);

            validateTokenType(claims, JwtService.TOKEN_TYPE_REFRESH);

            return claims;
        } catch (Exception exception) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }
}
