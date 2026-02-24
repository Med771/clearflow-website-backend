package backend.website.clearflow.logic.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 320) String email,
        @Size(min = 8, max = 100) String password,
        Boolean isBlock,
        Boolean isActive
) {
}
