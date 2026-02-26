package backend.website.clearflow.logic.user;

import backend.website.clearflow.logic.user.dto.CreateUserRequest;
import backend.website.clearflow.logic.user.dto.UpdateUserRequest;
import backend.website.clearflow.logic.user.dto.UserResponse;
import backend.website.clearflow.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface UserService {

    Page<UserResponse> getUsers(String search, Set<UserRole> roles, UUID parentId, boolean includeInactive, Pageable pageable);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    UserResponse softDeleteUser(UUID userId);
}
