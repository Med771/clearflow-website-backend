package backend.website.clearflow.logic.profile.photo;

import backend.website.clearflow.logic.profile.photo.dto.ProfilePhotoResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ProfilePhotoService {
    ProfilePhotoResponse getMyPhoto();

    ProfilePhotoResponse uploadMyPhoto(MultipartFile file);

    void deleteMyPhoto();

    Resource loadMyPhotoResource();
}
