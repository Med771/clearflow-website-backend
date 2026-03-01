package backend.website.clearflow.logic.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import backend.website.clearflow.logic.report.dto.MonthlyReportListItem;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Tag(name = "Отчёты", description = "Операции со статистикой продаж по товарам и промокодам")
public class ReportController {
    private final MonthlyPromoReportService monthlyPromoReportService;

    @GetMapping(value = "/monthly-report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "PDF-отчет за месяц по промокодам", description = "Генерирует PDF-отчет продавца за выбранный месяц: реквизиты, таблица по промокодам (кол-во и доход), итоговые суммы.")
    public ResponseEntity<byte[]> getMonthlyPromoReportPdf(
            @Parameter(description = "Идентификатор продавца (для OWNER/ADMIN)") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Месяц в формате YYYY-MM") @RequestParam(required = false) String month,
            @Parameter(description = "Дата счета в формате YYYY-MM-DD (если не передана, используется текущая дата)") @RequestParam(required = false) LocalDate invoiceDate,
            @Parameter(description = "Открывать в браузере (true) или скачивать файлом (false)") @RequestParam(defaultValue = "false") boolean inline
    ) {
        YearMonth targetMonth = parseYearMonth(month);
        byte[] content = monthlyPromoReportService.generateMonthlyPromoReport(
                sellerId,
                targetMonth,
                invoiceDate
        );
        String filename = "promo-report-" + targetMonth + ".pdf";
        String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + filename + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(content);
    }

    @GetMapping
    @Operation(summary = "Список отчётов", description = "Для SELLER — свои отчёты. Для ADMIN/OWNER — все отчёты.")
    public List<MonthlyReportListItem> listReports() {
        return monthlyPromoReportService.listReports();
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
