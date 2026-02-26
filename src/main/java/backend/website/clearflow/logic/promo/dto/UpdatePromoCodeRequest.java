package backend.website.clearflow.logic.promo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на обновление промокода")
public record UpdatePromoCodeRequest(
        @Schema(description = "Новое название промокода")
        @Size(max = 255) String name,
        @Schema(description = "Признак активности промокода")
        Boolean isActive,
        @ArraySchema(schema = @Schema(description = "Новый список связанных товаров"))
        List<UUID> productIds
) {
}
