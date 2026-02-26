package backend.website.clearflow.logic.promo;

import backend.website.clearflow.logic.promo.dto.CreatePromoCodeRequest;
import backend.website.clearflow.logic.promo.dto.PromoCodeResponse;
import backend.website.clearflow.logic.promo.dto.UpdatePromoCodeRequest;
import backend.website.clearflow.model.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/promo-codes")
@RequiredArgsConstructor
@Tag(name = "Промокоды", description = "Управление промокодами (акциями) продавцов")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping
    @Operation(summary = "Список промокодов", description = "Возвращает список промокодов с фильтрацией и пагинацией")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список промокодов получен"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public PageResponse<PromoCodeResponse> list(
            @Parameter(description = "Идентификатор продавца (для OWNER/ADMIN)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Поиск по названию промокода") @RequestParam(required = false) String search,
            @Parameter(description = "Включать неактивные промокоды") @RequestParam(defaultValue = "false") boolean includeInactive,
            @Parameter(description = "Пагинация и сортировка. Формат sort: field,asc|desc")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(promoCodeService.list(sellerId, search, includeInactive, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить промокод", description = "Возвращает промокод по идентификатору")
    public PromoCodeResponse getById(@PathVariable UUID id) {
        return promoCodeService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать промокод", description = "Создает новый промокод продавца")
    public PromoCodeResponse create(@Valid @RequestBody CreatePromoCodeRequest request) {
        return promoCodeService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить промокод", description = "Обновляет параметры промокода и привязку товаров")
    public PromoCodeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePromoCodeRequest request) {
        return promoCodeService.update(id, request);
    }
}
