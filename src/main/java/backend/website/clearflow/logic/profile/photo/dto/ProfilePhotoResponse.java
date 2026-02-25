package backend.website.clearflow.logic.profile.photo.dto;

public record ProfilePhotoResponse(
        boolean hasPhoto,
        String mimeType,
        long fileSize,
        String checksumSha256
) {
    public static ProfilePhotoResponse empty() {
        return new ProfilePhotoResponse(false, null, 0L, null);
    }
}
