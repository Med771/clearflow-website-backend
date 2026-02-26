package backend.website.clearflow.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.owner")
public record OwnerProperties(
        String email,
        String password
) {
}
