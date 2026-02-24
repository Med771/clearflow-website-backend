package backend.website.clearflow.logic.profile;

import backend.website.clearflow.config.property.StorageProperties;
import backend.website.clearflow.helper.FileStorageService;
import backend.website.clearflow.logic.profile.dto.ProfilePhotoResponse;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class ProfilePhotoServiceImpl implements ProfilePhotoService {

    private static final String LOCAL_STORAGE = "LOCAL";

    private final AuthContextService authContextService;
    private final UserPhotoRepository userPhotoRepository;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    public ProfilePhotoServiceImpl(
            AuthContextService authContextService,
            UserPhotoRepository userPhotoRepository,
            FileStorageService fileStorageService,
            StorageProperties storageProperties
    ) {
        this.authContextService = authContextService;
        this.userPhotoRepository = userPhotoRepository;
        this.fileStorageService = fileStorageService;
        this.storageProperties = storageProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfilePhotoResponse getMyPhoto() {
        UserEntity actor = authContextService.currentActorOrThrow();
        return userPhotoRepository.findByUserId(actor.getId())
                .map(this::toResponse)
                .orElse(new ProfilePhotoResponse(false, null, 0L, null));
    }

    @Override
    @Transactional
    public ProfilePhotoResponse uploadMyPhoto(MultipartFile file) {
        UserEntity actor = authContextService.currentActorOrThrow();
        String contentType = validateAndGetContentType(file);
        byte[] bytes = toBytes(file);
        String extension = resolveExtension(contentType);

        UserPhotoEntity existing = userPhotoRepository.findByUserId(actor.getId()).orElse(null);
        String relativePath = fileStorageService.saveProfilePhoto(actor.getId(), bytes, extension);

        if (existing != null) {
            fileStorageService.delete(existing.getRelativePath());
        }

        UserPhotoEntity photo = existing != null ? existing : new UserPhotoEntity();
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
        UserEntity actor = authContextService.currentActorOrThrow();
        userPhotoRepository.findByUserId(actor.getId()).ifPresent(photo -> {
            fileStorageService.delete(photo.getRelativePath());
            userPhotoRepository.delete(photo);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadMyPhotoResource() {
        UserEntity actor = authContextService.currentActorOrThrow();
        UserPhotoEntity photo = userPhotoRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new NotFoundException("Profile photo not found"));
        return fileStorageService.loadAsResource(photo.getRelativePath());
    }

    private ProfilePhotoResponse toResponse(UserPhotoEntity photo) {
        return new ProfilePhotoResponse(
                true,
                photo.getMimeType(),
                photo.getFileSize(),
                photo.getChecksumSha256()
        );
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
        } catch (Exception exception) {
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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not calculate checksum", exception);
        }
    }
}
