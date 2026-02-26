package backend.website.clearflow.logic.auth.dto;

import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import backend.website.clearflow.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ответ после регистрации продавца")
public record RegisterSellerResponse(
        @Schema(description = "Идентификатор созданного пользователя")
        UUID userId,
        @Schema(description = "Email продавца")
        String email,
        @Schema(description = "Роль пользователя")
        UserRole role,
        @Schema(description = "Текущий статус верификации")
        SellerVerificationStatus verificationStatus,
        @Schema(description = "Информационное сообщение")
        String message
) {
}
