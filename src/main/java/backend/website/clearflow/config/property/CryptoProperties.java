package backend.website.clearflow.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crypto")
public record CryptoProperties(
        String keyVersion,
        String aesKey
) {
}
