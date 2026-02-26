package backend.website.clearflow.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        String accessCookieName,
        String refreshCookieName,
        String accessSecret,
        String refreshSecret,
        long accessTtlSeconds,
        long refreshTtlSeconds,
        boolean cookieSecure
) {
}
