package backend.website.clearflow.logic.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Дневная статистика по продавцу, промокоду и товару")
public record PromoStatDailyResponse(
        @Schema(description = "Идентификатор строки статистики") UUID id,
        @Schema(description = "Идентификатор продавца") UUID sellerId,
        @Schema(description = "Идентификатор промокода") UUID promoCodeId,
        @Schema(description = "Идентификатор товара") UUID productId,
        @Schema(description = "Дата статистики") LocalDate statDate,
        @Schema(description = "Количество заказов") long ordersCount,
        @Schema(description = "Количество единиц товара") long itemsCount,
        @Schema(description = "Выручка") BigDecimal revenue
) {
}
