package backend.website.clearflow.logic.profile.dto;

import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import backend.website.clearflow.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Профиль текущего пользователя")
public record MyProfileResponse(
        @Schema(description = "Идентификатор пользователя") UUID id,
        @Schema(description = "Email") String email,
        @Schema(description = "Роль пользователя") UserRole role,
        @Schema(description = "Пользователь заблокирован") boolean isBlock,
        @Schema(description = "Пользователь активен") boolean isActive,
        @Schema(description = "Указан ли Ozon API ключ") boolean hasOzonApiKey,
        @Schema(description = "Загружена ли фотография профиля") boolean hasPhoto,
        @Schema(description = "Идентификатор родителя в иерархии") UUID parentId,
        @Schema(description = "Кто создал пользователя") UUID creatorId,
        @Schema(description = "Дата создания") Instant createdAt,
        @Schema(description = "Дата обновления") Instant updatedAt,
        @Schema(description = "ФИО") String fullName,
        @Schema(description = "Контактный телефон") String contactPhone,
        @Schema(description = "Название компании") String companyName,
        @Schema(description = "Название банка") String bankName,
        @Schema(description = "ИНН") String inn,
        @Schema(description = "БИК") String bik,
        @Schema(description = "Расчетный счет") String settlementAccount,
        @Schema(description = "Корпоративный счет") String corporateAccount,
        @Schema(description = "Адрес") String address,
        @Schema(description = "Ссылка на профиль продавца Ozon") String ozonSellerLink,
        @Schema(description = "Статус верификации продавца") SellerVerificationStatus verificationStatus,
        @Schema(description = "Комментарий модератора") String verificationComment
) {
}
