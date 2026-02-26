package backend.website.clearflow.logic.profile.verification;

import backend.website.clearflow.logic.profile.SellerProfileEntity;
import backend.website.clearflow.logic.profile.verification.dto.SellerVerificationResponse;
import backend.website.clearflow.logic.user.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SellerVerificationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "fullName", source = "profile.fullName")
    @Mapping(target = "contactPhone", source = "profile.contactPhone")
    @Mapping(target = "companyName", source = "profile.companyName")
    @Mapping(target = "inn", source = "profile.inn")
    @Mapping(target = "ozonSellerLink", source = "profile.ozonSellerLink")
    @Mapping(target = "verificationStatus", source = "profile.verificationStatus")
    @Mapping(target = "verificationComment", source = "profile.verificationComment")
    @Mapping(target = "verificationSubmittedAt", source = "profile.verificationSubmittedAt")
    @Mapping(target = "verifiedAt", source = "profile.verifiedAt")
    @Mapping(target = "verifiedBy", source = "profile.verifiedBy")
    SellerVerificationResponse toResponse(UserEntity user, SellerProfileEntity profile);
}
