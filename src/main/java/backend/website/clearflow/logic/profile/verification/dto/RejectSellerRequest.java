package backend.website.clearflow.logic.profile.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на отклонение продавца")
public record RejectSellerRequest(
        @Schema(description = "Комментарий модератора при отклонении", example = "Не заполнены обязательные реквизиты")
        @NotBlank @Size(max = 2000) String comment
) {
}
