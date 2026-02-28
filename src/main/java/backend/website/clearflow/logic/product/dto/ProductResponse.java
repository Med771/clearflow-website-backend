package backend.website.clearflow.logic.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Данные товара")
public record ProductResponse(
        @Schema(description = "Идентификатор товара") UUID id,
        @Schema(description = "Идентификатор продавца") UUID sellerId,
        @Schema(description = "Название товара") String name,
        @Schema(description = "Идентификатор товара в Ozon") Long ozonProductId,
        @Schema(description = "Товар активен") boolean isActive,
        @Schema(description = "Актуальная ссылка на фото товара из Ozon", nullable = true) String photoUrl,
        @Schema(description = "Кто создал запись") UUID creatorId,
        @Schema(description = "Дата создания") Instant createdAt,
        @Schema(description = "Дата обновления") Instant updatedAt
) {
}
