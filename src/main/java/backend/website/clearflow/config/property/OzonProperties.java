package backend.website.clearflow.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ozon")
public record OzonProperties(
        String baseUrl,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
