package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.AdminRepairView;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The client copy of a repair as an A5 portrait PDF - the same slip as the {@code ticketSlip}
 * fragment in tickets/ticketPrint.html, laid out by hand (PDFBox has no HTML layout).
 *
 * The slip's content is mirrored section for section; the only text decided elsewhere is the cost
 * and the status, which come from {@link AdminRepairView} so the PDF and the HTML slip follow the
 * same rules: {@code isCostAssessed} / {@code getCost} / {@code getCostSuffix} /
 * {@code getCostPlaceholder} for the cost and {@code getStatusLabel} for the status. Remarks are
 * shop-only and are never read here.
 *
 * Only the PDF standard fonts are used (Helvetica, WinAnsi), so nothing is embedded. A character
 * outside that encoding (a peso sign, Cyrillic, CJK, an emoji ...) is printed as "?" instead of
 * failing the request. Whitespace, including line breaks, is collapsed to single spaces the way the
 * HTML slip collapses it.
 */
public class ClientCopyPdfService {

    static final String SHOP_NAME = "RNPC Repair Shop";
    static final String SHOP_SUBTITLE = "PC and Cellphone Repair Shop";
    static final String CLAIM_NOTE = "Please present this slip when claiming your item.";
    static final String NOT_RECORDED = "Not recorded";

    private static final PDRectangle PAGE_SIZE = PDRectangle.A5;
    private static final float MARGIN = 36f;
    private static final float CONTENT_WIDTH = PAGE_SIZE.getWidth() - 2 * MARGIN;
    private static final float LEADING = 13f;
    private static final float BODY = 10f;
    private static final float BOX_PAD = 6f;
    private static final float KEY_GAP = 14f;

    private static final Color TEXT = new Color(0x1F, 0x23, 0x2B);
    private static final Color MUTED = new Color(0x6B, 0x72, 0x80);
    private static final Color BOX_FILL = new Color(0xF3, 0xF4, 0xF6);
    private static final Color RULE = new Color(0x9C, 0xA3, 0xAF);

    /** The PDF's file name, e.g. "JO-00015.pdf" - anything outside letters, digits, dot, dash and underscore becomes "_". */
    public static String fileName(RepairRecord repair) {
        String jobOrder = repair.getJobOrderNumber() == null ? "" : repair.getJobOrderNumber().trim();
        String safe = jobOrder.replaceAll("[^A-Za-z0-9._-]", "_");
        return (safe.isEmpty() ? "client-copy" : safe) + ".pdf";
    }

    /** The document title: "JO-00015 client copy". */
    public static String title(RepairRecord repair) {
        String jobOrder = repair.getJobOrderNumber() == null ? "" : repair.getJobOrderNumber().trim();
        return jobOrder.isEmpty() ? "Client copy" : jobOrder + " client copy";
    }

    public byte[] render(RepairRecord repair) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.getDocumentInformation().setTitle(title(repair));
            // Show the title, not the file name, in the browser tab.
            PDViewerPreferences preferences = new PDViewerPreferences(new COSDictionary());
            preferences.setDisplayDocTitle(true);
            document.getDocumentCatalog().setViewerPreferences(preferences);

            try (Slip slip = new Slip(document)) {
                slip.draw(repair);
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** One render's drawing state: the open page, the cursor, and the two fonts. */
    private static final class Slip implements AutoCloseable {
        private final PDDocument document;
        private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream stream;
        private float y;

        Slip(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        // ---- The slip, top to bottom ---------------------------------------------------------------

        void draw(RepairRecord repair) throws IOException {
            AdminRepairView view = AdminRepairView.from(repair);
            Client client = repair.getClient();

            // header
            ensure(46);
            centered(SHOP_NAME, bold, 14, TEXT);
            y -= 13;
            centered(SHOP_SUBTITLE, regular, 9, MUTED);
            y -= 9;
            rule(true);
            y -= 14;

            // job order and date
            ensure(24);
            String jobOrder = clean(repair.getJobOrderNumber());
            text(jobOrder, bold, 11, TEXT, MARGIN, y);
            String date = repair.getRepairDate() == null ? "" : clean(formatDate(repair.getRepairDate()));
            text(date, regular, 9, MUTED, MARGIN + CONTENT_WIDTH - width(date, regular, 9), y);
            y -= 22;

            // client
            label("Client");
            row("Name", client == null ? "" : client.getFullName(), false);
            row("Contact", client == null ? "" : client.getContactNumber(), false);
            y -= 8;

            // device
            label("Device");
            row("Item", repair.deviceSummary(" - "), false);
            if (isBlank(repair.getSerialNumber())) {
                row("Serial", NOT_RECORDED, true);
            } else {
                row("Serial", repair.getSerialNumber(), false);
            }
            y -= 8;

            label("Reported Issue");
            box(repair.getIssueDescription());
            y -= 18;

            if (!isBlank(repair.getFix())) {
                label("Fix Applied");
                box(repair.getFix());
                y -= 18;
            }
            if (!isBlank(repair.getRecommendation())) {
                label("Recommendation");
                box(repair.getRecommendation());
                y -= 18;
            }
            // Remarks are shop-only and deliberately not printed - see the class comment.

            ensure(12);
            rule(true);
            y -= 12;

            if (!isBlank(repair.getTechnician())) {
                row("Technician", repair.getTechnician(), false);
            }
            if (view.isCostAssessed()) {
                costRow(view);
            } else {
                row("Estimated cost", view.getCostPlaceholder(), false);
            }
            row("Status", view.getStatusLabel(), false);
            if (repair.getWarrantyEndDate() != null) {
                row("Warranty ends", formatDate(repair.getWarrantyEndDate()), false);
            }
            y -= 4;

            ensure(12);
            rule(true);
            y -= 14;

            ensure(15);
            text(CLAIM_NOTE, regular, 9, MUTED, MARGIN, y);
            y -= 14;

            signatures();
        }

        // ---- Building blocks -----------------------------------------------------------------------

        private void label(String text) throws IOException {
            ensure(24);
            text(text.toUpperCase(Locale.ROOT), bold, 8, MUTED, MARGIN, y);
            y -= 13;
        }

        /** "Key ........ value", the value right-aligned and wrapped if long; the key's baseline is the first line's. */
        private void row(String key, String value, boolean muted) throws IOException {
            float keyWidth = width(key, regular, BODY);
            float valueWidth = CONTENT_WIDTH - keyWidth - KEY_GAP;
            List<String> lines = wrap(clean(value), bold, BODY, valueWidth);
            if (lines.isEmpty()) {
                lines = List.of("");
            }
            ensure(lines.size() * LEADING + 3);
            text(key, regular, BODY, MUTED, MARGIN, y);
            float lineY = y;
            for (String line : lines) {
                text(line, bold, BODY, muted ? MUTED : TEXT, MARGIN + CONTENT_WIDTH - width(line, bold, BODY), lineY);
                lineY -= LEADING;
            }
            y = lineY - 3;
        }

        /** The Estimated cost row for an assessed cost: "Php 1,500.00", plus a muted "(diagnostic fee)" when cancelled. */
        private void costRow(AdminRepairView view) throws IOException {
            String amount = "Php " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
                    .format(view.getCost());
            String suffix = view.getCostSuffix().isEmpty() ? "" : " " + clean(view.getCostSuffix());
            ensure(LEADING + 3);
            text("Estimated cost", regular, BODY, MUTED, MARGIN, y);
            float total = width(amount, bold, BODY) + width(suffix, regular, BODY);
            float x = MARGIN + CONTENT_WIDTH - total;
            stream.beginText();
            stream.setFont(bold, BODY);
            stream.setNonStrokingColor(TEXT);
            stream.newLineAtOffset(x, y);
            stream.showText(amount);
            if (!suffix.isEmpty()) {
                stream.setFont(regular, BODY);
                stream.setNonStrokingColor(MUTED);
                stream.showText(suffix);
            }
            stream.endText();
            y -= LEADING + 3;
        }

        /** A shaded box with wrapped text; split across pages by line if it does not fit. */
        private void box(String value) throws IOException {
            List<String> lines = wrap(clean(value), regular, BODY, CONTENT_WIDTH - 2 * BOX_PAD);
            if (lines.isEmpty()) {
                lines = List.of("");
            }
            y += 4; // a row's baseline sits at y, a box's top edge does: pull the box up under its label
            int next = 0;
            while (next < lines.size()) {
                float room = y - MARGIN - 2 * BOX_PAD;
                int fit = (int) Math.floor(room / LEADING);
                if (fit < 1) {
                    newPage();
                    continue;
                }
                int count = Math.min(fit, lines.size() - next);
                float height = count * LEADING + 2 * BOX_PAD;
                stream.setNonStrokingColor(BOX_FILL);
                stream.addRect(MARGIN, y - height, CONTENT_WIDTH, height);
                stream.fill();
                float lineY = y - BOX_PAD - BODY;
                for (int i = 0; i < count; i++) {
                    text(lines.get(next + i), regular, BODY, TEXT, MARGIN + BOX_PAD, lineY);
                    lineY -= LEADING;
                }
                y -= height;
                next += count;
                if (next < lines.size()) {
                    newPage();
                }
            }
        }

        private void signatures() throws IOException {
            ensure(60);
            y -= 26;
            float gap = 24f;
            float column = (CONTENT_WIDTH - gap) / 2;
            String[] captions = {"Client signature", "Received by"};
            for (int i = 0; i < captions.length; i++) {
                float left = MARGIN + i * (column + gap);
                stream.setStrokingColor(RULE);
                stream.setLineWidth(0.8f);
                stream.setLineDashPattern(new float[0], 0);
                stream.moveTo(left, y);
                stream.lineTo(left + column, y);
                stream.stroke();
                float captionWidth = width(captions[i], regular, 8.5f);
                text(captions[i], regular, 8.5f, MUTED, left + (column - captionWidth) / 2, y - 12);
            }
            y -= 12;
        }

        private void rule(boolean dashed) throws IOException {
            stream.setStrokingColor(RULE);
            stream.setLineWidth(0.6f);
            stream.setLineDashPattern(dashed ? new float[]{3, 3} : new float[0], 0);
            stream.moveTo(MARGIN, y);
            stream.lineTo(MARGIN + CONTENT_WIDTH, y);
            stream.stroke();
            stream.setLineDashPattern(new float[0], 0);
        }

        private void centered(String value, PDFont font, float size, Color color) throws IOException {
            String text = clean(value);
            text(text, font, size, color, MARGIN + (CONTENT_WIDTH - width(text, font, size)) / 2, y);
        }

        private void text(String value, PDFont font, float size, Color color, float x, float baseline) throws IOException {
            if (value.isEmpty()) {
                return;
            }
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(value);
            stream.endText();
        }

        // ---- Paging, measuring, wrapping -----------------------------------------------------------

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_SIZE.getHeight() - MARGIN;
        }

        private void ensure(float height) throws IOException {
            if (y - height < MARGIN) {
                newPage();
            }
        }

        private float width(String text, PDFont font, float size) throws IOException {
            return text.isEmpty() ? 0 : font.getStringWidth(text) / 1000f * size;
        }

        /** Greedy word wrap on spaces; a single word wider than the line is broken by character. */
        private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (width(candidate, font, size) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                String rest = word;
                while (width(rest, font, size) > maxWidth) {
                    int cut = rest.length();
                    while (cut > 1 && width(rest.substring(0, cut), font, size) > maxWidth) {
                        cut = rest.offsetByCodePoints(cut, -1);
                    }
                    lines.add(rest.substring(0, cut));
                    rest = rest.substring(cut);
                }
                line.append(rest);
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
            return lines;
        }

        /**
         * Text safe to hand to showText: null becomes empty, every run of whitespace or control
         * characters becomes one space (newlines would throw in showText), and a character the font's
         * encoding cannot represent becomes "?".
         */
        String clean(String value) {
            if (value == null) {
                return "";
            }
            StringBuilder out = new StringBuilder();
            boolean pendingSpace = false;
            for (int i = 0; i < value.length(); ) {
                int cp = value.codePointAt(i);
                i += Character.charCount(cp);
                if (Character.isWhitespace(cp) || Character.isSpaceChar(cp) || Character.isISOControl(cp)) {
                    pendingSpace = out.length() > 0;
                    continue;
                }
                if (pendingSpace) {
                    out.append(' ');
                    pendingSpace = false;
                }
                out.appendCodePoint(encodable(cp) ? cp : '?');
            }
            return out.toString();
        }

        private boolean encodable(int codePoint) {
            try {
                regular.encode(new String(Character.toChars(codePoint)));
                bold.encode(new String(Character.toChars(codePoint)));
                return true;
            } catch (IllegalArgumentException | IOException e) {
                return false;
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** The slip's own date format - "Oct 3, 2026", day not padded (AdminRepairView.getDate pads it). */
    private static String formatDate(Date date) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(date);
    }
}
