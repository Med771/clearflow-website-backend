package backend.website.clearflow.logic.user;

import backend.website.clearflow.logic.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "isBlock", source = "block")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "hasOzonApiKey", expression = "java(entity.getOzonApiKeyCiphertext() != null && !entity.getOzonApiKeyCiphertext().isBlank())")
    UserResponse toResponse(UserEntity entity);
}
