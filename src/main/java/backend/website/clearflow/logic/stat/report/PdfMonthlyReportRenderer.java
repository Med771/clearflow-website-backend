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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Component
public class PdfMonthlyReportRenderer {

    private static final Locale RU_LOCALE = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter INVOICE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String CLASSPATH_FONT = "/fonts/DejaVuSans.ttf";
    private static final Color GRAY_BORDER = new Color(180, 180, 180);
    private static final Color HEADER_BG = new Color(240, 240, 240);
    private static final Color GRAY_TEXT = new Color(90, 90, 90);
    private static final List<String> FONT_PATHS = List.of(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "C:/Windows/Fonts/arial.ttf"
    );

    public byte[] render(MonthlyPromoReport report) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 40, 40);
            PdfWriter.getInstance(document, output);
            document.open();

            BaseFont baseFont = loadBaseFont();
            Font regular10 = new Font(baseFont, 10);
            Font regular9 = new Font(baseFont, 9);
            Font bold10 = new Font(baseFont, 10, Font.BOLD);
            Font bold11 = new Font(baseFont, 11, Font.BOLD);
            Font title16 = new Font(baseFont, 16, Font.BOLD);
            Font total18 = new Font(baseFont, 18, Font.BOLD);
            Font gray10 = new Font(baseFont, 10, Font.NORMAL, GRAY_TEXT);

            addTopHeader(document, regular10, gray10);
            document.add(new Paragraph(" "));
            addRecipientCompactBankTable(document, report.recipient(), regular10, bold10);
            document.add(new Paragraph(" "));

            Paragraph invoiceTitle = new Paragraph(
                    "## Счёт №" + firstNotBlank(report.invoiceNumber(), "без номера")
                            + " от " + report.invoiceDate().format(INVOICE_DATE_FORMAT),
                    title16
            );
            invoiceTitle.setAlignment(Rectangle.ALIGN_CENTER);
            invoiceTitle.setSpacingBefore(8);
            invoiceTitle.setSpacingAfter(10);
            document.add(invoiceTitle);

            document.add(new Paragraph(buildPartyLine("Получатель", report.recipient()), regular9));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(buildPartyLine("Плательщик", report.payer()), regular9));
            document.add(new Paragraph(" "));

            addItemsTable(document, report, regular9, bold10);
            document.add(new Paragraph(" "));

            String countWords = pluralize(report.rows().size(), "наименование", "наименования", "наименований");
            document.add(new Paragraph(
                    "Всего " + report.rows().size() + " " + countWords + " на сумму " + amountInWords(report.totalRevenue()) + ".",
                    regular10
            ));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Итог к оплате:", bold11));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(formatMoney(report.totalRevenue()) + " ₽", total18));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Без НДС", gray10));
            document.add(new Paragraph(" "));

            addSignatures(document, report, regular10, bold10);
            document.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Failed to render PDF report", exception);
        }
    }

    private void addTopHeader(Document document, Font regular, Font gray) throws DocumentException {
        PdfPTable top = new PdfPTable(new float[]{1f, 1f});
        top.setWidthPercentage(100);
        PdfPCell left = noBorderCell("Получатель", regular, Rectangle.ALIGN_LEFT);
        PdfPCell right = noBorderCell("Без НДС", gray, Rectangle.ALIGN_RIGHT);
        left.setPadding(0);
        right.setPadding(0);
        top.addCell(left);
        top.addCell(right);
        document.add(top);
    }

    private void addRecipientCompactBankTable(Document document, MonthlyPromoReportPartyDetails recipient, Font regular, Font bold) throws DocumentException {
        String bank = partyValue(recipient, MonthlyPromoReportPartyDetails::bankName);
        String bik = partyValue(recipient, MonthlyPromoReportPartyDetails::bik);
        String corr = partyValue(recipient, MonthlyPromoReportPartyDetails::corporateAccount);
        String inn = partyValue(recipient, MonthlyPromoReportPartyDetails::inn);
        String account = partyValue(recipient, MonthlyPromoReportPartyDetails::settlementAccount);
        String name = partyValue(recipient, MonthlyPromoReportPartyDetails::name);

        PdfPTable bankTable = new PdfPTable(new float[]{1f, 1f});
        bankTable.setWidthPercentage(100);
        bankTable.addCell(compactCell(bank, regular, false));
        bankTable.addCell(compactCell("БИК " + bik, regular, false));
        bankTable.addCell(compactCell("Банк получателя", regular, false));
        bankTable.addCell(compactCell("Кор. Счёт " + corr, regular, false));
        bankTable.addCell(compactCell("", regular, false));
        bankTable.addCell(compactCell("ИНН " + inn, regular, false));
        bankTable.addCell(compactCell("", regular, false));
        bankTable.addCell(compactCell("Счёт " + account, regular, false));
        bankTable.addCell(compactCell(name, bold, true));
        bankTable.addCell(compactCell("Получатель", bold, true));
        document.add(bankTable);
    }

    private void addItemsTable(Document document, MonthlyPromoReport report, Font regular, Font bold) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.6f, 3.6f, 1.5f, 0.9f, 1f, 1.3f, 1.2f, 1.4f});
        table.setWidthPercentage(100);
        table.addCell(headerCell("№", bold, Rectangle.ALIGN_CENTER));
        table.addCell(headerCell("Название товара или услуги", bold, Rectangle.ALIGN_LEFT));
        table.addCell(headerCell("Промокод", bold, Rectangle.ALIGN_CENTER));
        table.addCell(headerCell("Кол-во", bold, Rectangle.ALIGN_CENTER));
        table.addCell(headerCell("Ед. изм.", bold, Rectangle.ALIGN_CENTER));
        table.addCell(headerCell("Цена", bold, Rectangle.ALIGN_RIGHT));
        table.addCell(headerCell("НДС", bold, Rectangle.ALIGN_LEFT));
        table.addCell(headerCell("Сумма", bold, Rectangle.ALIGN_RIGHT));

        int index = 1;
        for (MonthlyPromoReportRow row : report.rows()) {
            BigDecimal quantity = BigDecimal.valueOf(row.itemsSold());
            BigDecimal price = row.itemsSold() == 0
                    ? BigDecimal.ZERO
                    : row.revenue().divide(quantity, 2, RoundingMode.HALF_UP);
            table.addCell(bodyCell(String.valueOf(index++), regular, Rectangle.ALIGN_CENTER));
            table.addCell(bodyCell(firstNotBlank(row.productName(), "Без названия"), regular, Rectangle.ALIGN_LEFT));
            table.addCell(bodyCell(firstNotBlank(row.promoCodeName(), "-"), regular, Rectangle.ALIGN_CENTER));
            table.addCell(bodyCell(String.valueOf(row.itemsSold()), regular, Rectangle.ALIGN_CENTER));
            table.addCell(bodyCell("шт.", regular, Rectangle.ALIGN_CENTER));
            table.addCell(bodyCell(formatMoney(price), regular, Rectangle.ALIGN_RIGHT));
            table.addCell(bodyCell("Без НДС", regular, Rectangle.ALIGN_LEFT));
            table.addCell(bodyCell(formatMoney(row.revenue()), regular, Rectangle.ALIGN_RIGHT));
        }

        if (report.rows().isEmpty()) {
            PdfPCell cell = bodyCell("За выбранный период данных нет", regular, Rectangle.ALIGN_CENTER);
            cell.setColspan(8);
            table.addCell(cell);
        }
        document.add(table);
    }

    private void addSignatures(Document document, MonthlyPromoReport report, Font regular, Font bold) throws DocumentException {
        PdfPTable signatures = new PdfPTable(new float[]{1f, 1f});
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(8);

        signatures.addCell(noBorderCell("Получатель:", bold, Rectangle.ALIGN_LEFT));
        signatures.addCell(noBorderCell("Плательщик:", bold, Rectangle.ALIGN_LEFT));
        signatures.addCell(noBorderCell("____________________", regular, Rectangle.ALIGN_LEFT));
        signatures.addCell(noBorderCell("____________________", regular, Rectangle.ALIGN_LEFT));
        signatures.addCell(noBorderCell(firstNotBlank(partyValue(report.recipient(), MonthlyPromoReportPartyDetails::name), "Не указано"), regular, Rectangle.ALIGN_LEFT));
        signatures.addCell(noBorderCell(firstNotBlank(partyValue(report.payer(), MonthlyPromoReportPartyDetails::name), "Не указано").toUpperCase(RU_LOCALE), regular, Rectangle.ALIGN_LEFT));
        document.add(signatures);
    }

    private String buildPartyLine(String label, MonthlyPromoReportPartyDetails party) {
        return label + ": "
                + partyValue(party, MonthlyPromoReportPartyDetails::name)
                + ", ИНН " + partyValue(party, MonthlyPromoReportPartyDetails::inn)
                + ", " + partyValue(party, MonthlyPromoReportPartyDetails::address)
                + ", р/с " + partyValue(party, MonthlyPromoReportPartyDetails::settlementAccount)
                + ", в банке " + partyValue(party, MonthlyPromoReportPartyDetails::bankName)
                + ", БИК " + partyValue(party, MonthlyPromoReportPartyDetails::bik)
                + ", к/с " + partyValue(party, MonthlyPromoReportPartyDetails::corporateAccount);
    }

    private PdfPCell compactCell(String value, Font font, boolean highlightedRow) {
        PdfPCell cell = new PdfPCell(new Phrase(firstNotBlank(value, " "), font));
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(GRAY_BORDER);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
        cell.setHorizontalAlignment(Rectangle.ALIGN_LEFT);
        if (highlightedRow) {
            cell.setBackgroundColor(new Color(250, 250, 250));
        }
        return cell;
    }

    private PdfPCell headerCell(String value, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Rectangle.ALIGN_MIDDLE);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(GRAY_BORDER);
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell bodyCell(String value, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Rectangle.ALIGN_MIDDLE);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(GRAY_BORDER);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell noBorderCell(String value, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.NO_BORDER);
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
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    private String firstNotBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback;
    }

    private String amountInWords(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        long rubles = normalized.longValue();
        int kopeks = normalized.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        return numberToWordsRu(rubles) + " " + pluralize(rubles, "рубль", "рубля", "рублей")
                + " " + String.format("%02d", kopeks) + " " + pluralize(kopeks, "копейка", "копейки", "копеек");
    }

    private String numberToWordsRu(long value) {
        if (value == 0) {
            return "ноль";
        }
        String[] unitsMale = {"", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
        String[] unitsFemale = {"", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
        String[] teens = {"десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать", "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"};
        String[] tens = {"", "", "двадцать", "тридцать", "сорок", "пятьдесят", "шестьдесят", "семьдесят", "восемьдесят", "девяносто"};
        String[] hundreds = {"", "сто", "двести", "триста", "четыреста", "пятьсот", "шестьсот", "семьсот", "восемьсот", "девятьсот"};
        String[][] groupForms = {
                {"миллиард", "миллиарда", "миллиардов"},
                {"миллион", "миллиона", "миллионов"},
                {"тысяча", "тысячи", "тысяч"},
                {"", "", ""}
        };

        StringBuilder result = new StringBuilder();
        int[] groups = {
                (int) (value / 1_000_000_000),
                (int) ((value / 1_000_000) % 1_000),
                (int) ((value / 1_000) % 1_000),
                (int) (value % 1_000)
        };
        for (int i = 0; i < groups.length; i++) {
            int group = groups[i];
            if (group == 0) {
                continue;
            }
            appendTriplet(result, group, hundreds, tens, teens, i == 2 ? unitsFemale : unitsMale);
            String form = pluralize(group, groupForms[i][0], groupForms[i][1], groupForms[i][2]);
            if (!form.isBlank()) {
                result.append(form).append(' ');
            }
        }
        return result.toString().trim().replaceAll("\\s{2,}", " ");
    }

    private void appendTriplet(StringBuilder builder, int n, String[] hundreds, String[] tens, String[] teens, String[] units) {
        int h = n / 100;
        int t = (n % 100) / 10;
        int u = n % 10;
        if (h > 0) {
            builder.append(hundreds[h]).append(' ');
        }
        if (t == 1) {
            builder.append(teens[u]).append(' ');
            return;
        }
        if (t > 1) {
            builder.append(tens[t]).append(' ');
        }
        if (u > 0) {
            builder.append(units[u]).append(' ');
        }
    }

    private String pluralize(long value, String one, String few, String many) {
        long mod100 = value % 100;
        if (mod100 >= 11 && mod100 <= 14) {
            return many;
        }
        long mod10 = value % 10;
        if (mod10 == 1) {
            return one;
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return few;
        }
        return many;
    }

    private String partyValue(MonthlyPromoReportPartyDetails party, Function<MonthlyPromoReportPartyDetails, String> extractor) {
        if (party == null) {
            return "Не указано";
        }
        return firstNotBlank(extractor.apply(party), "Не указано");
    }
}
