package backend.website.clearflow.logic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на регистрацию продавца")
public record RegisterSellerRequest(
        @Schema(description = "Email продавца", example = "seller@example.com")
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "Пароль", example = "StrongPass123")
        @NotBlank @Size(min = 8, max = 100) String password,
        @Schema(description = "ФИО контактного лица", example = "Иванов Иван Иванович")
        @NotBlank @Size(max = 255) String fullName,
        @Schema(description = "Контактный телефон", example = "+79990000000")
        @NotBlank @Size(max = 30) String contactPhone,
        @Schema(description = "Название компании", example = "ООО Ромашка")
        @NotBlank @Size(max = 255) String companyName,
        @Schema(description = "ИНН (10 или 12 цифр)", example = "7701234567")
        @NotBlank @Pattern(regexp = "^\\d{10}(\\d{2})?$", message = "inn must contain 10 or 12 digits") String inn,
        @Schema(description = "Client-Id продавца в Ozon", example = "123456")
        @Size(max = 100) String ozonClientId,
        @Schema(description = "Название банка", example = "ПАО Сбербанк")
        @Size(max = 255) String bankName,
        @Schema(description = "БИК (9 цифр)", example = "044525225")
        @Pattern(regexp = "^\\d{9}$", message = "bik must contain 9 digits") String bik,
        @Schema(description = "Расчетный счет (20 цифр)")
        @Pattern(regexp = "^\\d{20}$", message = "settlementAccount must contain 20 digits") String settlementAccount,
        @Schema(description = "Корпоративный счет (20 цифр)")
        @Pattern(regexp = "^\\d{20}$", message = "corporateAccount must contain 20 digits") String corporateAccount,
        @Schema(description = "Юридический адрес", example = "г. Москва, ул. Пример, д. 1")
        @Size(max = 500) String address,
        @Schema(description = "Ссылка на профиль продавца на Ozon", example = "https://www.ozon.ru/seller/12345")
        @Size(max = 500) @Pattern(regexp = "^(https?://.+)?$", message = "ozonSellerLink must be a valid http/https url") String ozonSellerLink
) {
}
