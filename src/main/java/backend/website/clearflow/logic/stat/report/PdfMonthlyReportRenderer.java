package backend.website.clearflow.logic.stat.report;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class PdfMonthlyReportRenderer {

    private static final Locale RU_LOCALE = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", RU_LOCALE);
    private static final String CLASSPATH_FONT = "/fonts/DejaVuSans.ttf";
    private static final List<String> FONT_PATHS = List.of(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "C:/Windows/Fonts/arial.ttf"
    );

    public byte[] render(MonthlyPromoReport report) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, output);
            document.open();

            Font regular = new Font(loadBaseFont(), 10);
            Font bold = new Font(loadBaseFont(), 10, Font.BOLD);
            Font title = new Font(loadBaseFont(), 14, Font.BOLD);

            document.add(new Paragraph("Отчет по промокодам за " + capitalizeMonth(report.month().format(MONTH_FORMAT)), title));
            document.add(new Paragraph(" ", regular));
            document.add(new Paragraph("Продавец: " + firstNotBlank(report.header().companyName(), report.header().fullName(), "Не указано"), bold));
            document.add(new Paragraph("Email: " + firstNotBlank(report.header().sellerEmail(), "Не указано"), regular));
            document.add(new Paragraph("ИНН: " + firstNotBlank(report.header().inn(), "Не указано"), regular));
            document.add(new Paragraph("Банк: " + firstNotBlank(report.header().bankName(), "Не указано"), regular));
            document.add(new Paragraph("БИК: " + firstNotBlank(report.header().bik(), "Не указано"), regular));
            document.add(new Paragraph("Расчетный счет: " + firstNotBlank(report.header().settlementAccount(), "Не указано"), regular));
            document.add(new Paragraph("Корпоративный счет: " + firstNotBlank(report.header().corporateAccount(), "Не указано"), regular));
            document.add(new Paragraph("Адрес: " + firstNotBlank(report.header().address(), "Не указано"), regular));
            document.add(new Paragraph(" ", regular));

            PdfPTable table = new PdfPTable(new float[]{1.2f, 5.8f, 2.2f, 2.8f});
            table.setWidthPercentage(100);
            table.addCell(headerCell("№", bold));
            table.addCell(headerCell("Промокод", bold));
            table.addCell(headerCell("Кол-во", bold));
            table.addCell(headerCell("Доход, ₽", bold));

            int index = 1;
            for (MonthlyPromoReportRow row : report.rows()) {
                table.addCell(bodyCell(String.valueOf(index++), regular, Rectangle.ALIGN_CENTER));
                table.addCell(bodyCell(firstNotBlank(row.promoCodeName(), "Без названия"), regular, Rectangle.ALIGN_LEFT));
                table.addCell(bodyCell(String.valueOf(row.itemsSold()), regular, Rectangle.ALIGN_RIGHT));
                table.addCell(bodyCell(formatMoney(row.revenue()), regular, Rectangle.ALIGN_RIGHT));
            }

            if (report.rows().isEmpty()) {
                PdfPCell cell = new PdfPCell(new Phrase("За выбранный период данных нет", regular));
                cell.setColspan(4);
                cell.setHorizontalAlignment(Rectangle.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }

            document.add(table);
            document.add(new Paragraph(" ", regular));
            document.add(new Paragraph("Итого продаж: " + report.totalItemsSold(), bold));
            document.add(new Paragraph("Итого доход: " + formatMoney(report.totalRevenue()) + " ₽", bold));
            document.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Failed to render PDF report", exception);
        }
    }

    private PdfPCell headerCell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Rectangle.ALIGN_CENTER);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell bodyCell(String value, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        return cell;
    }

    private BaseFont loadBaseFont() throws IOException, DocumentException {
        BaseFont classpathFont = tryLoadClasspathFont();
        if (classpathFont != null) {
            return classpathFont;
        }
        for (String path : FONT_PATHS) {
            try {
                if (!Files.exists(Path.of(path))) {
                    continue;
                }
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
                // try next path
            }
        }
        throw new IOException("No UTF-8 compatible font found for PDF generation");
    }

    private BaseFont tryLoadClasspathFont() {
        try (InputStream stream = PdfMonthlyReportRenderer.class.getResourceAsStream(CLASSPATH_FONT)) {
            if (stream == null) {
                return null;
            }
            byte[] bytes = stream.readAllBytes();
            return BaseFont.createFont("DejaVuSans", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("ru-RU"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value);
    }

    private String firstNotBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private String firstNotBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback;
    }

    private String capitalizeMonth(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(RU_LOCALE) + value.substring(1);
    }
}
