package backend.website.clearflow.logic.stat;

import backend.website.clearflow.logic.stat.dto.PromoStatDailyAggregateResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatDailyResponse;
import backend.website.clearflow.logic.stat.dto.ProductStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.PromoStatsDashboardResponse;
import backend.website.clearflow.logic.stat.dto.UpsertPromoStatDailyRequest;
import backend.website.clearflow.logic.stat.report.MonthlyPromoReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/stats/promo")
@RequiredArgsConstructor
@Tag(name = "Статистика", description = "Операции со статистикой продаж по товарам и промокодам")
public class PromoStatController {

    private final PromoStatService promoStatService;
    private final MonthlyPromoReportService monthlyPromoReportService;

    @PatchMapping("/daily")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Upsert дневной статистики", description = "Создает или обновляет дневную строку статистики для связки seller/promo/product/date")
    public PromoStatDailyResponse upsert(@Valid @RequestBody UpsertPromoStatDailyRequest request) {
        return promoStatService.upsert(request);
    }

    @GetMapping("/daily")
    @Operation(summary = "Дневная статистика", description = "Возвращает статистику по дням с фильтрами и диапазоном дат")
    public List<PromoStatDailyAggregateResponse> getDaily(
            @Parameter(description = "Идентификатор продавца (для OWNER/ADMIN)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Фильтр по промокоду") @RequestParam(required = false) UUID promoCodeId,
            @Parameter(description = "Фильтр по товару") @RequestParam(required = false) UUID productId,
            @Parameter(description = "Начало периода (YYYY-MM-DD)") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "Конец периода (YYYY-MM-DD)") @RequestParam(required = false) LocalDate to
    ) {
        return promoStatService.getDaily(sellerId, promoCodeId, productId, from, to);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Дашборд продавца", description = "Возвращает сводную статистику по продавцу за месяц")
    public PromoStatsDashboardResponse getDashboard(
            @Parameter(description = "Идентификатор продавца (для OWNER/ADMIN)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Месяц в формате YYYY-MM") @RequestParam(required = false) String month,
            @Parameter(description = "Количество элементов в топе") @RequestParam(defaultValue = "10") Integer topLimit
    ) {
        return promoStatService.getDashboard(sellerId, parseYearMonth(month), topLimit);
    }

    @GetMapping("/product-dashboard")
    @Operation(summary = "Дашборд товара", description = "Возвращает статистику конкретного товара за месяц и топ промокодов")
    public ProductStatsDashboardResponse getProductDashboard(
            @Parameter(description = "Идентификатор продавца (для OWNER/ADMIN)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Идентификатор товара") @RequestParam UUID productId,
            @Parameter(description = "Месяц в формате YYYY-MM") @RequestParam(required = false) String month,
            @Parameter(description = "Количество элементов в топе") @RequestParam(defaultValue = "10") Integer topLimit
    ) {
        return promoStatService.getProductDashboard(sellerId, productId, parseYearMonth(month), topLimit);
    }

    @GetMapping(value = "/monthly-report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "PDF-отчет за месяц по промокодам", description = "Генерирует PDF-отчет продавца за выбранный месяц: реквизиты, таблица по промокодам (кол-во и доход), итоговые суммы.")
    public ResponseEntity<byte[]> getMonthlyPromoReportPdf(
            @Parameter(description = "Идентификатор продавца (для OWNER/ADMIN)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Месяц в формате YYYY-MM") @RequestParam(required = false) String month,
            @Parameter(description = "Открывать в браузере (true) или скачивать файлом (false)") @RequestParam(defaultValue = "false") boolean inline
    ) {
        YearMonth targetMonth = parseYearMonth(month);
        byte[] content = monthlyPromoReportService.generateMonthlyPromoReport(sellerId, targetMonth);
        String filename = "promo-report-" + targetMonth + ".pdf";
        String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + filename + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(content);
    }

    private YearMonth parseYearMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException exception) {
            throw new backend.website.clearflow.model.error.BadRequestException("month must be in yyyy-MM format");
        }
    }
}
