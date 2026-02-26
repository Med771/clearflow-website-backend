package backend.website.clearflow.logic.profile.photo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Метаданные фотографии профиля")
public record ProfilePhotoResponse(
        @Schema(description = "Наличие загруженной фотографии")
        boolean hasPhoto,
        @Schema(description = "MIME-тип файла", example = "image/jpeg")
        String mimeType,
        @Schema(description = "Размер файла в байтах")
        long fileSize,
        @Schema(description = "SHA-256 контрольная сумма файла")
        String checksumSha256
) {
    public static ProfilePhotoResponse empty() {
        return new ProfilePhotoResponse(false, null, 0L, null);
    }
}
