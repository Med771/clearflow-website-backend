package backend.website.clearflow.logic.stat;

import backend.website.clearflow.logic.stat.dto.PromoStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatsDailyRevenuePoint;
import backend.website.clearflow.logic.stat.dto.PromoStatsTopProductResponse;
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
public class PromoStatsDashboardDao {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public PromoStatsDashboardResponse load(UUID sellerId, YearMonth targetMonth, int topLimit) {
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        YearMonth previousMonth = targetMonth.minusMonths(1);
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();
        LocalDate last7Start = LocalDate.now().minusDays(6);
        LocalDate last7End = LocalDate.now();

        String sql = """
                WITH input AS (
                    SELECT
                        CAST(:sellerId AS uuid) AS seller_id,
                        CAST(:monthStart AS date) AS month_start,
                        CAST(:monthEnd AS date) AS month_end,
                        CAST(:prevStart AS date) AS prev_start,
                        CAST(:prevEnd AS date) AS prev_end,
                        CAST(:last7Start AS date) AS last7_start,
                        CAST(:last7End AS date) AS last7_end
                ),
                month_rows AS (
                    SELECT psd.*
                    FROM promo_stats_daily psd
                    JOIN input i ON i.seller_id = psd.seller_id
                    WHERE psd.stat_date BETWEEN i.month_start AND i.month_end
                ),
                prev_rows AS (
                    SELECT psd.*
                    FROM promo_stats_daily psd
                    JOIN input i ON i.seller_id = psd.seller_id
                    WHERE psd.stat_date BETWEEN i.prev_start AND i.prev_end
                ),
                kpi_month AS (
                    SELECT
                        COALESCE(SUM(items_count), 0) AS month_items_sold,
                        COALESCE(SUM(revenue), 0)::numeric(18,2) AS month_revenue
                    FROM month_rows
                ),
                kpi_last7 AS (
                    SELECT
                        COALESCE(SUM(psd.revenue), 0)::numeric(18,2) AS last7_revenue
                    FROM promo_stats_daily psd
                    JOIN input i ON i.seller_id = psd.seller_id
                    WHERE psd.stat_date BETWEEN i.last7_start AND i.last7_end
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
                top_products AS (
                    SELECT
                        p.id AS product_id,
                        p.name AS product_name,
                        COALESCE(SUM(mr.items_count), 0) AS month_items_sold,
                        COALESCE(SUM(mr.revenue), 0)::numeric(18,2) AS month_revenue
                    FROM month_rows mr
                    JOIN products p ON p.id = mr.product_id
                    GROUP BY p.id, p.name
                    ORDER BY month_items_sold DESC, month_revenue DESC, p.name
                    LIMIT :topLimit
                )
                SELECT
                    km.month_items_sold,
                    km.month_revenue,
                    kl.last7_revenue,
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
                                    'productId', tp.product_id,
                                    'productName', tp.product_name,
                                    'monthItemsSold', tp.month_items_sold,
                                    'monthRevenue', tp.month_revenue
                                )
                                ORDER BY tp.month_items_sold DESC, tp.month_revenue DESC, tp.product_name
                            ),
                            '[]'::json
                        )
                        FROM top_products tp
                    ) AS top_products_json
                FROM kpi_month km
                CROSS JOIN kpi_last7 kl
                """;

        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("sellerId", sellerId)
                .setParameter("monthStart", monthStart)
                .setParameter("monthEnd", monthEnd)
                .setParameter("prevStart", previousStart)
                .setParameter("prevEnd", previousEnd)
                .setParameter("last7Start", last7Start)
                .setParameter("last7End", last7End)
                .setParameter("topLimit", topLimit)
                .getSingleResult();

        long monthItemsSold = toLong(row[0]);
        BigDecimal monthRevenue = toBigDecimal(row[1]);
        BigDecimal last7DaysRevenue = toBigDecimal(row[2]);
        List<PromoStatsDailyRevenuePoint> dailyRevenue = mapDailyRevenueJson(row[3]);
        List<PromoStatsTopProductResponse> topProducts = mapTopProductsJson(row[4]);

        return new PromoStatsDashboardResponse(
                targetMonth.getYear(),
                targetMonth.getMonthValue(),
                monthItemsSold,
                monthRevenue,
                last7DaysRevenue,
                dailyRevenue,
                topProducts
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
            throw new IllegalStateException("Failed to parse daily revenue JSON", exception);
        }
    }

    private List<PromoStatsTopProductResponse> mapTopProductsJson(Object jsonObject) {
        if (jsonObject == null) {
            return List.of();
        }
        try {
            JsonNode arrayNode = objectMapper.readTree(String.valueOf(jsonObject));
            List<PromoStatsTopProductResponse> result = new ArrayList<>(arrayNode.size());
            for (JsonNode item : arrayNode) {
                UUID productId = UUID.fromString(item.path("productId").asText());
                String productName = item.path("productName").asText();
                long monthItemsSold = item.path("monthItemsSold").asLong();
                BigDecimal monthRevenue = new BigDecimal(item.path("monthRevenue").asText("0"));
                result.add(new PromoStatsTopProductResponse(productId, productName, monthItemsSold, monthRevenue));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse top products JSON", exception);
        }
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : (value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value)));
    }
}
