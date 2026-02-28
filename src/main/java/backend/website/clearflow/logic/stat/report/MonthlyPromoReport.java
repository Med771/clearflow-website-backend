package backend.website.clearflow.logic.stat.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record MonthlyPromoReport(
        YearMonth month,
        String invoiceNumber,
        LocalDate invoiceDate,
        MonthlyPromoReportPartyDetails recipient,
        MonthlyPromoReportPartyDetails payer,
        List<MonthlyPromoReportRow> rows,
        long totalItemsSold,
        BigDecimal totalRevenue
) {
}
