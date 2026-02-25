package backend.website.clearflow.logic.profile;

import backend.website.clearflow.logic.profile.dto.MyProfileResponse;
import backend.website.clearflow.logic.profile.dto.UpdateMyProfileRequest;
import backend.website.clearflow.logic.profile.photo.ProfilePhotoService;
import backend.website.clearflow.logic.profile.photo.dto.ProfilePhotoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfilePhotoService profilePhotoService;

    @GetMapping("/me")
    public MyProfileResponse getMyProfile() {
        return profileService.getMyProfile();
    }

    @PatchMapping("/me")
    public MyProfileResponse patchMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        return profileService.updateMyProfile(request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyProfile() {
        profileService.deleteMyProfile();
    }

    @GetMapping("/me/photo")
    public ProfilePhotoResponse getMyPhoto() {
        return profilePhotoService.getMyPhoto();
    }

    @PutMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfilePhotoResponse uploadMyPhoto(@RequestParam("file") MultipartFile file) {
        return profilePhotoService.uploadMyPhoto(file);
    }

    @DeleteMapping("/me/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyPhoto() {
        profilePhotoService.deleteMyPhoto();
    }

    @GetMapping("/me/photo/file")
    public ResponseEntity<Resource> getMyPhotoFile() {
        ProfilePhotoResponse meta = profilePhotoService.getMyPhoto();
        Resource resource = profilePhotoService.loadMyPhotoResource();
        MediaType mediaType = meta.mimeType() != null ? MediaType.parseMediaType(meta.mimeType()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
