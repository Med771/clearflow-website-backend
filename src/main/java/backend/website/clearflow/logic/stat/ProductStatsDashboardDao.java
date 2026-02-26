package backend.website.clearflow.logic.stat;

import backend.website.clearflow.logic.stat.dto.ProductStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.ProductStatsTopPromoCodeResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatsDailyRevenuePoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductStatsDashboardDao {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductStatsDashboardResponse load(UUID sellerId, UUID productId, YearMonth targetMonth, int topLimit) {
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        YearMonth previousMonth = targetMonth.minusMonths(1);
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();

        String sql = """
                WITH input AS (
                    SELECT
                        CAST(:sellerId AS uuid) AS seller_id,
                        CAST(:productId AS uuid) AS product_id,
                        CAST(:monthStart AS date) AS month_start,
                        CAST(:monthEnd AS date) AS month_end,
                        CAST(:prevStart AS date) AS prev_start,
                        CAST(:prevEnd AS date) AS prev_end
                ),
                product_meta AS (
                    SELECT p.id AS product_id, p.name AS product_name
                    FROM products p
                    JOIN input i ON i.product_id = p.id AND i.seller_id = p.seller_id
                ),
                month_rows AS (
                    SELECT psd.*
                    FROM promo_stats_daily psd
                    JOIN input i ON i.seller_id = psd.seller_id AND i.product_id = psd.product_id
                    WHERE psd.stat_date BETWEEN i.month_start AND i.month_end
                ),
                prev_rows AS (
                    SELECT psd.*
                    FROM promo_stats_daily psd
                    JOIN input i ON i.seller_id = psd.seller_id AND i.product_id = psd.product_id
                    WHERE psd.stat_date BETWEEN i.prev_start AND i.prev_end
                ),
                kpi_month AS (
                    SELECT
                        COALESCE(SUM(items_count), 0) AS month_items_sold,
                        COALESCE(SUM(revenue), 0)::numeric(18,2) AS month_revenue
                    FROM month_rows
                ),
                daily_current AS (
                    SELECT EXTRACT(day FROM stat_date)::int AS day_num, COALESCE(SUM(revenue), 0)::numeric(18,2) AS revenue
                    FROM month_rows
                    GROUP BY EXTRACT(day FROM stat_date)
                ),
                daily_previous AS (
                    SELECT EXTRACT(day FROM stat_date)::int AS day_num, COALESCE(SUM(revenue), 0)::numeric(18,2) AS revenue
                    FROM prev_rows
                    GROUP BY EXTRACT(day FROM stat_date)
                ),
                daily_graph AS (
                    SELECT
                        gs.day_num,
                        COALESCE(dc.revenue, 0)::numeric(18,2) AS current_revenue,
                        COALESCE(dp.revenue, 0)::numeric(18,2) AS previous_revenue
                    FROM generate_series(1, 31) AS gs(day_num)
                    LEFT JOIN daily_current dc ON dc.day_num = gs.day_num
                    LEFT JOIN daily_previous dp ON dp.day_num = gs.day_num
                ),
                top_promo_codes AS (
                    SELECT
                        pc.id AS promo_code_id,
                        pc.name AS promo_code_name,
                        COALESCE(SUM(mr.items_count), 0) AS month_items_sold,
                        COALESCE(SUM(mr.revenue), 0)::numeric(18,2) AS month_revenue
                    FROM month_rows mr
                    JOIN promo_codes pc ON pc.id = mr.promo_code_id
                    GROUP BY pc.id, pc.name
                    ORDER BY month_revenue DESC, month_items_sold DESC, pc.name
                    LIMIT :topLimit
                )
                SELECT
                    pm.product_id,
                    pm.product_name,
                    km.month_items_sold,
                    km.month_revenue,
                    (
                        SELECT COALESCE(
                            json_agg(
                                json_build_object(
                                    'dayOfMonth', dg.day_num,
                                    'currentRevenue', dg.current_revenue,
                                    'previousRevenue', dg.previous_revenue
                                )
                                ORDER BY dg.day_num
                            ),
                            '[]'::json
                        )
                        FROM daily_graph dg
                    ) AS daily_revenue_json,
                    (
                        SELECT COALESCE(
                            json_agg(
                                json_build_object(
                                    'promoCodeId', tpc.promo_code_id,
                                    'promoCodeName', tpc.promo_code_name,
                                    'monthItemsSold', tpc.month_items_sold,
                                    'monthRevenue', tpc.month_revenue
                                )
                                ORDER BY tpc.month_revenue DESC, tpc.month_items_sold DESC, tpc.promo_code_name
                            ),
                            '[]'::json
                        )
                        FROM top_promo_codes tpc
                    ) AS top_promo_codes_json
                FROM product_meta pm
                CROSS JOIN kpi_month km
                """;

        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("sellerId", sellerId)
                .setParameter("productId", productId)
                .setParameter("monthStart", monthStart)
                .setParameter("monthEnd", monthEnd)
                .setParameter("prevStart", previousStart)
                .setParameter("prevEnd", previousEnd)
                .setParameter("topLimit", topLimit)
                .getSingleResult();

        return new ProductStatsDashboardResponse(
                targetMonth.getYear(),
                targetMonth.getMonthValue(),
                UUID.fromString(String.valueOf(row[0])),
                String.valueOf(row[1]),
                toLong(row[2]),
                toBigDecimal(row[3]),
                mapDailyRevenueJson(row[4]),
                mapTopPromoCodesJson(row[5])
        );
    }

    private List<PromoStatsDailyRevenuePoint> mapDailyRevenueJson(Object jsonObject) {
        if (jsonObject == null) {
            return List.of();
        }
        try {
            JsonNode arrayNode = objectMapper.readTree(String.valueOf(jsonObject));
            List<PromoStatsDailyRevenuePoint> result = new ArrayList<>(arrayNode.size());
            for (JsonNode item : arrayNode) {
                int dayOfMonth = item.path("dayOfMonth").asInt();
                BigDecimal currentRevenue = new BigDecimal(item.path("currentRevenue").asText("0"));
                BigDecimal previousRevenue = new BigDecimal(item.path("previousRevenue").asText("0"));
                result.add(new PromoStatsDailyRevenuePoint(dayOfMonth, currentRevenue, previousRevenue));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse product daily revenue JSON", exception);
        }
    }

    private List<ProductStatsTopPromoCodeResponse> mapTopPromoCodesJson(Object jsonObject) {
        if (jsonObject == null) {
            return List.of();
        }
        try {
            JsonNode arrayNode = objectMapper.readTree(String.valueOf(jsonObject));
            List<ProductStatsTopPromoCodeResponse> result = new ArrayList<>(arrayNode.size());
            for (JsonNode item : arrayNode) {
                UUID promoCodeId = UUID.fromString(item.path("promoCodeId").asText());
                String promoCodeName = item.path("promoCodeName").asText();
                long monthItemsSold = item.path("monthItemsSold").asLong();
                BigDecimal monthRevenue = new BigDecimal(item.path("monthRevenue").asText("0"));
                result.add(new ProductStatsTopPromoCodeResponse(promoCodeId, promoCodeName, monthItemsSold, monthRevenue));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse top promo codes JSON", exception);
        }
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : (value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value)));
    }
}
