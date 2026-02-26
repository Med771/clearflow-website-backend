package backend.website.clearflow.logic.profile.verification.dto;

import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Данные профиля продавца для верификации")
public record SellerVerificationResponse(
        @Schema(description = "Идентификатор пользователя") UUID userId,
        @Schema(description = "Email") String email,
        @Schema(description = "ФИО") String fullName,
        @Schema(description = "Контактный телефон") String contactPhone,
        @Schema(description = "Компания") String companyName,
        @Schema(description = "ИНН") String inn,
        @Schema(description = "Ссылка на профиль Ozon") String ozonSellerLink,
        @Schema(description = "Статус верификации") SellerVerificationStatus verificationStatus,
        @Schema(description = "Комментарий модератора") String verificationComment,
        @Schema(description = "Дата отправки на проверку") Instant verificationSubmittedAt,
        @Schema(description = "Дата проверки") Instant verifiedAt,
        @Schema(description = "Кто проверил") UUID verifiedBy
) {
}
