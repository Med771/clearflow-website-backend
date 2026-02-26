package backend.website.clearflow.logic.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Строка топа промокодов в дашборде товара")
public record ProductStatsTopPromoCodeResponse(
        @Schema(description = "Идентификатор промокода")
        UUID promoCodeId,
        @Schema(description = "Название промокода")
        String promoCodeName,
        @Schema(description = "Количество проданных единиц за месяц")
        long monthItemsSold,
        @Schema(description = "Выручка за месяц")
        BigDecimal monthRevenue
) {
}
