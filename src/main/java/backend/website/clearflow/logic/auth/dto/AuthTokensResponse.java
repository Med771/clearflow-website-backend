package backend.website.clearflow.logic.auth.dto;

import backend.website.clearflow.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ответ с информацией о токенах и пользователе")
public record AuthTokensResponse(
        @Schema(description = "Идентификатор пользователя")
        UUID userId,
        @Schema(description = "Email пользователя")
        String email,
        @Schema(description = "Роль пользователя")
        UserRole role,
        @Schema(description = "Время жизни access токена в секундах", example = "900")
        long accessExpiresInSeconds,
        @Schema(description = "Время жизни refresh токена в секундах", example = "2592000")
        long refreshExpiresInSeconds
) {
}
