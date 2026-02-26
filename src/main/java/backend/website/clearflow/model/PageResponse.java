package backend.website.clearflow.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Универсальный ответ с пагинацией")
public record PageResponse<T>(
        @ArraySchema(schema = @Schema(description = "Данные текущей страницы"))
        List<T> data,
        @Schema(description = "Номер страницы (0-based)", example = "0")
        int page,
        @Schema(description = "Размер страницы", example = "20")
        int size,
        @Schema(description = "Общее количество элементов", example = "125")
        long totalElements,
        @Schema(description = "Общее количество страниц", example = "7")
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
