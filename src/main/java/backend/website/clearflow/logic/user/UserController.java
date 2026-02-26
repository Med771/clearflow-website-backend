package backend.website.clearflow.logic.user;

import backend.website.clearflow.logic.user.dto.CreateUserRequest;
import backend.website.clearflow.logic.user.dto.UpdateUserRequest;
import backend.website.clearflow.logic.user.dto.UserResponse;
import backend.website.clearflow.model.PageResponse;
import backend.website.clearflow.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Пользователи", description = "Управление пользователями и ролями")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Список пользователей", description = "Возвращает пользователей с учетом ролей, фильтров и пагинации")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей получен"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public PageResponse<UserResponse> getUsers(
            @Parameter(description = "Поиск по email") @RequestParam(required = false) String search,
            @Parameter(description = "Фильтр по ролям") @RequestParam(required = false) Set<UserRole> roles,
            @Parameter(description = "Фильтр по родительскому пользователю") @RequestParam(required = false) UUID parentId,
            @Parameter(description = "Включать неактивных пользователей") @RequestParam(defaultValue = "false") boolean includeInactive,
            @Parameter(description = "Пагинация и сортировка. Формат sort: field,asc|desc")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(userService.getUsers(search, roles, parentId, includeInactive, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пользователя", description = "Создает пользователя с заданной ролью и связью подчиненности")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить пользователя", description = "Обновляет данные пользователя по идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь обновлен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public UserResponse patchUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Деактивировать пользователя", description = "Выполняет soft-delete пользователя (isActive=false)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь деактивирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public UserResponse deleteUser(@PathVariable UUID id) {
        return userService.softDeleteUser(id);
    }
}
