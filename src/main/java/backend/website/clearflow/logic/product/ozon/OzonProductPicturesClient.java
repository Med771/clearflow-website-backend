package backend.website.clearflow.logic.product.ozon;

import backend.website.clearflow.config.property.OzonProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OzonProductPicturesClient {

    private final OzonProperties ozonProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OzonProductPicturesClient(OzonProperties ozonProperties) {
        this.ozonProperties = ozonProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(ozonProperties.connectTimeoutSeconds()))
                .build();
    }

    public Map<Long, String> fetchPrimaryPhotoLinks(Set<Long> ozonProductIds, String clientId, String apiKey) {
        if (ozonProductIds == null || ozonProductIds.isEmpty() || isBlank(clientId) || isBlank(apiKey)) {
            return Map.of();
        }
        try {
            String payload = buildPayload(ozonProductIds);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(ozonProperties.baseUrl()) + "/v2/product/pictures/info"))
                    .timeout(Duration.ofSeconds(ozonProperties.readTimeoutSeconds()))
                    .header("Client-Id", clientId)
                    .header("Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Map.of();
            }
            return parsePhotoLinks(response.body());
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String buildPayload(Set<Long> ozonProductIds) throws IOException {
        List<String> ids = ozonProductIds.stream().map(String::valueOf).collect(Collectors.toList());
        Map<String, Object> root = new HashMap<>();
        root.put("product_id", ids);
        return objectMapper.writeValueAsString(root);
    }

    private Map<Long, String> parsePhotoLinks(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode items = root.path("items");
        if (!items.isArray()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (JsonNode item : items) {
            long productId = item.path("product_id").asLong(0L);
            if (productId == 0L) {
                continue;
            }
            String photoUrl = firstPhotoUrl(item);
            if (!isBlank(photoUrl)) {
                result.put(productId, photoUrl);
            }
        }
        return result;
    }

    private String firstPhotoUrl(JsonNode item) {
        String fromPrimary = firstArrayValue(item.path("primary_photo"));
        if (!isBlank(fromPrimary)) {
            return fromPrimary;
        }
        String fromPhoto = firstArrayValue(item.path("photo"));
        if (!isBlank(fromPhoto)) {
            return fromPhoto;
        }
        return firstArrayValue(item.path("color_photo"));
    }

    private String firstArrayValue(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            return null;
        }
        String value = node.get(0).asText(null);
        return isBlank(value) ? null : value;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
