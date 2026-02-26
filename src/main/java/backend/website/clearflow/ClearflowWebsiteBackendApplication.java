package backend.website.clearflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClearflowWebsiteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearflowWebsiteBackendApplication.class, args);
    }

}
