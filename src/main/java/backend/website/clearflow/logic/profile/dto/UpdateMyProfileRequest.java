package backend.website.clearflow.logic.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление профиля текущего пользователя")
public record UpdateMyProfileRequest(
        @Schema(description = "ФИО") @Size(max = 255) String fullName,
        @Schema(description = "Контактный телефон") @Size(max = 30) String contactPhone,
        @Schema(description = "Название компании") @Size(max = 255) String companyName,
        @Schema(description = "Название банка") @Size(max = 255) String bankName,
        @Schema(description = "ИНН (10 или 12 цифр)") @Pattern(regexp = "^\\d{10}(\\d{2})?$", message = "inn must contain 10 or 12 digits") String inn,
        @Schema(description = "БИК (9 цифр)") @Pattern(regexp = "^\\d{9}$", message = "bik must contain 9 digits") String bik,
        @Schema(description = "Расчетный счет (20 цифр)") @Pattern(regexp = "^\\d{20}$", message = "settlementAccount must contain 20 digits") String settlementAccount,
        @Schema(description = "Корпоративный счет (20 цифр)") @Pattern(regexp = "^\\d{20}$", message = "corporateAccount must contain 20 digits") String corporateAccount,
        @Schema(description = "Адрес") @Size(max = 500) String address,
        @Schema(description = "Ссылка на профиль продавца Ozon") @Size(max = 500) @Pattern(regexp = "^(https?://.+)?$", message = "ozonSellerLink must be a valid http/https url") String ozonSellerLink
) {
}
