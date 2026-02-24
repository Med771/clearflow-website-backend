package backend.website.clearflow.logic.profile.dto;

public record ProfilePhotoResponse(
        boolean hasPhoto,
        String mimeType,
        long fileSize,
        String checksumSha256
) {
}
