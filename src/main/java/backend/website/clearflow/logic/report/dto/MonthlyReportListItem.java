package backend.website.clearflow.logic.report.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MonthlyReportListItem(
        UUID id,
        UUID sellerId,
        String reportMonth,
        LocalDate invoiceDate,
        Instant createdAt
) {}
