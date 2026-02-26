package backend.website.clearflow.logic.stat.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Сводный дашборд статистики продавца за месяц")
public record PromoStatsDashboardResponse(
        @Schema(description = "Год отчета", example = "2026")
        int year,
        @Schema(description = "Месяц отчета (1-12)", example = "2")
        int month,
        @Schema(description = "Количество проданных единиц за месяц")
        long monthItemsSold,
        @Schema(description = "Выручка за месяц")
        BigDecimal monthRevenue,
        @Schema(description = "Выручка за последние 7 дней")
        BigDecimal last7DaysRevenue,
        @ArraySchema(schema = @Schema(description = "Точки графика выручки по дням"))
        List<PromoStatsDailyRevenuePoint> dailyRevenue,
        @ArraySchema(schema = @Schema(description = "Топ товаров за месяц"))
        List<PromoStatsTopProductResponse> topProducts
) {
}
