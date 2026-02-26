package backend.website.clearflow.logic.stat;

import backend.website.clearflow.logic.stat.dto.PromoStatDailyAggregateResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatDailyResponse;
import backend.website.clearflow.logic.stat.dto.ProductStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.UpsertPromoStatDailyRequest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface PromoStatService {
    PromoStatDailyResponse upsert(UpsertPromoStatDailyRequest request);

    List<PromoStatDailyAggregateResponse> getDaily(UUID sellerId, UUID promoCodeId, UUID productId, LocalDate from, LocalDate to);

    PromoStatsDashboardResponse getDashboard(UUID sellerId, YearMonth month, Integer topLimit);

    ProductStatsDashboardResponse getProductDashboard(UUID sellerId, UUID productId, YearMonth month, Integer topLimit);
}
