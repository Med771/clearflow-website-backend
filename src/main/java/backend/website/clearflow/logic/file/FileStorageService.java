package backend.website.clearflow.logic.file;

import org.springframework.core.io.Resource;

import java.util.UUID;

public interface FileStorageService {
    String saveProfilePhoto(UUID userId, byte[] bytes, String extension);

    void delete(String relativePath);

    Resource loadAsResource(String relativePath);
}
