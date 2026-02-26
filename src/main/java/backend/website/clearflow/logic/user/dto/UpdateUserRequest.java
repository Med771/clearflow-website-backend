package backend.website.clearflow.logic.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление пользователя")
public record UpdateUserRequest(
        @Schema(description = "Новый email")
        @Email @Size(max = 320) String email,
        @Schema(description = "Новый пароль")
        @Size(min = 8, max = 100) String password,
        @Schema(description = "Признак блокировки пользователя")
        Boolean isBlock,
        @Schema(description = "Признак активности пользователя")
        Boolean isActive
) {
}
