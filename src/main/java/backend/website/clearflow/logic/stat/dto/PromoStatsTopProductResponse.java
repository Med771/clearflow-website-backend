package backend.website.clearflow.logic.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Строка топа товаров в дашборде продавца")
public record PromoStatsTopProductResponse(
        @Schema(description = "Идентификатор товара")
        UUID productId,
        @Schema(description = "Название товара")
        String productName,
        @Schema(description = "Количество проданных единиц за месяц")
        long monthItemsSold,
        @Schema(description = "Выручка за месяц")
        BigDecimal monthRevenue
) {
}
