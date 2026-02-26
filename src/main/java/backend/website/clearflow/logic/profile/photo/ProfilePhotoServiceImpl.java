package backend.website.clearflow.logic.profile.photo;

import backend.website.clearflow.config.property.StorageProperties;
import backend.website.clearflow.helper.FileStorageService;
import backend.website.clearflow.logic.profile.photo.dto.ProfilePhotoResponse;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class ProfilePhotoServiceImpl implements ProfilePhotoService {

    private static final String LOCAL_STORAGE = "LOCAL";

    private final AuthContextService authContextService;
    private final UserPhotoRepository userPhotoRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    @Override
    @Transactional(readOnly = true)
    public ProfilePhotoResponse getMyPhoto() {
        UserEntity actor = authContextService.currentActiveActorAllowBlockedOrThrow();
        return userPhotoRepository.findByUserId(actor.getId())
                .map(this::toResponse)
                .orElseGet(ProfilePhotoResponse::empty);
    }

    @Override
    @Transactional
    public ProfilePhotoResponse uploadMyPhoto(MultipartFile file) {
        UserEntity actor = authContextService.currentActiveActorAllowBlockedOrThrow();
        String contentType = validateAndGetContentType(file);
        byte[] bytes = toBytes(file);
        String extension = resolveExtension(contentType);

        UserPhotoEntity photo = userPhotoRepository.findByUserId(actor.getId()).orElseGet(UserPhotoEntity::new);
        if (photo.getRelativePath() != null) {
            fileStorageService.delete(photo.getRelativePath());
        }

        String relativePath = fileStorageService.saveProfilePhoto(actor.getId(), bytes, extension);
        photo.setUserId(actor.getId());
        photo.setStorageType(LOCAL_STORAGE);
        photo.setRelativePath(relativePath);
        photo.setMimeType(contentType);
        photo.setFileSize(bytes.length);
        photo.setChecksumSha256(sha256(bytes));
        return toResponse(userPhotoRepository.save(photo));
    }

    @Override
    @Transactional
    public void deleteMyPhoto() {
        UserEntity actor = authContextService.currentActiveActorAllowBlockedOrThrow();
        userPhotoRepository.findByUserId(actor.getId()).ifPresent(photo -> {
            fileStorageService.delete(photo.getRelativePath());
            userPhotoRepository.delete(photo);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadMyPhotoResource() {
        UserEntity actor = authContextService.currentActiveActorAllowBlockedOrThrow();
        UserPhotoEntity photo = userPhotoRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new NotFoundException("Profile photo not found"));
        return fileStorageService.loadAsResource(photo.getRelativePath());
    }

    private ProfilePhotoResponse toResponse(UserPhotoEntity photo) {
        return new ProfilePhotoResponse(true, photo.getMimeType(), photo.getFileSize(), photo.getChecksumSha256());
    }

    private String validateAndGetContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Photo file is empty");
        }
        if (file.getSize() > storageProperties.maxPhotoBytes()) {
            throw new BadRequestException("Photo file size exceeds limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }
        resolveExtension(contentType);
        return contentType;
    }

    private byte[] toBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read file bytes", exception);
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> throw new BadRequestException("Unsupported image type");
        };
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not calculate checksum", exception);
        }
    }
}
