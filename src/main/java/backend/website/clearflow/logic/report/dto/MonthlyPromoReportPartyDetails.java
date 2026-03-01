package backend.website.clearflow.logic.report.dto;

public record MonthlyPromoReportPartyDetails(
        String name,
        String inn,
        String bankName,
        String bik,
        String settlementAccount,
        String corporateAccount,
        String address
) {
}
