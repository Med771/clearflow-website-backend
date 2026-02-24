package backend.website.clearflow.logic.user;

import backend.website.clearflow.logic.user.dto.CreateUserRequest;
import backend.website.clearflow.logic.user.dto.UpdateUserRequest;
import backend.website.clearflow.logic.user.dto.UserResponse;
import backend.website.clearflow.model.PageResponse;
import backend.website.clearflow.model.UserRole;
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
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Set<UserRole> roles,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(userService.getUsers(search, roles, parentId, includeInactive, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PatchMapping("/{id}")
    public UserResponse patchUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public UserResponse deleteUser(@PathVariable UUID id) {
        return userService.softDeleteUser(id);
    }
}
