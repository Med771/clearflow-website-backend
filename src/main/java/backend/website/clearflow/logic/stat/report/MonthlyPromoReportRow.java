package backend.website.clearflow.logic.stat.report;

import java.math.BigDecimal;
import java.util.UUID;

public record MonthlyPromoReportRow(
        UUID promoCodeId,
        String promoCodeName,
        long itemsSold,
        BigDecimal revenue
) {
}
