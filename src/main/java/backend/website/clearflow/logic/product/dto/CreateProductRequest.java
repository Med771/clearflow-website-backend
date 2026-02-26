package backend.website.clearflow.logic.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Запрос на создание товара")
public record CreateProductRequest(
        @Schema(description = "Внутреннее название товара", example = "Кроссовки белые")
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Идентификатор товара в Ozon", example = "123456789")
        @NotNull Long ozonProductId,
        @Schema(description = "Идентификатор продавца (для OWNER/ADMIN)")
        UUID sellerId
) {
}
