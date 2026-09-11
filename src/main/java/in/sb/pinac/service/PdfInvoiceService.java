package in.sb.pinac.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import in.sb.pinac.entity.Payment;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfInvoiceService {

    public byte[] generateInvoicePdf(Payment payment) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            Color brandPurple = new Color(124, 45, 141);
            Color darkGray = new Color(40, 40, 40);
            Color lightBg = new Color(248, 241, 250);
            Color borderGray = new Color(230, 220, 235);

            // Fonts
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, brandPurple);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkGray);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, darkGray);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, brandPurple);
            Font whiteBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3f, 2f});

            // Left Header (Company Branding)
            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.addElement(new Paragraph("PINAC INSTITUTE", headerFont));
            brandCell.addElement(new Paragraph("Learn Today. Build Tomorrow.", subHeaderFont));
            brandCell.addElement(new Paragraph("Suyojit Sankul, Sharanpur Road, Nashik, Maharashtra 422002", textFont));
            brandCell.addElement(new Paragraph("Email: support@pinacinstitute.com | Phone: +91 72191 94211", textFont));
            headerTable.addCell(brandCell);

            // Right Header (Invoice Meta)
            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(Rectangle.NO_BORDER);
            metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph invTitle = new Paragraph("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, brandPurple));
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(invTitle);

            Paragraph invNo = new Paragraph("Invoice No: " + payment.getInvoiceNumber(), boldFont);
            invNo.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(invNo);

            Paragraph orderNo = new Paragraph("Order ID: " + payment.getOrderId(), textFont);
            orderNo.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(orderNo);

            String dateStr = payment.getCreatedAt() != null
                    ? payment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                    : "N/A";
            Paragraph dateP = new Paragraph("Date: " + dateStr, textFont);
            dateP.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(dateP);

            Paragraph statusP = new Paragraph("Status: PAID", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(16, 149, 93)));
            statusP.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(statusP);

            headerTable.addCell(metaCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // Divider Line
            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(brandPurple);
            lineCell.setFixedHeight(2f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineTable.addCell(lineCell);
            document.add(lineTable);

            document.add(new Paragraph(" "));

            // Billed To Section
            PdfPTable billTable = new PdfPTable(1);
            billTable.setWidthPercentage(100);
            PdfPCell billCell = new PdfPCell();
            billCell.setBackgroundColor(lightBg);
            billCell.setBorderColor(borderGray);
            billCell.setPadding(10);

            billCell.addElement(new Paragraph("BILLED TO (STUDENT DETAILS)", titleFont));
            billCell.addElement(new Paragraph("Student Name : " + (payment.getUser() != null ? payment.getUser().getName() : "Valued Learner"), boldFont));
            billCell.addElement(new Paragraph("Student ID   : " + (payment.getUser() != null && payment.getUser().getStudentId() != null ? payment.getUser().getStudentId() : "N/A"), textFont));
            billCell.addElement(new Paragraph("Email Address: " + (payment.getUser() != null ? payment.getUser().getEmail() : "N/A"), textFont));
            billCell.addElement(new Paragraph("Mobile Number: " + (payment.getUser() != null && payment.getUser().getMobile() != null ? payment.getUser().getMobile() : "N/A"), textFont));

            billTable.addCell(billCell);
            document.add(billTable);

            document.add(new Paragraph(" "));

            // Line Items Table
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{1f, 4f, 2f, 2f});

            // Table Header
            String[] headers = {"#", "Item Description", "Category", "Amount (INR)"};
            for (String h : headers) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, whiteBold));
                hCell.setBackgroundColor(brandPurple);
                hCell.setPadding(8);
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(hCell);
            }

            // Table Row
            PdfPCell c1 = new PdfPCell(new Phrase("1", textFont));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setPadding(8);
            itemsTable.addCell(c1);

            String courseTitle = payment.getCourse() != null ? payment.getCourse().getTitle() : "Online Learning Course";
            PdfPCell c2 = new PdfPCell(new Phrase(courseTitle + "\n(Lifetime Access + Mentorship + Certificate)", textFont));
            c2.setPadding(8);
            itemsTable.addCell(c2);

            String category = payment.getCourse() != null && payment.getCourse().getCategory() != null ? payment.getCourse().getCategory() : "Professional Skills";
            PdfPCell c3 = new PdfPCell(new Phrase(category, textFont));
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            c3.setPadding(8);
            itemsTable.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Phrase("₹" + String.format("%.2f", payment.getAmount()), boldFont));
            c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c4.setPadding(8);
            itemsTable.addCell(c4);

            document.add(itemsTable);

            // Total Breakdown Table
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.setWidths(new float[]{1.5f, 1.5f});

            addTotalRow(totalTable, "Subtotal:", "₹" + String.format("%.2f", payment.getAmount()), textFont, textFont);
            addTotalRow(totalTable, "Taxes (GST 18%):", "Included", textFont, textFont);
            addTotalRow(totalTable, "Total Paid:", "₹" + String.format("%.2f", payment.getAmount()), boldFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, brandPurple));

            document.add(totalTable);

            document.add(new Paragraph(" "));

            // Payment Details & QR Verification Section
            PdfPTable footerInfo = new PdfPTable(2);
            footerInfo.setWidthPercentage(100);
            footerInfo.setWidths(new float[]{3f, 2f});

            PdfPCell payDetailsCell = new PdfPCell();
            payDetailsCell.setBorder(Rectangle.NO_BORDER);
            payDetailsCell.addElement(new Paragraph("PAYMENT TRANSACTION DETAILS", titleFont));
            payDetailsCell.addElement(new Paragraph("Payment Gateway: " + (payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "Razorpay"), textFont));
            payDetailsCell.addElement(new Paragraph("Payment ID: " + (payment.getPaymentId() != null ? payment.getPaymentId() : "PAY_" + payment.getId()), textFont));
            payDetailsCell.addElement(new Paragraph("Security: 256-bit Encrypted Transaction Verified", textFont));
            footerInfo.addCell(payDetailsCell);

            PdfPCell stampCell = new PdfPCell();
            stampCell.setBorder(Rectangle.NO_BORDER);
            stampCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph authorizedP = new Paragraph("Authorized Signatory\nPINAC Institute of Creative Media", boldFont);
            authorizedP.setAlignment(Element.ALIGN_RIGHT);
            stampCell.addElement(authorizedP);
            footerInfo.addCell(stampCell);

            document.add(footerInfo);

            document.add(new Paragraph(" "));

            // Terms & Conditions
            Paragraph termsHeader = new Paragraph("Terms & Conditions:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.GRAY));
            Paragraph termsBody = new Paragraph(
                    "1. This is a computer-generated invoice and requires no physical signature.\n" +
                    "2. Course access is granted for lifetime for the registered student account.\n" +
                    "3. For course support and mentorship queries, email support@pinacinstitute.com.",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)
            );
            document.add(termsHeader);
            document.add(termsBody);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPadding(4);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setPadding(4);
        table.addCell(vCell);
    }
}
