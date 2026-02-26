package backend.website.clearflow.logic.user.dto;

import backend.website.clearflow.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Данные пользователя")
public record UserResponse(
        @Schema(description = "Идентификатор пользователя")
        UUID id,
        @Schema(description = "Email")
        String email,
        @Schema(description = "Роль")
        UserRole role,
        @Schema(description = "Пользователь заблокирован")
        boolean isBlock,
        @Schema(description = "Пользователь активен")
        boolean isActive,
        @Schema(description = "Указан ли Ozon API ключ")
        boolean hasOzonApiKey,
        @Schema(description = "Идентификатор родителя в иерархии")
        UUID parentId,
        @Schema(description = "Кто создал пользователя")
        UUID creatorId,
        @Schema(description = "Дата создания")
        Instant createdAt,
        @Schema(description = "Дата обновления")
        Instant updatedAt
) {
}
