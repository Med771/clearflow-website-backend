package backend.website.clearflow.logic.profile;

import backend.website.clearflow.logic.profile.dto.MyProfileResponse;
import backend.website.clearflow.logic.profile.dto.UpdateMyProfileRequest;
import backend.website.clearflow.logic.profile.photo.ProfilePhotoService;
import backend.website.clearflow.logic.profile.photo.dto.ProfilePhotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Профиль", description = "Операции с собственным профилем и фотографией")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfilePhotoService profilePhotoService;

    @GetMapping("/me")
    @Operation(summary = "Получить мой профиль", description = "Возвращает профиль текущего аутентифицированного пользователя")
    public MyProfileResponse getMyProfile() {
        return profileService.getMyProfile();
    }

    @PatchMapping("/me")
    @Operation(summary = "Обновить мой профиль", description = "Обновляет редактируемые поля профиля текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    })
    public MyProfileResponse patchMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        return profileService.updateMyProfile(request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить мой профиль", description = "Деактивирует текущий профиль (недоступно для роли OWNER)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Профиль удален"),
            @ApiResponse(responseCode = "403", description = "Операция недоступна для текущей роли")
    })
    public void deleteMyProfile() {
        profileService.deleteMyProfile();
    }

    @GetMapping("/me/photo")
    @Operation(summary = "Получить метаданные фото", description = "Возвращает информацию о фото профиля текущего пользователя")
    public ProfilePhotoResponse getMyPhoto() {
        return profilePhotoService.getMyPhoto();
    }

    @PutMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить фото профиля", description = "Загружает или заменяет фото профиля")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Фото успешно загружено"),
            @ApiResponse(responseCode = "400", description = "Некорректный файл")
    })
    public ProfilePhotoResponse uploadMyPhoto(@RequestParam("file") MultipartFile file) {
        return profilePhotoService.uploadMyPhoto(file);
    }

    @DeleteMapping("/me/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить фото профиля", description = "Удаляет фото профиля текущего пользователя")
    public void deleteMyPhoto() {
        profilePhotoService.deleteMyPhoto();
    }

    @GetMapping("/me/photo/file")
    @Operation(summary = "Скачать фото профиля", description = "Возвращает бинарный файл фото профиля текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл фото возвращен"),
            @ApiResponse(responseCode = "404", description = "Фото не найдено")
    })
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
