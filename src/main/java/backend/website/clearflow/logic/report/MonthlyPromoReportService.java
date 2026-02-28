package backend.website.clearflow.logic.report;

import backend.website.clearflow.logic.report.dto.MonthlyReportListItem;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface MonthlyPromoReportService {
    byte[] generateMonthlyPromoReport(UUID sellerId, YearMonth month, LocalDate invoiceDate);

    List<MonthlyReportListItem> listReports();
}
