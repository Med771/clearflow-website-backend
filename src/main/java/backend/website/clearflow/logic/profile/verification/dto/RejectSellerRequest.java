package backend.website.clearflow.logic.profile.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSellerRequest(
        @NotBlank @Size(max = 2000) String comment
) {
}
