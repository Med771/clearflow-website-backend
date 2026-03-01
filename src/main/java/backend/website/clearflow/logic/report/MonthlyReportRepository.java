package backend.website.clearflow.logic.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReportEntity, UUID> {

    Optional<MonthlyReportEntity> findBySellerIdAndReportMonth(UUID sellerId, String reportMonth);

    List<MonthlyReportEntity> findAllBySellerIdOrderByReportMonthDesc(UUID sellerId);

    List<MonthlyReportEntity> findAllByOrderByReportMonthDesc();
}
