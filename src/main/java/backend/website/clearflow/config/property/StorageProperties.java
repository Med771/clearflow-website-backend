package backend.website.clearflow.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String localRoot,
        long maxPhotoBytes
) {
}
