package backend.website.clearflow.logic.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Запрос на создание или обновление дневной статистики")
public record UpsertPromoStatDailyRequest(
        @Schema(description = "Идентификатор продавца")
        @NotNull UUID sellerId,
        @Schema(description = "Идентификатор промокода")
        @NotNull UUID promoCodeId,
        @Schema(description = "Идентификатор товара")
        @NotNull UUID productId,
        @Schema(description = "Дата статистики", example = "2026-02-20")
        @NotNull LocalDate statDate,
        @Schema(description = "Количество заказов")
        @NotNull Long ordersCount,
        @Schema(description = "Количество проданных единиц")
        @NotNull Long itemsCount,
        @Schema(description = "Выручка за день")
        @NotNull BigDecimal revenue
) {
}
