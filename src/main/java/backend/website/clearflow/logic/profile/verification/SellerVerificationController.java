package backend.website.clearflow.logic.profile.verification;

import backend.website.clearflow.logic.profile.verification.dto.RejectSellerRequest;
import backend.website.clearflow.logic.profile.verification.dto.SellerVerificationResponse;
import backend.website.clearflow.model.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/verification/sellers")
@RequiredArgsConstructor
@Tag(name = "Верификация продавцов", description = "Модерация и проверка профилей продавцов")
public class SellerVerificationController {

    private final SellerVerificationService sellerVerificationService;

    @GetMapping
    @Operation(summary = "Список заявок на верификацию", description = "Возвращает список профилей продавцов для проверки")
    public PageResponse<SellerVerificationResponse> list(
            @Parameter(description = "Фильтр по статусу верификации") @RequestParam(required = false) SellerVerificationStatus status,
            @Parameter(description = "Поиск по ФИО, компании, ИНН или телефону") @RequestParam(required = false) String search,
            @Parameter(description = "Пагинация и сортировка. Формат sort: field,asc|desc")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(sellerVerificationService.list(status, search, pageable));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить профиль продавца для проверки", description = "Возвращает подробные данные профиля продавца по userId")
    public SellerVerificationResponse getByUserId(@PathVariable UUID userId) {
        return sellerVerificationService.getByUserId(userId);
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Одобрить продавца", description = "Подтверждает профиль продавца и переводит статус в APPROVED")
    public SellerVerificationResponse approve(@PathVariable UUID userId) {
        return sellerVerificationService.approve(userId);
    }

    @PostMapping("/{userId}/reject")
    @Operation(summary = "Отклонить продавца", description = "Отклоняет профиль продавца с комментарием модератора")
    public SellerVerificationResponse reject(@PathVariable UUID userId, @Valid @RequestBody RejectSellerRequest request) {
        return sellerVerificationService.reject(userId, request.comment());
    }
}
