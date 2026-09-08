package com.huizhipay.acquiring.transfi.util;

import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Invoice PDF 渲染工具（OpenPDF / org.openpdf）。
 * <p>
 * 唯一公开路径：Map data → Spring {@link Resource}，
 * 可直接作为 {@code TransFiClient.uploadInvoice(@RequestPart Resource)} 入参。
 * </p>
 */
public final class PdfUtil {

    private PdfUtil() {
    }

    private static final Color BRAND_BLUE = new Color(31, 73, 125);
    private static final Color MUTED_GRAY = new Color(120, 120, 120);

    private static final String[] CN_FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
    };

    // ==================== 公共入口 ====================

    /**
     * 根据 HuizhiPay 结算发票模板渲染 PDF，返回带文件名的 Spring Resource。
     * <p>支持的 key：
     * order_id / created_at / buyer_name / buyer_email_masked / buyer_country
     * / merchant_clean_name / safe_product_description / mapped_sku
     * / fiat_amount / fiat_currency
     */
    public static Resource renderInvoice(Map<String, String> data) {
        Map<String, String> d = fillDefaults(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document doc = new Document(PageSize.A4, 48f, 48f, 54f, 54f);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            writeInvoiceBody(doc, d);
        } catch (DocumentException e) {
            throw new RuntimeException("渲染发票 PDF 失败", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }

        String filename = "invoice-" + d.get("order_id") + ".pdf";
        return new NamedPdfResource(baos.toByteArray(), filename);
    }

    // ==================== 字体 ====================

    private static Font loadFont(float size, int style) {
        for (String path : CN_FONT_CANDIDATES) {
            try {
                BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, style);
            } catch (Exception ignored) {
            }
        }
        return new Font(Font.HELVETICA, size, style);
    }

    private static Font normal(float size) {
        return loadFont(size, Font.NORMAL);
    }

    private static Font bold(float size) {
        return loadFont(size, Font.BOLD);
    }

    // ==================== 正文 ====================

    private static void writeInvoiceBody(Document doc, Map<String, String> d) throws DocumentException {
        Font brandF = bold(14f);
        Font taglineF = normal(10f);
        Font titleF = bold(22f);
        Font metaL = normal(10f);
        Font metaV = normal(11f);
        Font secL = bold(10f);
        Font secV = normal(12f);
        Font thF = normal(10f);
        Font tdF = normal(11f);
        Font totalL = bold(12f);
        Font totalV = bold(13f);
        Font footerF = normal(9f);

        Paragraph spacer = new Paragraph(" ", normal(12f));
        spacer.setSpacingAfter(6f);

        // --- 品牌 ---
        Paragraph brand = new Paragraph("HuizhiPay Settlement Services", brandF);
        brand.setSpacingAfter(2f);
        doc.add(brand);

        Paragraph tagline = new Paragraph("Authorized Technology Routing Provider", taglineF);
        tagline.getFont().setColor(MUTED_GRAY);
        tagline.setSpacingAfter(28f);
        doc.add(tagline);

        doc.add(horizontalLine());
        doc.add(spacer);

        // --- INVOICE ---
        Paragraph title = new Paragraph("INVOICE", titleF);
        title.getFont().setColor(BRAND_BLUE);
        title.setSpacingAfter(18f);
        doc.add(title);

        doc.add(metaRow("Invoice No:", "#" + d.get("order_id"), metaL, metaV));
        doc.add(metaRow("Date:", d.get("created_at"), metaL, metaV));
        doc.add(metaRow("Status:", "PENDING SETTLEMENT", metaL, metaV));
        doc.add(spacer);
        doc.add(horizontalLine());
        doc.add(spacer);
        doc.add(spacer);

        // --- 两列地址（PdfPCell 只能用构造器 / setPhrase，不能 .add()）---
        PdfPTable twoCol = new PdfPTable(new float[]{50f, 50f});
        twoCol.setWidthPercentage(100f);
        twoCol.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        twoCol.addCell(addressCell(secL, secV,
                "Billed To (End User)",
                d.get("buyer_name"), d.get("buyer_email_masked"),
                "Country: " + d.get("buyer_country")));
        twoCol.addCell(addressCell(secL, secV,
                "Merchant of Record",
                d.get("merchant_clean_name"),
                "Processed via HuizhiPay API", "Hong Kong SAR"));
        doc.add(twoCol);
        doc.add(spacer);
        doc.add(horizontalLine());
        doc.add(spacer);

        // --- 明细 ---
        PdfPTable items = new PdfPTable(new float[]{55f, 10f, 17.5f, 17.5f});
        items.setWidthPercentage(100f);

        tableHeader(items, "Description", thF, Element.ALIGN_LEFT);
        tableHeader(items, "Qty", thF, Element.ALIGN_CENTER);
        tableHeader(items, "Unit Price", thF, Element.ALIGN_RIGHT);
        tableHeader(items, "Total", thF, Element.ALIGN_RIGHT);

        String amtCur = d.get("fiat_amount") + " " + d.get("fiat_currency");

        PdfPCell desc = new PdfPCell(new Phrase(
                d.get("safe_product_description") + " (SKU: " + d.get("mapped_sku") + ")", tdF));
        desc.setBorder(Rectangle.NO_BORDER);
        desc.setPaddingTop(6f);
        desc.setPaddingBottom(6f);
        items.addCell(desc);

        PdfPCell qty = new PdfPCell(new Phrase("1", tdF));
        qty.setHorizontalAlignment(Element.ALIGN_CENTER);
        qty.setBorder(Rectangle.NO_BORDER);
        items.addCell(qty);

        PdfPCell up = new PdfPCell(new Phrase(amtCur, tdF));
        up.setHorizontalAlignment(Element.ALIGN_RIGHT);
        up.setBorder(Rectangle.NO_BORDER);
        items.addCell(up);

        PdfPCell tot = new PdfPCell(new Phrase(amtCur, tdF));
        tot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tot.setBorder(Rectangle.NO_BORDER);
        items.addCell(tot);

        doc.add(items);
        doc.add(spacer);

        // --- Total Due ---
        PdfPTable totalTbl = new PdfPTable(new float[]{75f, 25f});
        totalTbl.setWidthPercentage(100f);
        totalTbl.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell tl = new PdfPCell(new Phrase("Total Due:", totalL));
        tl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTbl.addCell(tl);

        PdfPCell tv = new PdfPCell(new Phrase(amtCur, totalV));
        tv.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTbl.addCell(tv);

        doc.add(totalTbl);
        doc.add(spacer);
        doc.add(horizontalLine());
        doc.add(spacer);
        doc.add(spacer);

        // --- Footer ---
        Paragraph f1 = new Paragraph(
                "This invoice is generated automatically by the HuizhiPay Routing Middleware for reconciliation purposes.",
                footerF);
        f1.getFont().setColor(MUTED_GRAY);
        f1.setSpacingAfter(4f);
        doc.add(f1);

        Paragraph f2 = new Paragraph(
                "Settlement Facilitated by Licensed Partners (TransFi / Fiat-to-Crypto API).",
                footerF);
        f2.getFont().setColor(MUTED_GRAY);
        doc.add(f2);
    }

    // ==================== 辅助 ====================

    private static Map<String, String> fillDefaults(Map<String, String> input) {
        Map<String, String> m = new LinkedHashMap<>();
        if (input != null) m.putAll(input);
        m.putIfAbsent("order_id", "INV-UNKNOWN");
        m.putIfAbsent("created_at", "—");
        m.putIfAbsent("buyer_name", "—");
        m.putIfAbsent("buyer_email_masked", "—");
        m.putIfAbsent("buyer_country", "—");
        m.putIfAbsent("merchant_clean_name", "—");
        m.putIfAbsent("safe_product_description", "—");
        m.putIfAbsent("mapped_sku", "—");
        m.putIfAbsent("fiat_amount", "0.00");
        m.putIfAbsent("fiat_currency", "USD");
        return m;
    }

    private static Paragraph metaRow(String label, String value, Font labelF, Font valueF) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", labelF));
        p.add(new Chunk(value, valueF));
        p.setSpacingAfter(4f);
        return p;
    }

    private static Paragraph horizontalLine() {
        Paragraph line = new Paragraph();
        line.add(new Chunk(new LineSeparator(1f, 100f, MUTED_GRAY, Element.ALIGN_LEFT, -2f)));
        return line;
    }

    /**
     * 构造地址单元格。
     * 注意：PdfPCell 没有 .add()，只能把所有内容先合成一个 Paragraph 再传入构造器。
     */
    private static PdfPCell addressCell(Font headingF, Font bodyF,
                                        String heading, String line1, String line2, String line3) {
        Paragraph content = new Paragraph();
        content.add(new Chunk(heading + "\n", headingF));
        content.add(new Chunk(line1 + "\n", bodyF));
        content.add(new Chunk(line2 + "\n", bodyF));
        content.add(new Chunk(line3, bodyF));

        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static void tableHeader(PdfPTable table, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(new Color(245, 245, 245));
        c.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        c.setHorizontalAlignment(align);
        c.setPaddingTop(6f);
        c.setPaddingBottom(6f);
        table.addCell(c);
    }

    private static class NamedPdfResource extends ByteArrayResource {
        private final String filename;

        NamedPdfResource(byte[] data, String filename) {
            super(data);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public String getDescription() {
            return "PDF [" + filename + "]";
        }
    }
}
