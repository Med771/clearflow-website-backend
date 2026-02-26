package backend.website.clearflow.logic.stat.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Сводный дашборд статистики товара за месяц")
public record ProductStatsDashboardResponse(
        @Schema(description = "Год отчета", example = "2026")
        int year,
        @Schema(description = "Месяц отчета (1-12)", example = "2")
        int month,
        @Schema(description = "Идентификатор товара")
        UUID productId,
        @Schema(description = "Название товара")
        String productName,
        @Schema(description = "Количество проданных единиц за месяц")
        long monthItemsSold,
        @Schema(description = "Выручка за месяц")
        BigDecimal monthRevenue,
        @ArraySchema(schema = @Schema(description = "Точки графика выручки по дням"))
        List<PromoStatsDailyRevenuePoint> dailyRevenue,
        @ArraySchema(schema = @Schema(description = "Топ промокодов по товару"))
        List<ProductStatsTopPromoCodeResponse> topPromoCodes
) {
}
