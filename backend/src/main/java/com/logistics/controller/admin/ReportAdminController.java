package com.logistics.controller.admin;

import com.logistics.dto.admin.AdminFinancialPoint;
import com.logistics.dto.admin.AdminShipperReportDto;
import com.logistics.dto.admin.AdminOfficeReportDto;
import com.logistics.dto.admin.AdminShopReportDto;
import com.logistics.service.admin.ReportAdminService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportAdminController {

    private final ReportAdminService reportService;

    public ReportAdminController(ReportAdminService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/financial")
    public ResponseEntity<List<AdminFinancialPoint>> financial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<com.logistics.dto.admin.AdminFinancialPoint> points = reportService.getFinancialByDate(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(points);
    }

    @GetMapping("/shipper")
    public ResponseEntity<List<AdminShipperReportDto>> shippers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminShipperReportDto> list = reportService.getShipperReport(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/shipper/{shipperId}")
    public ResponseEntity<AdminShipperReportDto> shipperDetail(@PathVariable Integer shipperId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminShipperReportDto> list = reportService.getShipperReport(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(list.stream().filter(it -> it.getShipperId().equals(shipperId)).findFirst().orElse(null));
    }

    @GetMapping("/transferred")
    public ResponseEntity<List<AdminFinancialPoint>> transferred(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminFinancialPoint> points = reportService.getTransferredByDate(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(points);
    }

    @GetMapping("/fees")
    public ResponseEntity<List<AdminFinancialPoint>> fees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminFinancialPoint> points = reportService.getShippingFeeByDate(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(points);
    }

    @GetMapping("/operations")
    public ResponseEntity<List<Map<String, Object>>> operations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<Object[]> rows = reportService.getOrderOperationSummary(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        List<Map<String, Object>> out = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            Date d = (Date) r[0];
            m.put("date", d != null ? d.toLocalDate().toString() : null);
            m.put("totalOrders", r[1] == null ? 0 : ((Number) r[1]).longValue());
            m.put("delivered", r[2] == null ? 0 : ((Number) r[2]).longValue());
            m.put("failed", r[3] == null ? 0 : ((Number) r[3]).longValue());
            m.put("avgDeliverySeconds", r[4] == null ? 0 : ((Number) r[4]).doubleValue());
            m.put("returning", r[5] == null ? 0 : ((Number) r[5]).longValue());
            m.put("returned", r[6] == null ? 0 : ((Number) r[6]).longValue());
            long total = m.get("totalOrders") == null ? 0L : ((Number) m.get("totalOrders")).longValue();
            long returned = m.get("returned") == null ? 0L : ((Number) m.get("returned")).longValue();
            double returnRate = total > 0 ? ((double) returned) / ((double) total) : 0.0;
            m.put("returnRate", returnRate);
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/operations/export")
    public ResponseEntity<byte[]> exportOperations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        byte[] data = reportService.exportOperationsXlsx(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=operations_report.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(data);
    }

    @GetMapping("/office")
    public ResponseEntity<List<AdminOfficeReportDto>> officeReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        return ResponseEntity.ok(reportService.getOfficeReport(s.atStartOfDay(), e.atTime(LocalTime.MAX)));
    }

    @GetMapping("/shop")
    public ResponseEntity<List<AdminShopReportDto>> shopReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        return ResponseEntity.ok(reportService.getShopReport(s.atStartOfDay(), e.atTime(LocalTime.MAX)));
    }

    @GetMapping("/office/export")
    public ResponseEntity<byte[]> exportOffice(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        byte[] data = reportService.exportOfficeXlsx(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=office_report.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(data);
    }

    @GetMapping("/shop/export")
    public ResponseEntity<byte[]> exportShop(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        byte[] data = reportService.exportShopXlsx(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=shop_report.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(data);
    }
}
