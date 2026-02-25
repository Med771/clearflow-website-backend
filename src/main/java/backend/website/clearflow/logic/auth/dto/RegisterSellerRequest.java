package backend.website.clearflow.logic.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterSellerRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(max = 30) String contactPhone,
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Pattern(regexp = "^\\d{10}(\\d{2})?$", message = "inn must contain 10 or 12 digits") String inn,
        @Size(max = 255) String bankName,
        @Pattern(regexp = "^\\d{9}$", message = "bik must contain 9 digits") String bik,
        @Pattern(regexp = "^\\d{20}$", message = "settlementAccount must contain 20 digits") String settlementAccount,
        @Pattern(regexp = "^\\d{20}$", message = "corporateAccount must contain 20 digits") String corporateAccount,
        @Size(max = 500) String address,
        @Size(max = 500) @Pattern(regexp = "^(https?://.+)?$", message = "ozonSellerLink must be a valid http/https url") String ozonSellerLink
) {
}
