package backend.website.clearflow.logic.promo;

import backend.website.clearflow.logic.promo.dto.CreatePromoCodeRequest;
import backend.website.clearflow.logic.promo.dto.PromoCodeResponse;
import backend.website.clearflow.logic.promo.dto.UpdatePromoCodeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PromoCodeService {
    Page<PromoCodeResponse> list(UUID sellerId, String search, boolean includeInactive, Pageable pageable);

    PromoCodeResponse getById(UUID id);

    PromoCodeResponse create(CreatePromoCodeRequest request);

    PromoCodeResponse update(UUID id, UpdatePromoCodeRequest request);
}
