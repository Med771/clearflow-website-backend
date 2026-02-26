package backend.website.clearflow.logic.promo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на создание промокода")
public record CreatePromoCodeRequest(
        @Schema(description = "Название промокода", example = "BLACKFRIDAY")
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Идентификатор акции в Ozon (action_id)", example = "987654321")
        @NotNull Long actionId,
        @Schema(description = "Идентификатор продавца (для OWNER/ADMIN)")
        UUID sellerId,
        @ArraySchema(schema = @Schema(description = "Список товаров, связанных с промокодом"))
        List<UUID> productIds
) {
}
