package backend.website.clearflow.logic.user.dto;

import backend.website.clearflow.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Запрос на создание пользователя")
public record CreateUserRequest(
        @Schema(description = "Email нового пользователя")
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "Пароль нового пользователя")
        @NotBlank @Size(min = 8, max = 100) String password,
        @Schema(description = "Роль нового пользователя")
        @NotNull UserRole role,
        @Schema(description = "Родительский пользователь в иерархии")
        UUID parentId,
        @Schema(description = "Ozon API ключ в открытом виде (будет зашифрован)")
        String ozonApiKey
) {
}
