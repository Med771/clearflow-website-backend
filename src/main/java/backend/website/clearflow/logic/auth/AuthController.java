package backend.website.clearflow.logic.auth;

import backend.website.clearflow.logic.auth.dto.AuthLoginRequest;
import backend.website.clearflow.logic.auth.dto.AuthTokensResponse;
import backend.website.clearflow.logic.auth.dto.RegisterSellerRequest;
import backend.website.clearflow.logic.auth.dto.RegisterSellerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Операции входа, обновления сессии и регистрации продавца")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-seller")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация продавца", description = "Создает учетную запись продавца со статусом верификации PENDING")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Продавец успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    })
    public RegisterSellerResponse registerSeller(@Valid @RequestBody RegisterSellerRequest request) {
        return authService.registerSeller(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Аутентифицирует пользователя и выставляет access/refresh cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "401", description = "Неверный email или пароль")
    })
    public AuthTokensResponse login(@Valid @RequestBody AuthLoginRequest request, HttpServletResponse response) {
        return authService.login(request, response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токенов", description = "Обновляет access/refresh токены по refresh cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены успешно обновлены"),
            @ApiResponse(responseCode = "401", description = "Refresh токен отсутствует или недействителен")
    })
    public AuthTokensResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return authService.refresh(request, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Выход из системы", description = "Завершает сессию пользователя и очищает auth cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Выход выполнен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
    }
}
