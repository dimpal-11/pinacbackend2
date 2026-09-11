package in.sb.pinac.controller.admin;

import in.sb.pinac.service.admin.AdminExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/export")
public class AdminExportController {

    @Autowired
    private AdminExcelExportService adminExcelExportService;

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(defaultValue = "payments") String type,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        byte[] excelBytes = adminExcelExportService.exportExcel(type, timeframe, startDate, endDate);
        String filename = "PINAC_" + capitalize(type) + "_Report_" + System.currentTimeMillis() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "payments") String type,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        byte[] csvBytes = adminExcelExportService.exportCsv(type, timeframe, startDate, endDate);
        String filename = "PINAC_" + capitalize(type) + "_Report_" + System.currentTimeMillis() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Export";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
