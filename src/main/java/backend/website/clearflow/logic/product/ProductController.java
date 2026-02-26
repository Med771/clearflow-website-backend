package backend.website.clearflow.logic.product;

import backend.website.clearflow.logic.product.dto.CreateProductRequest;
import backend.website.clearflow.logic.product.dto.ProductResponse;
import backend.website.clearflow.logic.product.dto.UpdateProductRequest;
import backend.website.clearflow.model.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/products")
@Tag(name = "Товары", description = "Управление товарами продавцов")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Список товаров", description = "Возвращает список товаров с фильтрацией и пагинацией")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список товаров получен"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public PageResponse<ProductResponse> list(
            @Parameter(description = "Идентификатор продавца (для OWNER/ADMIN)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Поиск по названию товара") @RequestParam(required = false) String search,
            @Parameter(description = "Включать неактивные товары") @RequestParam(defaultValue = "false") boolean includeInactive,
            @Parameter(description = "Пагинация и сортировка. Формат sort: field,asc|desc")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(productService.list(sellerId, search, includeInactive, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить товар", description = "Возвращает товар по идентификатору")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать товар", description = "Создает новый товар продавца")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить товар", description = "Обновляет параметры товара")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }
}
