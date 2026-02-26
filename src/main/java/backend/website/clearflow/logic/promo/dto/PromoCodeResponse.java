package backend.website.clearflow.logic.promo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Данные промокода")
public record PromoCodeResponse(
        @Schema(description = "Идентификатор промокода") UUID id,
        @Schema(description = "Идентификатор продавца") UUID sellerId,
        @Schema(description = "Название промокода") String name,
        @Schema(description = "Идентификатор акции в Ozon") Long actionId,
        @Schema(description = "Промокод активен") boolean isActive,
        @ArraySchema(schema = @Schema(description = "Список связанных товаров")) List<UUID> productIds,
        @Schema(description = "Кто создал запись") UUID creatorId,
        @Schema(description = "Дата создания") Instant createdAt,
        @Schema(description = "Дата обновления") Instant updatedAt
) {
}
