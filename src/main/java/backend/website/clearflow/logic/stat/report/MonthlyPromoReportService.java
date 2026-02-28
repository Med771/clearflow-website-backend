package backend.website.clearflow.logic.stat.report;

import java.time.YearMonth;
import java.util.UUID;

public interface MonthlyPromoReportService {
    byte[] generateMonthlyPromoReport(UUID sellerId, YearMonth month, MonthlyPromoReportDocumentOptions options);
}
