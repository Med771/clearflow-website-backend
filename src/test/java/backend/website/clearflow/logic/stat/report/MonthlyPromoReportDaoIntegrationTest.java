package backend.website.clearflow.logic.stat.report;

import backend.website.clearflow.logic.report.MonthlyPromoReportDao;
import backend.website.clearflow.logic.report.dto.MonthlyPromoReportRow;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class MonthlyPromoReportDaoIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MonthlyPromoReportDao monthlyPromoReportDao;

    @Test
    void keepsSeparateRowsForDifferentPromoIdsWithSameName() {
        UUID sellerId = UUID.randomUUID();
        UUID promoA = UUID.randomUUID();
        UUID promoB = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 2, 15);

        entityManager.createNativeQuery("""
                INSERT INTO users (id, email, password, role, is_block, is_active, parent_id, session_version, created_at, updated_at)
                VALUES (:id, :email, 'x', 'SELLER', false, true, null, 0, now(), now())
                """)
                .setParameter("id", sellerId)
                .setParameter("email", "seller-" + sellerId + "@test.local")
                .executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO promo_codes (id, seller_id, name, action_id, is_active, created_at, updated_at)
                VALUES (:id, :sellerId, :name, :actionId, true, now(), now())
                """)
                .setParameter("id", promoA)
                .setParameter("sellerId", sellerId)
                .setParameter("name", "SAME_NAME")
                .setParameter("actionId", 1001L)
                .executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO promo_codes (id, seller_id, name, action_id, is_active, created_at, updated_at)
                VALUES (:id, :sellerId, :name, :actionId, true, now(), now())
                """)
                .setParameter("id", promoB)
                .setParameter("sellerId", sellerId)
                .setParameter("name", "SAME_NAME")
                .setParameter("actionId", 1002L)
                .executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO products (id, seller_id, name, ozon_product_id, is_active, created_at, updated_at)
                VALUES (:id, :sellerId, :name, :ozonProductId, true, now(), now())
                """)
                .setParameter("id", productA)
                .setParameter("sellerId", sellerId)
                .setParameter("name", "ProductA")
                .setParameter("ozonProductId", 2001L)
                .executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO products (id, seller_id, name, ozon_product_id, is_active, created_at, updated_at)
                VALUES (:id, :sellerId, :name, :ozonProductId, true, now(), now())
                """)
                .setParameter("id", productB)
                .setParameter("sellerId", sellerId)
                .setParameter("name", "ProductB")
                .setParameter("ozonProductId", 2002L)
                .executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO promo_stats_daily (id, seller_id, promo_code_id, product_id, stat_date, orders_count, items_count, revenue, created_at, updated_at)
                VALUES (:id, :sellerId, :promoId, :productId, :statDate, 1, :items, :revenue, now(), now())
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("sellerId", sellerId)
                .setParameter("promoId", promoA)
                .setParameter("productId", productA)
                .setParameter("statDate", date)
                .setParameter("items", 2L)
                .setParameter("revenue", 200.00)
                .executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO promo_stats_daily (id, seller_id, promo_code_id, product_id, stat_date, orders_count, items_count, revenue, created_at, updated_at)
                VALUES (:id, :sellerId, :promoId, :productId, :statDate, 1, :items, :revenue, now(), now())
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("sellerId", sellerId)
                .setParameter("promoId", promoB)
                .setParameter("productId", productB)
                .setParameter("statDate", date)
                .setParameter("items", 3L)
                .setParameter("revenue", 300.00)
                .executeUpdate();

        entityManager.flush();

        List<MonthlyPromoReportRow> rows = monthlyPromoReportDao.loadPromoRows(
                sellerId,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28)
        );

        assertEquals(2, rows.size());
        assertEquals("ProductB", rows.get(0).productName());
        assertEquals("ProductA", rows.get(1).productName());
        assertEquals(300.00, rows.get(0).revenue().doubleValue(), 0.001);
        assertEquals(200.00, rows.get(1).revenue().doubleValue(), 0.001);
    }
}
