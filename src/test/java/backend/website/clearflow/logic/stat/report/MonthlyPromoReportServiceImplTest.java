package backend.website.clearflow.logic.stat.report;

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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                        new MonthlyPromoReportRow(UUID.randomUUID(), "PROMO1", 10, new BigDecimal("1500.50")),
                        new MonthlyPromoReportRow(UUID.randomUUID(), "PROMO2", 5, new BigDecimal("499.50"))
                ));
        byte[] expected = "pdf".getBytes();
        when(pdfMonthlyReportRenderer.render(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

        byte[] actual = service.generateMonthlyPromoReport(null, YearMonth.of(2026, 2));

        assertArrayEquals(expected, actual);
        ArgumentCaptor<MonthlyPromoReport> captor = ArgumentCaptor.forClass(MonthlyPromoReport.class);
        verify(pdfMonthlyReportRenderer).render(captor.capture());
        MonthlyPromoReport report = captor.getValue();
        assertEquals(15L, report.totalItemsSold());
        assertEquals(new BigDecimal("2000.00"), report.totalRevenue());
        assertEquals(2, report.rows().size());
    }

    @Test
    void adminMustPassSellerId() {
        UserEntity admin = new UserEntity();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);
        when(authContextService.currentActorOrThrow()).thenReturn(admin);

        assertThrows(BadRequestException.class, () -> service.generateMonthlyPromoReport(null, YearMonth.now()));
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

        assertThrows(BadRequestException.class, () -> service.generateMonthlyPromoReport(targetId, YearMonth.now()));
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

        assertThrows(NotFoundException.class, () -> service.generateMonthlyPromoReport(sellerId, YearMonth.of(2026, 1)));
    }
}
