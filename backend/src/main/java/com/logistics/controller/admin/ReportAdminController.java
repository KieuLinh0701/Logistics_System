package com.logistics.controller.admin;

import com.logistics.dto.admin.*;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.ReportAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Report", description = "Báo cáo thống kê và xuất dữ liệu")
public class ReportAdminController {

    private static final Logger log = LoggerFactory.getLogger(ReportAdminController.class);

    private final ReportAdminService reportService;

    public ReportAdminController(ReportAdminService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/financial")
    public ResponseEntity<ApiResponse<List<AdminFinancialPoint>>> financial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        try {
            List<com.logistics.dto.admin.AdminFinancialPoint> points = reportService.getFinancialByDate(s.atStartOfDay(), e.atTime(LocalTime.MAX));
            return ResponseEntity.ok(ApiResponse.success(points));
        } catch (Exception ex) {
            log.error("[ADMIN_REPORT_ERROR] endpoint=financial start={} end={}", s, e, ex);
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }
    }

    @GetMapping("/shipper")
    public ResponseEntity<ApiResponse<List<AdminShipperReportDto>>> shippersSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminShipperReportDto> list = reportService.getShipperReport(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/shipper/{shipperId}")
    public ResponseEntity<ApiResponse<AdminShipperReportDto>> shipperDetail(@PathVariable Integer shipperId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminShipperReportDto> list = reportService.getShipperReport(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ApiResponse.success(list.stream().filter(it -> it.getShipperId().equals(shipperId)).findFirst().orElse(null)));
    }

    @GetMapping("/transferred")
    public ResponseEntity<ApiResponse<List<AdminFinancialPoint>>> transferred(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminFinancialPoint> points = reportService.getTransferredByDate(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ApiResponse.success(points));
    }

    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<List<AdminFinancialPoint>>> fees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<AdminFinancialPoint> points = reportService.getShippingFeeByDate(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ApiResponse.success(points));
    }

    @GetMapping("/operations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> operations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        try {
            List<Object[]> rows = reportService.getOrderOperationSummary(s.atStartOfDay(), e.atTime(LocalTime.MAX));
            List<Map<String, Object>> out = rows.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                LocalDate d = com.logistics.repository.ReportRepository.toLocalDate(r[0]);
                m.put("date", d != null ? d.toString() : null);
                m.put("totalOrders", com.logistics.repository.ReportRepository.safeLong(r[1]));
                m.put("delivered", com.logistics.repository.ReportRepository.safeLong(r[2]));
                m.put("failed", com.logistics.repository.ReportRepository.safeLong(r[3]));
                m.put("avgDeliverySeconds", com.logistics.repository.ReportRepository.safeBigDecimal(r[4]).doubleValue());
                m.put("returning", com.logistics.repository.ReportRepository.safeLong(r[5]));
                m.put("returned", com.logistics.repository.ReportRepository.safeLong(r[6]));
                long total = ((Number) m.get("totalOrders")).longValue();
                long returned = ((Number) m.get("returned")).longValue();
                double returnRate = total > 0 ? ((double) returned) / ((double) total) : 0.0;
                m.put("returnRate", returnRate);
                return m;
            }).toList();
            return ResponseEntity.ok(ApiResponse.success(out));
        } catch (Exception ex) {
            log.error("[ADMIN_REPORT_ERROR] endpoint=operations start={} end={}", s, e, ex);
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminOverviewDto>> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        try {
            AdminOverviewDto dto = reportService.getOverview(s.atStartOfDay(), e.atTime(LocalTime.MAX));
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (Exception ex) {
            log.error("[ADMIN_REPORT_ERROR] endpoint=overview start={} end={}", s, e, ex);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/offices")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> offices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        List<java.util.Map<String, Object>> list = reportService.getOfficeReportDetailed(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/shippers")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> shippers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        try {
            List<java.util.Map<String, Object>> list = reportService.getShipperReportDetailed(s.atStartOfDay(), e.atTime(LocalTime.MAX));
            return ResponseEntity.ok(ApiResponse.success(list));
        } catch (Exception ex) {
            log.error("[ADMIN_REPORT_ERROR] endpoint=shippers start={} end={}", s, e, ex);
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }
    }

    @GetMapping("/finance")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> finance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        java.util.Map<String, Object> report = reportService.getFinanceReport(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ApiResponse.success(report));
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

        @GetMapping("/overview/export")
        public ResponseEntity<byte[]> exportOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        byte[] data = reportService.exportOverviewXlsx(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=overview_report.xlsx")
            .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .body(data);
        }

        @GetMapping("/offices/export")
        public ResponseEntity<byte[]> exportOfficesDetailed(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        byte[] data = reportService.exportOfficesDetailedXlsx(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=offices_detailed_report.xlsx")
            .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .body(data);
        }

        @GetMapping("/shippers/export")
        public ResponseEntity<byte[]> exportShippersDetailed(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        byte[] data = reportService.exportShippersDetailedXlsx(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=shippers_detailed_report.xlsx")
            .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .body(data);
        }

        @GetMapping("/finance/export")
        public ResponseEntity<byte[]> exportFinance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        byte[] data = reportService.exportFinanceXlsx(s.atStartOfDay(), e.atTime(LocalTime.MAX));
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=finance_report.xlsx")
            .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .body(data);
        }

    @GetMapping("/office")
    public ResponseEntity<ApiResponse<List<AdminOfficeReportDto>>> officeReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        return ResponseEntity.ok(ApiResponse.success(reportService.getOfficeReport(s.atStartOfDay(), e.atTime(LocalTime.MAX))));
    }

    @GetMapping("/shop")
    public ResponseEntity<ApiResponse<List<AdminShopReportDto>>> shopReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().minusDays(30) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        return ResponseEntity.ok(ApiResponse.success(reportService.getShopReport(s.atStartOfDay(), e.atTime(LocalTime.MAX))));
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
