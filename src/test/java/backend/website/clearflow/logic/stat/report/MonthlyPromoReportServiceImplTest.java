package backend.website.clearflow.logic.stat.report;

import backend.website.clearflow.logic.report.*;
import backend.website.clearflow.logic.report.dto.MonthlyReportListItem;
import backend.website.clearflow.logic.report.dto.MonthlyPromoReport;
import backend.website.clearflow.logic.report.dto.MonthlyPromoReportHeader;
import backend.website.clearflow.logic.report.dto.MonthlyPromoReportRow;
import backend.website.clearflow.logic.user.UserEntity;
import backend.website.clearflow.logic.user.UserRepository;
import backend.website.clearflow.model.UserRole;
import backend.website.clearflow.model.error.BadRequestException;
import backend.website.clearflow.model.error.NotFoundException;
import backend.website.clearflow.security.AuthContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyPromoReportServiceImplTest {

    @Mock
    private AuthContextService authContextService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MonthlyPromoReportDao monthlyPromoReportDao;
    @Mock
    private PdfMonthlyReportRenderer pdfMonthlyReportRenderer;
    @Mock
    private MonthlyReportRepository monthlyReportRepository;

    @InjectMocks
    private MonthlyPromoReportServiceImpl service;

    @Test
    void sellerGetsOwnMonthlyReportWithTotals() {
        UUID sellerId = UUID.randomUUID();
        UserEntity seller = new UserEntity();
        seller.setId(sellerId);
        seller.setRole(UserRole.SELLER);
        when(authContextService.currentActorOrThrow()).thenReturn(seller);

        MonthlyPromoReportHeader header = new MonthlyPromoReportHeader(
                sellerId, "seller@test.com", "Иванов Иван", "ООО Тест", "7701234567",
                "Банк", "044525225", "123", "456", "Москва"
        );
        when(monthlyPromoReportDao.loadSellerHeader(sellerId)).thenReturn(Optional.of(header));
        when(monthlyPromoReportDao.loadPromoRows(sellerId, YearMonth.of(2026, 2).atDay(1), YearMonth.of(2026, 2).atEndOfMonth()))
                .thenReturn(List.of(
                        new MonthlyPromoReportRow(UUID.randomUUID(), "PROMO1", "Товар 1", 10, new BigDecimal("1500.50")),
                        new MonthlyPromoReportRow(UUID.randomUUID(), "PROMO2", "Товар 2", 5, new BigDecimal("499.50"))
                ));
        byte[] expected = "pdf".getBytes();
        when(pdfMonthlyReportRenderer.render(org.mockito.ArgumentMatchers.any())).thenReturn(expected);
        when(monthlyReportRepository.findBySellerIdAndReportMonth(sellerId, "2026-02")).thenReturn(Optional.empty());

        LocalDate invoiceDate = LocalDate.of(2026, 2, 20);
        byte[] actual = service.generateMonthlyPromoReport(null, YearMonth.of(2026, 2), invoiceDate);

        assertArrayEquals(expected, actual);
        ArgumentCaptor<MonthlyPromoReport> captor = ArgumentCaptor.forClass(MonthlyPromoReport.class);
        verify(pdfMonthlyReportRenderer).render(captor.capture());
        MonthlyPromoReport report = captor.getValue();
        assertEquals(15L, report.totalItemsSold());
        assertEquals(new BigDecimal("2000.00"), report.totalRevenue());
        assertEquals(2, report.rows().size());
        assertEquals(invoiceDate, report.invoiceDate());
        assertEquals("ООО Тест", report.recipient().name());
        assertEquals("ООО Тест", report.payer().name());

        ArgumentCaptor<MonthlyReportEntity> saveCaptor = ArgumentCaptor.forClass(MonthlyReportEntity.class);
        verify(monthlyReportRepository).save(saveCaptor.capture());
        MonthlyReportEntity saved = saveCaptor.getValue();
        assertEquals(sellerId, saved.getSellerId());
        assertEquals("2026-02", saved.getReportMonth());
        assertEquals(invoiceDate, saved.getInvoiceDate());
        assertArrayEquals(expected, saved.getPdfContent());
    }

    @Test
    void sellerGetsCachedReportWhenExists() {
        UUID sellerId = UUID.randomUUID();
        UserEntity seller = new UserEntity();
        seller.setId(sellerId);
        seller.setRole(UserRole.SELLER);
        when(authContextService.currentActorOrThrow()).thenReturn(seller);

        byte[] cached = "cached-pdf".getBytes();
        MonthlyReportEntity existing = new MonthlyReportEntity();
        existing.setSellerId(sellerId);
        existing.setReportMonth("2026-02");
        existing.setInvoiceDate(LocalDate.of(2026, 2, 15));
        existing.setPdfContent(cached);
        when(monthlyReportRepository.findBySellerIdAndReportMonth(sellerId, "2026-02"))
                .thenReturn(Optional.of(existing));

        byte[] actual = service.generateMonthlyPromoReport(null, YearMonth.of(2026, 2), null);

        assertArrayEquals(cached, actual);
        verify(pdfMonthlyReportRenderer, never()).render(any());
        verify(monthlyReportRepository, never()).save(any());
    }

    @Test
    void adminMustPassSellerId() {
        UserEntity admin = new UserEntity();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);
        when(authContextService.currentActorOrThrow()).thenReturn(admin);

        assertThrows(BadRequestException.class, () -> service.generateMonthlyPromoReport(null, YearMonth.now(), null));
    }

    @Test
    void adminCannotUseNonSellerTarget() {
        UUID targetId = UUID.randomUUID();
        UserEntity admin = new UserEntity();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);
        when(authContextService.currentActorOrThrow()).thenReturn(admin);

        UserEntity manager = new UserEntity();
        manager.setId(targetId);
        manager.setRole(UserRole.MANAGER);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(manager));

        assertThrows(BadRequestException.class, () -> service.generateMonthlyPromoReport(targetId, YearMonth.now(), null));
    }

    @Test
    void throwsWhenSellerHeaderMissing() {
        UUID sellerId = UUID.randomUUID();
        UserEntity owner = new UserEntity();
        owner.setId(UUID.randomUUID());
        owner.setRole(UserRole.OWNER);
        when(authContextService.currentActorOrThrow()).thenReturn(owner);

        UserEntity seller = new UserEntity();
        seller.setId(sellerId);
        seller.setRole(UserRole.SELLER);
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(monthlyPromoReportDao.loadSellerHeader(sellerId)).thenReturn(Optional.empty());

        when(monthlyReportRepository.findBySellerIdAndReportMonth(sellerId, "2026-01")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.generateMonthlyPromoReport(sellerId, YearMonth.of(2026, 1), null));
    }

    @Test
    void listReportsSellerSeesOwn() {
        UUID sellerId = UUID.randomUUID();
        UserEntity seller = new UserEntity();
        seller.setId(sellerId);
        seller.setRole(UserRole.SELLER);
        when(authContextService.currentActorOrThrow()).thenReturn(seller);

        MonthlyReportEntity e = new MonthlyReportEntity();
        e.setId(UUID.randomUUID());
        e.setSellerId(sellerId);
        e.setReportMonth("2026-01");
        e.setInvoiceDate(LocalDate.of(2026, 1, 10));
        e.setCreatedAt(Instant.now());
        when(monthlyReportRepository.findAllBySellerIdOrderByReportMonthDesc(sellerId))
                .thenReturn(List.of(e));

        List<MonthlyReportListItem> list = service.listReports();

        assertEquals(1, list.size());
        assertEquals(e.getId(), list.getFirst().id());
        assertEquals(sellerId, list.getFirst().sellerId());
        assertEquals("2026-01", list.getFirst().reportMonth());
    }

    @Test
    void listReportsOwnerSeesAll() {
        UserEntity owner = new UserEntity();
        owner.setId(UUID.randomUUID());
        owner.setRole(UserRole.OWNER);
        when(authContextService.currentActorOrThrow()).thenReturn(owner);

        MonthlyReportEntity e = new MonthlyReportEntity();
        e.setId(UUID.randomUUID());
        e.setSellerId(UUID.randomUUID());
        e.setReportMonth("2026-02");
        e.setInvoiceDate(LocalDate.of(2026, 2, 1));
        e.setCreatedAt(Instant.now());
        when(monthlyReportRepository.findAllByOrderByReportMonthDesc()).thenReturn(List.of(e));

        List<MonthlyReportListItem> list = service.listReports();

        assertEquals(1, list.size());
        assertEquals(e.getId(), list.getFirst().id());
    }
}
