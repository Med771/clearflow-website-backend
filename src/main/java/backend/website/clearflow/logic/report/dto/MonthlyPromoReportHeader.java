package backend.website.clearflow.logic.report.dto;

import java.util.UUID;

public record MonthlyPromoReportHeader(
        UUID sellerId,
        String sellerEmail,
        String fullName,
        String companyName,
        String inn,
        String bankName,
        String bik,
        String settlementAccount,
        String corporateAccount,
        String address
) {
}
