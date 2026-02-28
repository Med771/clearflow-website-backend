package backend.website.clearflow.logic.stat.report;

import java.time.LocalDate;

public record MonthlyPromoReportDocumentOptions(
        String invoiceNumber,
        LocalDate invoiceDate,
        MonthlyPromoReportPartyDetails payer
) {
}
