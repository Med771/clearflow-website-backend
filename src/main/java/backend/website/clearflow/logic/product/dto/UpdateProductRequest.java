package backend.website.clearflow.logic.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление товара")
public record UpdateProductRequest(
        @Schema(description = "Новое название товара")
        @Size(max = 255) String name,
        @Schema(description = "Признак активности товара")
        Boolean isActive
) {
}
