package backend.website.clearflow.config;

import backend.website.clearflow.config.property.SwaggerProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final SwaggerProperties swaggerProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(swaggerProperties.title())
                        .description(swaggerProperties.description())
                        .version(swaggerProperties.version()))

                .servers(swaggerProperties.servers().stream()
                        .map(serv -> new Server()
                                .url(serv.url())
                                .description(serv.description()))
                        .toList()
                );
    }
}
