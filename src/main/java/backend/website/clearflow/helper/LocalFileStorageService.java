package backend.website.clearflow.helper;

import backend.website.clearflow.config.property.StorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.root = Path.of(storageProperties.localRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize local storage directory", exception);
        }
    }

    @Override
    public String saveProfilePhoto(UUID userId, byte[] bytes, String extension) {
        String safeExtension = extension == null || extension.isBlank() ? "bin" : extension.toLowerCase();
        String fileName = UUID.randomUUID() + "." + safeExtension;
        Path userDir = root.resolve("profile").resolve(userId.toString());
        Path filePath = userDir.resolve(fileName);
        try {
            Files.createDirectories(userDir);
            Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save file", exception);
        }
        return root.relativize(filePath).toString().replace("\\", "/");
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(relativePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete file", exception);
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) {
        try {
            Path path = resolveSafe(relativePath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("File is not readable");
            }
            return resource;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load file", exception);
        }
    }

    private Path resolveSafe(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalStateException("Invalid storage path");
        }
        return resolved;
    }
}
