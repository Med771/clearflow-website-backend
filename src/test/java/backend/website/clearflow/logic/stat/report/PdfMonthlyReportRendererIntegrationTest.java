package backend.website.clearflow.logic.stat.report;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfMonthlyReportRendererIntegrationTest {

    private static final Path OUTPUT_DIR = Path.of("target", "manual-check");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("monthly-promo-report-preview.pdf");

    @Test
    void rendersPdfAndSavesLocalPreviewFile() throws Exception {
        PdfMonthlyReportRenderer renderer = new PdfMonthlyReportRenderer();

        MonthlyPromoReport report = new MonthlyPromoReport(
                YearMonth.of(2026, 2),
                "359",
                LocalDate.of(2025, 12, 9),
                new MonthlyPromoReportPartyDetails(
                        "ООО Ромашка",
                        "7701234567",
                        "Сбербанк",
                        "044525225",
                        "40702810900000000001",
                        "30101810400000000225",
                        "г. Москва, ул. Пример, д. 1"
                ),
                new MonthlyPromoReportPartyDetails(
                        "ИП Гаврилов Станислав Александрович",
                        "7707654321",
                        "Т-Банк",
                        "044525974",
                        "40802810800000000010",
                        "30101810145250000974",
                        "г. Москва, ул. Покупателя, д. 5"
                ),
                List.of(
                        new MonthlyPromoReportRow(UUID.randomUUID(), "PROMO-ALPHA", "Кроссовки", 12, new BigDecimal("12345.67")),
                        new MonthlyPromoReportRow(UUID.randomUUID(), "PROMO-BETA", "Футболка", 5, new BigDecimal("2500.00"))
                ),
                17L,
                new BigDecimal("14845.67")
        );

        byte[] pdf = renderer.render(report);
        Files.createDirectories(OUTPUT_DIR);
        Files.write(OUTPUT_FILE, pdf);

        String pdfSignature = new String(pdf, 0, Math.min(pdf.length, 8), StandardCharsets.US_ASCII);
        assertTrue(pdf.length > 500, "Rendered PDF should not be too small");
        assertTrue(pdfSignature.startsWith("%PDF-"), "Rendered bytes should contain PDF header signature");
        assertTrue(Files.exists(OUTPUT_FILE), "Preview file should exist on disk");
        assertEquals(pdf.length, Files.size(OUTPUT_FILE), "Saved preview file size should match rendered bytes");
    }
}
