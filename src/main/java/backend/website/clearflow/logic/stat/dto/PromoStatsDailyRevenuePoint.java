package backend.website.clearflow.logic.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Точка графика выручки по дням")
public record PromoStatsDailyRevenuePoint(
        @Schema(description = "День месяца", example = "15")
        int dayOfMonth,
        @Schema(description = "Выручка за день в выбранном месяце")
        BigDecimal currentRevenue,
        @Schema(description = "Выручка за этот же день в предыдущем месяце")
        BigDecimal previousRevenue
) {
}
