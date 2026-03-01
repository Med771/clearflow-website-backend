package backend.website.clearflow.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.swagger")
public record SwaggerProperties (
        String title,
        String description,
        String version,

        List<ServerProperties> servers
) {
    public record ServerProperties (
            String url,
            String description
    ){}
}
