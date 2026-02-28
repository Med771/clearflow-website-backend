package backend.website.clearflow.logic.stat.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyPromoReport(
        YearMonth month,
        MonthlyPromoReportHeader header,
        List<MonthlyPromoReportRow> rows,
        long totalItemsSold,
        BigDecimal totalRevenue
) {
}
