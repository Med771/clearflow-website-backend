package backend.website.clearflow.logic.profile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 255) String companyName,
        @Size(max = 255) String bankName,
        @Pattern(regexp = "^\\d{10}(\\d{2})?$", message = "inn must contain 10 or 12 digits") String inn,
        @Pattern(regexp = "^\\d{9}$", message = "bik must contain 9 digits") String bik,
        @Pattern(regexp = "^\\d{20}$", message = "settlementAccount must contain 20 digits") String settlementAccount,
        @Pattern(regexp = "^\\d{20}$", message = "corporateAccount must contain 20 digits") String corporateAccount,
        @Size(max = 500) String address
) {
}
