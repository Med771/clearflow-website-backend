package backend.website.clearflow.logic.report;

import backend.website.clearflow.logic.report.dto.MonthlyPromoReport;
import backend.website.clearflow.logic.report.dto.MonthlyPromoReportPartyDetails;
import backend.website.clearflow.logic.report.dto.MonthlyPromoReportRow;
import org.openpdf.text.*;
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

    private static final Color GRAY_BORDER = new Color(200, 200, 200);
    private static final Color HEADER_BG = new Color(245, 245, 245);
    private static final Color GRAY_TEXT = new Color(128, 128, 128);
    private static final Color LIGHT_GRAY_BG = new Color(250, 250, 250);

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

            Font regular12 = new Font(baseFont, 12);
            Font regular10 = new Font(baseFont, 10);
            Font regular9 = new Font(baseFont, 9);
            Font bold10 = new Font(baseFont, 10, Font.BOLD);
            Font bold11 = new Font(baseFont, 11, Font.BOLD);
            Font bold14 = new Font(baseFont, 14, Font.BOLD);
            Font bold16 = new Font(baseFont, 16, Font.BOLD);
            Font gray10 = new Font(baseFont, 10, Font.NORMAL, GRAY_TEXT);

            // 1. Верхний колонтитул
            addTopHeader(document, regular12, gray10);
            document.add(new Paragraph(" "));

            // 2. Компактная таблица с реквизитами банка
            addRecipientCompactBankTable(document, report.recipient(), regular9, bold10);
            document.add(new Paragraph(" "));

            // 3. Заголовок счёта
            Paragraph invoiceTitle = new Paragraph(
                    "Счёт №" + firstNotBlank(report.invoiceNumber(), "без номера")
                            + " от " + report.invoiceDate().format(INVOICE_DATE_FORMAT),
                    bold14
            );
            invoiceTitle.setAlignment(Element.ALIGN_CENTER);
            invoiceTitle.setSpacingBefore(8);
            invoiceTitle.setSpacingAfter(12);
            document.add(invoiceTitle);

            // 4. Полные реквизиты получателя
            Paragraph recipientDetails = new Paragraph(
                    buildPartyLine("Получатель", report.recipient()),
                    regular9
            );
            recipientDetails.setSpacingAfter(8);
            document.add(recipientDetails);

            // 5. Полные реквизиты плательщика
            Paragraph payerDetails = new Paragraph(
                    buildPartyLine("Плательщик", report.payer()),
                    regular9
            );
            payerDetails.setSpacingAfter(15);
            document.add(payerDetails);

            // 6. Таблица товаров с промокодами
            addItemsTable(document, report, regular9, bold10);
            document.add(new Paragraph(" "));

            // 7. Сумма прописью
            addAmountInWords(document, report, regular10);
            document.add(new Paragraph(" "));

            // 8. Итог к оплате
            document.add(new Paragraph("Итог к оплате:", bold11));
            document.add(new Paragraph(" "));

            Paragraph totalAmount = new Paragraph(
                    formatMoney(report.totalRevenue()) + " ₽",
                    bold16
            );
            totalAmount.setSpacingAfter(5);
            document.add(totalAmount);

            Paragraph noVat = new Paragraph("Без НДС", gray10);
            noVat.setSpacingAfter(20);
            document.add(noVat);

            // 9. Подписи сторон
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

        PdfPCell leftCell = new PdfPCell(new Phrase("Получатель", regular));
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        leftCell.setPaddingLeft(0);

        PdfPCell rightCell = new PdfPCell(new Phrase("Без НДС", gray));
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setPaddingRight(0);

        top.addCell(leftCell);
        top.addCell(rightCell);
        document.add(top);
    }

    private void addRecipientCompactBankTable(Document document,
                                              MonthlyPromoReportPartyDetails recipient,
                                              Font regular,
                                              Font bold) throws DocumentException {

        String bankName = partyValue(recipient, MonthlyPromoReportPartyDetails::bankName);
        String bik = partyValue(recipient, MonthlyPromoReportPartyDetails::bik);
        String corrAccount = partyValue(recipient, MonthlyPromoReportPartyDetails::corporateAccount);
        String inn = partyValue(recipient, MonthlyPromoReportPartyDetails::inn);
        String settlementAccount = partyValue(recipient, MonthlyPromoReportPartyDetails::settlementAccount);
        String name = partyValue(recipient, MonthlyPromoReportPartyDetails::name);

        PdfPTable bankTable = new PdfPTable(new float[]{1f, 1f});
        bankTable.setWidthPercentage(100);

        bankTable.addCell(createCompactCell(bankName, regular, false));
        bankTable.addCell(createCompactCell("БИК " + bik, regular, false));

        bankTable.addCell(createCompactCell("Банк получателя", regular, false));
        bankTable.addCell(createCompactCell("Кор. Счёт " + corrAccount, regular, false));

        bankTable.addCell(createCompactCell("", regular, false));
        bankTable.addCell(createCompactCell("ИНН " + inn, regular, false));

        bankTable.addCell(createCompactCell("", regular, false));
        bankTable.addCell(createCompactCell("Счёт " + settlementAccount, regular, false));

        bankTable.addCell(createCompactCell(name, bold, true));
        bankTable.addCell(createCompactCell("Получатель", bold, true));

        document.add(bankTable);
    }

    private PdfPCell createCompactCell(String value, Font font, boolean highlighted) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(GRAY_BORDER);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (highlighted) {
            cell.setBackgroundColor(LIGHT_GRAY_BG);
        }
        return cell;
    }

    private void addItemsTable(Document document,
                               MonthlyPromoReport report,
                               Font regular,
                               Font bold) throws DocumentException {

        // Колонки: №, Название, Промокод, Кол-во, Ед. изм., Цена, НДС, Сумма
        PdfPTable table = new PdfPTable(new float[]{0.5f, 2.5f, 1.5f, 0.8f, 0.8f, 1.2f, 1.2f, 1.5f});
        table.setWidthPercentage(100);

        // Заголовки таблицы
        table.addCell(createHeaderCell("№", bold, Element.ALIGN_CENTER));
        table.addCell(createHeaderCell("Наименование товара или услуги", bold, Element.ALIGN_LEFT));
        table.addCell(createHeaderCell("Промокод", bold, Element.ALIGN_CENTER));
        table.addCell(createHeaderCell("Кол-во", bold, Element.ALIGN_CENTER));
        table.addCell(createHeaderCell("Ед. изм.", bold, Element.ALIGN_CENTER));
        table.addCell(createHeaderCell("Цена", bold, Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("НДС", bold, Element.ALIGN_LEFT));
        table.addCell(createHeaderCell("Сумма", bold, Element.ALIGN_RIGHT));

        int index = 1;
        for (MonthlyPromoReportRow row : report.rows()) {
            BigDecimal quantity = BigDecimal.valueOf(row.itemsSold());
            BigDecimal price = row.itemsSold() == 0
                    ? BigDecimal.ZERO
                    : row.revenue().divide(quantity, 2, RoundingMode.HALF_UP);

            table.addCell(createBodyCell(String.valueOf(index++), regular, Element.ALIGN_CENTER));
            table.addCell(createBodyCell(firstNotBlank(row.productName(), "Без названия"), regular, Element.ALIGN_LEFT));
            table.addCell(createBodyCell(formatPromoCode(row.promoCodeName()), regular, Element.ALIGN_CENTER));
            table.addCell(createBodyCell(String.valueOf(row.itemsSold()), regular, Element.ALIGN_CENTER));
            table.addCell(createBodyCell("шт", regular, Element.ALIGN_CENTER));
            table.addCell(createBodyCell(formatMoney(price) + " ₽", regular, Element.ALIGN_RIGHT));
            table.addCell(createBodyCell("Без НДС", regular, Element.ALIGN_LEFT));
            table.addCell(createBodyCell(formatMoney(row.revenue()) + " ₽", regular, Element.ALIGN_RIGHT));
        }

        if (report.rows().isEmpty()) {
            PdfPCell emptyCell = createBodyCell("Нет данных за выбранный период", regular, Element.ALIGN_CENTER);
            emptyCell.setColspan(8);
            table.addCell(emptyCell);
        }

        document.add(table);
    }

    private String formatPromoCode(String promoCode) {
        if (promoCode == null || promoCode.isBlank()) {
            return "-";
        }
        return promoCode;
    }

    private void addAmountInWords(Document document, MonthlyPromoReport report, Font font) throws DocumentException {
        int itemsCount = report.rows().size();
        String countWord = pluralize(itemsCount, "наименование", "наименования", "наименований");
        String amountWords = amountInWords(report.totalRevenue());

        Paragraph amountInWordsPara = new Paragraph(
                String.format("Всего %d %s на сумму %s.", itemsCount, countWord, amountWords),
                font
        );
        amountInWordsPara.setSpacingBefore(10);
        amountInWordsPara.setSpacingAfter(5);
        document.add(amountInWordsPara);
    }

    private void addSignatures(Document document,
                               MonthlyPromoReport report,
                               Font regular,
                               Font bold) throws DocumentException {

        PdfPTable signaturesTable = new PdfPTable(new float[]{1f, 1f});
        signaturesTable.setWidthPercentage(100);
        signaturesTable.setSpacingBefore(30);

        PdfPCell recipientLabelCell = new PdfPCell(new Phrase("Получатель:", bold));
        recipientLabelCell.setBorder(Rectangle.NO_BORDER);
        recipientLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell payerLabelCell = new PdfPCell(new Phrase("Плательщик:", bold));
        payerLabelCell.setBorder(Rectangle.NO_BORDER);
        payerLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        signaturesTable.addCell(recipientLabelCell);
        signaturesTable.addCell(payerLabelCell);

        PdfPCell recipientSignatureCell = new PdfPCell(new Phrase("____________________", regular));
        recipientSignatureCell.setBorder(Rectangle.NO_BORDER);
        recipientSignatureCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell payerSignatureCell = new PdfPCell(new Phrase("____________________", regular));
        payerSignatureCell.setBorder(Rectangle.NO_BORDER);
        payerSignatureCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        signaturesTable.addCell(recipientSignatureCell);
        signaturesTable.addCell(payerSignatureCell);

        String recipientName = partyValue(report.recipient(), MonthlyPromoReportPartyDetails::name);
        String payerName = partyValue(report.payer(), MonthlyPromoReportPartyDetails::name).toUpperCase(RU_LOCALE);

        String[] recipientLines = splitNameIntoLines(recipientName, 35);
        String[] payerLines = splitNameIntoLines(payerName, 35);

        PdfPCell recipientNameCell = new PdfPCell();
        recipientNameCell.setBorder(Rectangle.NO_BORDER);
        recipientNameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (String line : recipientLines) {
            recipientNameCell.addElement(new Paragraph(line, regular));
        }

        PdfPCell payerNameCell = new PdfPCell();
        payerNameCell.setBorder(Rectangle.NO_BORDER);
        payerNameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (String line : payerLines) {
            payerNameCell.addElement(new Paragraph(line, regular));
        }

        signaturesTable.addCell(recipientNameCell);
        signaturesTable.addCell(payerNameCell);

        document.add(signaturesTable);
    }

    private String[] splitNameIntoLines(String name, int maxLength) {
        if (name.length() <= maxLength) {
            return new String[]{name};
        }

        int spaceIndex = name.lastIndexOf(' ', maxLength);
        if (spaceIndex > 0) {
            String firstLine = name.substring(0, spaceIndex);
            String secondLine = name.substring(spaceIndex + 1);
            return new String[]{firstLine, secondLine};
        }

        return new String[]{name};
    }

    private String buildPartyLine(String label, MonthlyPromoReportPartyDetails party) {
        return label + ": "
                + partyValue(party, MonthlyPromoReportPartyDetails::name) + ", "
                + "ИНН " + partyValue(party, MonthlyPromoReportPartyDetails::inn) + ", "
                + partyValue(party, MonthlyPromoReportPartyDetails::address) + ", "
                + "р/с " + partyValue(party, MonthlyPromoReportPartyDetails::settlementAccount) + ", "
                + "в банке " + partyValue(party, MonthlyPromoReportPartyDetails::bankName) + ", "
                + "БИК " + partyValue(party, MonthlyPromoReportPartyDetails::bik) + ", "
                + "к/с " + partyValue(party, MonthlyPromoReportPartyDetails::corporateAccount);
    }

    private PdfPCell createHeaderCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(GRAY_BORDER);
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createBodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(GRAY_BORDER);
        cell.setPadding(5);
        return cell;
    }

    private BaseFont loadBaseFont() throws IOException, DocumentException {
        BaseFont classpathFont = tryLoadClasspathFont();
        if (classpathFont != null) {
            return classpathFont;
        }

        for (String path : FONT_PATHS) {
            try {
                if (Files.exists(Path.of(path))) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {}
        }

        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
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
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(RU_LOCALE);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');

        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value);
    }

    private String firstNotBlank(String first, String fallback) {
        return (first != null && !first.isBlank()) ? first : fallback;
    }

    private String partyValue(MonthlyPromoReportPartyDetails party,
                              Function<MonthlyPromoReportPartyDetails, String> extractor) {
        if (party == null) {
            return "Не указано";
        }
        String value = extractor.apply(party);
        return (value != null && !value.isBlank()) ? value : "Не указано";
    }

    private String amountInWords(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        long rubles = normalized.longValue();
        int kopeks = normalized.remainder(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        return numberToWordsRu(rubles) + " "
                + pluralize(rubles, "рубль", "рубля", "рублей") + " "
                + String.format("%02d", kopeks) + " "
                + pluralize(kopeks, "копейка", "копейки", "копеек");
    }

    private String numberToWordsRu(long number) {
        if (number == 0) {
            return "ноль";
        }

        String[] unitsMale = {"", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
        String[] unitsFemale = {"", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
        String[] teens = {"десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать",
                "пятнадцать", "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"};
        String[] tens = {"", "", "двадцать", "тридцать", "сорок", "пятьдесят",
                "шестьдесят", "семьдесят", "восемьдесят", "девяносто"};
        String[] hundreds = {"", "сто", "двести", "триста", "четыреста", "пятьсот",
                "шестьсот", "семьсот", "восемьсот", "девятьсот"};

        String[][] groupForms = {
                {"миллиард", "миллиарда", "миллиардов"},
                {"миллион", "миллиона", "миллионов"},
                {"тысяча", "тысячи", "тысяч"}
        };

        long[] groups = {
                number / 1_000_000_000,
                (number / 1_000_000) % 1_000,
                (number / 1_000) % 1_000,
                number % 1_000
        };

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < groups.length; i++) {
            long group = groups[i];
            if (group == 0) {
                continue;
            }

            int groupIndex = groups.length - 1 - i;
            String[] units = (groupIndex == 2) ? unitsFemale : unitsMale;

            int hundredsDigit = (int) (group / 100);
            if (hundredsDigit > 0) {
                result.append(hundreds[hundredsDigit]).append(" ");
            }

            int lastTwo = (int) (group % 100);
            if (lastTwo >= 10 && lastTwo <= 19) {
                result.append(teens[lastTwo - 10]).append(" ");
            } else {
                int tensDigit = lastTwo / 10;
                if (tensDigit > 1) {
                    result.append(tens[tensDigit]).append(" ");
                }

                int unitsDigit = lastTwo % 10;
                if (unitsDigit > 0) {
                    result.append(units[unitsDigit]).append(" ");
                }
            }

            if (groupIndex < 3) {
                result.append(pluralize(group,
                                groupForms[groupIndex][0],
                                groupForms[groupIndex][1],
                                groupForms[groupIndex][2]))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private String pluralize(long number, String form1, String form2, String form5) {
        long n = Math.abs(number) % 100;
        if (n >= 11 && n <= 19) {
            return form5;
        }

        n = n % 10;
        if (n == 1) {
            return form1;
        }
        if (n >= 2 && n <= 4) {
            return form2;
        }
        return form5;
    }
}