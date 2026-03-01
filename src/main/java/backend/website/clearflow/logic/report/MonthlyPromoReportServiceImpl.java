package backend.website.clearflow.logic.report;

import backend.website.clearflow.logic.report.dto.*;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonthlyPromoReportServiceImpl implements MonthlyPromoReportService {

    private final AuthContextService authContextService;
    private final UserRepository userRepository;
    private final MonthlyPromoReportDao monthlyPromoReportDao;
    private final PdfMonthlyReportRenderer pdfMonthlyReportRenderer;
    private final MonthlyReportRepository monthlyReportRepository;

    @Override
    @Transactional
    public byte[] generateMonthlyPromoReport(UUID sellerId, YearMonth month, LocalDate invoiceDate) {
        UserEntity actor = authContextService.currentActorOrThrow();
        UUID targetSellerId = resolveTargetSellerId(actor, sellerId);
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        LocalDate effectiveInvoiceDate = invoiceDate != null ? invoiceDate : LocalDate.now();

        Optional<MonthlyReportEntity> existing = monthlyReportRepository.findBySellerIdAndReportMonth(
                targetSellerId, targetMonth.toString());
        if (existing.isPresent()) {
            return existing.get().getPdfContent();
        }

        byte[] pdfContent = buildAndRenderReport(targetSellerId, targetMonth, effectiveInvoiceDate);

        MonthlyReportEntity entity = new MonthlyReportEntity();
        entity.setSellerId(targetSellerId);
        entity.setReportMonth(targetMonth.toString());
        entity.setInvoiceDate(effectiveInvoiceDate);
        entity.setPdfContent(pdfContent);
        monthlyReportRepository.save(entity);

        return pdfContent;
    }

    private byte[] buildAndRenderReport(UUID targetSellerId, YearMonth targetMonth, LocalDate effectiveInvoiceDate) {
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
        MonthlyPromoReportPartyDetails recipient = new MonthlyPromoReportPartyDetails(
                firstNotBlank(header.companyName(), header.fullName()),
                header.inn(),
                header.bankName(),
                header.bik(),
                header.settlementAccount(),
                header.corporateAccount(),
                header.address()
        );

        MonthlyPromoReport report = new MonthlyPromoReport(
                targetMonth,
                resolveInvoiceNumber(targetMonth, targetSellerId),
                effectiveInvoiceDate,
                recipient,
                recipient,
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

    private String resolveInvoiceNumber(YearMonth month, UUID sellerId) {
        String shortSellerId = sellerId.toString().substring(0, 8);
        return month.toString().replace("-", "") + "-" + shortSellerId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyReportListItem> listReports() {
        UserEntity actor = authContextService.currentActorOrThrow();
        List<MonthlyReportEntity> entities;
        if (actor.getRole() == UserRole.SELLER) {
            entities = monthlyReportRepository.findAllBySellerIdOrderByReportMonthDesc(actor.getId());
        } else if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.OWNER) {
            entities = monthlyReportRepository.findAllByOrderByReportMonthDesc();
        } else {
            throw new ForbiddenException("Role cannot list reports");
        }
        return entities.stream()
                .map(e -> new MonthlyReportListItem(
                        e.getId(),
                        e.getSellerId(),
                        e.getReportMonth(),
                        e.getInvoiceDate(),
                        e.getCreatedAt()
                ))
                .toList();
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
