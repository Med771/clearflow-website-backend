package backend.website.clearflow.logic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на вход пользователя")
public record AuthLoginRequest(
        @Schema(description = "Email пользователя", example = "seller@example.com")
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "Пароль пользователя", example = "StrongPass123")
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
