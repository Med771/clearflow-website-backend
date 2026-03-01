package backend.website.clearflow.logic.report;

import backend.website.clearflow.logic.report.dto.MonthlyPromoReportHeader;
import backend.website.clearflow.logic.report.dto.MonthlyPromoReportRow;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MonthlyPromoReportDao {

    private final EntityManager entityManager;

    public Optional<MonthlyPromoReportHeader> loadSellerHeader(UUID sellerId) {
        String sql = """
                SELECT
                    u.id,
                    u.email,
                    sp.full_name,
                    sp.company_name,
                    sp.inn,
                    sp.bank_name,
                    sp.bik,
                    sp.settlement_account,
                    sp.corporate_account,
                    sp.address
                FROM users u
                LEFT JOIN seller_profiles sp ON sp.user_id = u.id
                WHERE u.id = :sellerId AND u.role = 'SELLER'
                """;
        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("sellerId", sellerId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = (Object[]) rows.getFirst();
        return Optional.of(new MonthlyPromoReportHeader(
                UUID.fromString(String.valueOf(row[0])),
                asString(row[1]),
                asString(row[2]),
                asString(row[3]),
                asString(row[4]),
                asString(row[5]),
                asString(row[6]),
                asString(row[7]),
                asString(row[8]),
                asString(row[9])
        ));
    }

    public List<MonthlyPromoReportRow> loadPromoRows(UUID sellerId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    pc.id AS promo_code_id,
                    pc.name AS promo_code_name,
                    p.name AS product_name,
                    COALESCE(SUM(psd.items_count), 0) AS items_sold,
                    COALESCE(SUM(psd.revenue), 0)::numeric(18,2) AS revenue
                FROM promo_stats_daily psd
                JOIN promo_codes pc ON pc.id = psd.promo_code_id
                JOIN products p ON p.id = psd.product_id
                WHERE psd.seller_id = :sellerId
                  AND psd.stat_date BETWEEN :fromDate AND :toDate
                GROUP BY pc.id, pc.name, p.name
                ORDER BY revenue DESC, items_sold DESC, promo_code_name, product_name
                """;
        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("sellerId", sellerId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getResultList();
        return rows.stream()
                .map(raw -> (Object[]) raw)
                .map(row -> new MonthlyPromoReportRow(
                        UUID.fromString(String.valueOf(row[0])),
                        asString(row[1]),
                        asString(row[2]),
                        ((Number) row[3]).longValue(),
                        asBigDecimal(row[4])
                ))
                .toList();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }
}
