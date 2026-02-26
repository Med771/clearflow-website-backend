package backend.website.clearflow.logic.profile;

import backend.website.clearflow.logic.profile.dto.MyProfileResponse;
import backend.website.clearflow.logic.profile.dto.UpdateMyProfileRequest;

public interface ProfileService {
    MyProfileResponse getMyProfile();

    MyProfileResponse updateMyProfile(UpdateMyProfileRequest request);

    void deleteMyProfile();
}
