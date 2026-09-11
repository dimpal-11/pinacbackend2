package in.sb.pinac.service;

import in.sb.pinac.entity.Payment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public byte[] generatePaymentsExcel(List<Payment> payments) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Successful Purchases");

            // Create Header Style (Purple background, Bold White text)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Create Data Row Style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Create Currency / Price Style
            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setAlignment(HorizontalAlignment.RIGHT);
            amountStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            DataFormat format = workbook.createDataFormat();
            amountStyle.setDataFormat(format.getFormat("₹#,##0.00"));

            // Exact 12 Columns Matching Google Sheet & Excel
            String[] columns = {
                    "Date & Time",
                    "User Name",
                    "User Email",
                    "Mobile Number",
                    "City",
                    "Course Name",
                    "Course ID",
                    "Course Price",
                    "Payment ID",
                    "Order ID",
                    "Payment Status",
                    "Course Active"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filter for successful purchases
            List<Payment> successfulPayments = payments.stream()
                    .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                    .collect(Collectors.toList());

            // If empty, fall back to all payments
            if (successfulPayments.isEmpty()) {
                successfulPayments = payments;
            }

            int rowIdx = 1;
            for (Payment p : successfulPayments) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                // 1. Date & Time
                String dateStr = p.getCreatedAt() != null
                        ? p.getCreatedAt().format(DATE_FORMATTER)
                        : "";
                Cell c0 = row.createCell(0);
                c0.setCellValue(dateStr);
                c0.setCellStyle(dataStyle);

                // 2. User Name
                Cell c1 = row.createCell(1);
                c1.setCellValue(p.getUser() != null ? p.getUser().getName() : "Learner");
                c1.setCellStyle(dataStyle);

                // 3. User Email
                Cell c2 = row.createCell(2);
                c2.setCellValue(p.getUser() != null ? p.getUser().getEmail() : "");
                c2.setCellStyle(dataStyle);

                // 4. Mobile Number
                Cell c3 = row.createCell(3);
                c3.setCellValue(p.getUser() != null && p.getUser().getMobile() != null ? p.getUser().getMobile() : "");
                c3.setCellStyle(dataStyle);

                // 5. City
                Cell c4 = row.createCell(4);
                c4.setCellValue(p.getUser() != null && p.getUser().getCity() != null ? p.getUser().getCity() : "Nashik");
                c4.setCellStyle(dataStyle);

                // 6. Course Name
                Cell c5 = row.createCell(5);
                c5.setCellValue(p.getCourse() != null ? p.getCourse().getTitle() : "PINAC Course");
                c5.setCellStyle(dataStyle);

                // 7. Course ID
                Cell c6 = row.createCell(6);
                c6.setCellValue(p.getCourse() != null && p.getCourse().getId() != null ? p.getCourse().getId().toString() : "1");
                c6.setCellStyle(dataStyle);

                // 8. Course Price
                Cell c7 = row.createCell(7);
                c7.setCellValue(p.getAmount() != null ? p.getAmount() : 0.0);
                c7.setCellStyle(amountStyle);

                // 9. Payment ID
                Cell c8 = row.createCell(8);
                c8.setCellValue(p.getPaymentId() != null ? p.getPaymentId() : "");
                c8.setCellStyle(dataStyle);

                // 10. Order ID
                Cell c9 = row.createCell(9);
                c9.setCellValue(p.getOrderId() != null ? p.getOrderId() : "");
                c9.setCellStyle(dataStyle);

                // 11. Payment Status
                Cell c10 = row.createCell(10);
                c10.setCellValue(p.getPaymentStatus() != null ? p.getPaymentStatus() : "SUCCESS");
                c10.setCellStyle(dataStyle);

                // 12. Course Active
                Cell c11 = row.createCell(11);
                c11.setCellValue(("SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) || "SUCCESSFUL".equalsIgnoreCase(p.getPaymentStatus())) ? "Yes" : "No");
                c11.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
