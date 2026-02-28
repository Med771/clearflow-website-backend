package backend.website.clearflow.logic.stat.report;

import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.ForbiddenException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonthlyPromoReportServiceImpl implements MonthlyPromoReportService {

    private final AuthContextService authContextService;
    private final UserRepository userRepository;
    private final MonthlyPromoReportDao monthlyPromoReportDao;
    private final PdfMonthlyReportRenderer pdfMonthlyReportRenderer;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateMonthlyPromoReport(UUID sellerId, YearMonth month) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        YearMonth targetMonth = month != null ? month : YearMonth.now();

        MonthlyPromoReportHeader header = monthlyPromoReportDao.loadSellerHeader(targetSellerId)
                .orElseThrow(() -> new NotFoundException("Seller not found"));
        List<MonthlyPromoReportRow> rows = monthlyPromoReportDao.loadPromoRows(
                targetSellerId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
        long totalItemsSold = rows.stream().mapToLong(MonthlyPromoReportRow::itemsSold).sum();
        BigDecimal totalRevenue = rows.stream()
                .map(MonthlyPromoReportRow::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MonthlyPromoReport report = new MonthlyPromoReport(
                targetMonth,
                header,
                rows,
                totalItemsSold,
                totalRevenue
        );
        return pdfMonthlyReportRenderer.render(report);
    }

    private UUID resolveTargetSellerId(UserEntity actor, UUID requestedSellerId) {
        if (actor.getRole() == UserRole.SELLER) {
            return actor.getId();
        }
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.OWNER) {
            if (requestedSellerId == null) {
                throw new BadRequestException("sellerId is required for admin/owner");
            }
            UserEntity seller = userRepository.findById(requestedSellerId).orElseThrow(() -> new NotFoundException("Seller not found"));
            if (seller.getRole() != UserRole.SELLER) {
                throw new BadRequestException("sellerId must refer to seller role");
            }
            return requestedSellerId;
        }
        throw new ForbiddenException("Role cannot generate report");
    }
}
