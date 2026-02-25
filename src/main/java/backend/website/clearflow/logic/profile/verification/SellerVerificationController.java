package backend.website.clearflow.logic.profile.verification;

import backend.website.clearflow.logic.profile.verification.dto.RejectSellerRequest;
import backend.website.clearflow.logic.profile.verification.dto.SellerVerificationResponse;
import backend.website.clearflow.model.PageResponse;
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
public class SellerVerificationController {

    private final SellerVerificationService sellerVerificationService;

    @GetMapping
    public PageResponse<SellerVerificationResponse> list(
            @RequestParam(required = false) SellerVerificationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(sellerVerificationService.list(status, search, pageable));
    }

    @GetMapping("/{userId}")
    public SellerVerificationResponse getByUserId(@PathVariable UUID userId) {
        return sellerVerificationService.getByUserId(userId);
    }

    @PostMapping("/{userId}/approve")
    public SellerVerificationResponse approve(@PathVariable UUID userId) {
        return sellerVerificationService.approve(userId);
    }

    @PostMapping("/{userId}/reject")
    public SellerVerificationResponse reject(@PathVariable UUID userId, @Valid @RequestBody RejectSellerRequest request) {
        return sellerVerificationService.reject(userId, request.comment());
    }
}
