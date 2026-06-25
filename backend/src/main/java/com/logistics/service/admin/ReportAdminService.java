package com.logistics.service.admin;

import com.logistics.dto.admin.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.repository.ReportRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReportAdminService {

    private final ReportRepository reportRepo;

    public ReportAdminService(ReportRepository reportRepo) {
        this.reportRepo = reportRepo;
    }

    public List<AdminFinancialPoint> getFinancialByDate(LocalDateTime start, LocalDateTime end) {
        return reportRepo.sumByDate(start, end);
    }

    public List<AdminShipperReportDto> getShipperReport(LocalDateTime start, LocalDateTime end) {
        return reportRepo.reportByShipper(start, end);
    }

    public List<AdminFinancialPoint> getTransferredByDate(LocalDateTime start, LocalDateTime end) {
        return reportRepo.transferredByDate(start, end);
    }

    public List<AdminFinancialPoint> getShippingFeeByDate(LocalDateTime start, LocalDateTime end) {
        return reportRepo.shippingFeeByDate(start, end);
    }

    public List<Object[]> getOrderOperationSummary(LocalDateTime start, LocalDateTime end) {
        return reportRepo.orderOperationSummary(start, end);
    }

    public List<AdminOfficeReportDto> getOfficeReport(LocalDateTime start, LocalDateTime end) {
        return reportRepo.reportByOffice(start, end);
    }

    public List<AdminShopReportDto> getShopReport(LocalDateTime start, LocalDateTime end) {
        return reportRepo.reportByShop(start, end);
    }

    public AdminOverviewDto getOverview(LocalDateTime start, LocalDateTime end) {
        Object[] r = reportRepo.overviewSummary(start, end);
        if (r == null) {
            r = new Object[10];
        }
        // Pad missing columns to length 10 to avoid AIOOBE
        if (r.length < 10) {
            Object[] padded = new Object[10];
            System.arraycopy(r, 0, padded, 0, r.length);
            r = padded;
        }

        Long totalOffices = nzLong(r[0]);
        Long totalEmployees = nzLong(r[1]);
        Long totalShippers = nzLong(r[2]);
        Long totalOrders = nzLong(r[3]);
        Long delivered = nzLong(r[4]);
        Long failed = nzLong(r[5]);
        Long returned = nzLong(r[6]);
        BigDecimal shippingRevenue = nzBigDecimal(r[7]);
        BigDecimal totalCodCollected = nzBigDecimal(r[8]);
        BigDecimal codTransferred = nzBigDecimal(r[9]);

        long inProgress = Math.max(0L, totalOrders - delivered - failed - returned);
        double successRate = totalOrders > 0
                ? ((double) delivered) / ((double) totalOrders) * 100.0
                : 0.0;
        BigDecimal codHeld = nzBigDecimal(totalCodCollected).subtract(nzBigDecimal(codTransferred));

        return new AdminOverviewDto(
            totalOffices,
            totalEmployees,
            totalShippers,
            totalOrders,
            delivered,
            failed,
            returned,
            inProgress,
            Math.round(successRate * 100.0) / 100.0,
            shippingRevenue,
            totalCodCollected,
            codTransferred,
            codHeld
        );
    }

    private static long nzLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static double nzDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static BigDecimal nzBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public byte[] exportOperationsXlsx(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = reportRepo.orderOperationSummary(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Operations");
            String[] headers = new String[] {"Date", "Total Orders", "Delivered", "Failed", "AvgDeliverySeconds"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            int rIdx = 1;
            for (Object[] r : rows) {
                Row row = sheet.createRow(rIdx++);
                LocalDate d = ReportRepository.toLocalDate(r[0]);
                row.createCell(0).setCellValue(d != null ? d.toString() : "");
                row.createCell(1).setCellValue(ReportRepository.safeBigDecimal(r[1]).doubleValue());
                row.createCell(2).setCellValue(ReportRepository.safeBigDecimal(r[2]).doubleValue());
                row.createCell(3).setCellValue(ReportRepository.safeBigDecimal(r[3]).doubleValue());
                row.createCell(4).setCellValue(ReportRepository.safeBigDecimal(r[4]).doubleValue());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public byte[] exportOfficeXlsx(LocalDateTime start, LocalDateTime end) {
        List<AdminOfficeReportDto> rows = reportRepo.reportByOffice(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Offices");
            String[] headers = new String[] {"OfficeId", "OfficeName", "TotalOrders"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            int rIdx = 1;
            for (AdminOfficeReportDto r : rows) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(r.getOfficeId() == null ? "" : r.getOfficeId().toString());
                row.createCell(1).setCellValue(r.getOfficeName() == null ? "" : r.getOfficeName());
                row.createCell(2).setCellValue(r.getTotalOrders() == null ? 0.0 : r.getTotalOrders().doubleValue());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public byte[] exportShopXlsx(LocalDateTime start, LocalDateTime end) {
        List<AdminShopReportDto> rows = reportRepo.reportByShop(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Shops");
            String[] headers = new String[] {"ShopId", "ShopName", "OrdersCount", "TotalOrderValue", "TotalShippingFee"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            int rIdx = 1;
            for (AdminShopReportDto r : rows) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(r.getShopId() == null ? "" : r.getShopId().toString());
                row.createCell(1).setCellValue(r.getShopName() == null ? "" : r.getShopName());
                row.createCell(2).setCellValue(r.getOrdersCount() == null ? 0.0 : r.getOrdersCount().doubleValue());
                row.createCell(3).setCellValue(r.getTotalOrderValue() == null ? 0.0 : r.getTotalOrderValue().doubleValue());
                row.createCell(4).setCellValue(r.getTotalShippingFee() == null ? 0.0 : r.getTotalShippingFee().doubleValue());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public byte[] exportOverviewXlsx(LocalDateTime start, LocalDateTime end) {
        AdminOverviewDto dto = getOverview(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Overview");
            int rIdx = 0;
            Row title = sheet.createRow(rIdx++);
            title.createCell(0).setCellValue("Báo cáo tổng quan");
            Row rangeRow = sheet.createRow(rIdx++);
            rangeRow.createCell(0).setCellValue("Từ ngày");
            rangeRow.createCell(1).setCellValue(start.toLocalDate().toString());
            rangeRow.createCell(2).setCellValue("Đến ngày");
            rangeRow.createCell(3).setCellValue(end.toLocalDate().toString());

            Row h = sheet.createRow(rIdx++);
            String[] headers = new String[] {"Key", "Value"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Total Offices", dto.getTotalOffices());
            map.put("Total Employees", dto.getTotalEmployees());
            map.put("Total Shippers", dto.getTotalShippers());
            map.put("Total Orders", dto.getTotalOrders());
            map.put("Delivered", dto.getDelivered());
            map.put("Failed", dto.getFailed());
            map.put("Returned", dto.getReturnedOrders());
            map.put("Success Rate (%)", dto.getSuccessRate());
            map.put("Shipping Revenue (VND)", dto.getShippingRevenue());
            map.put("Total COD Collected (VND)", dto.getTotalCodCollected());
            map.put("COD Transferred To Shop (VND)", dto.getCodTransferred());
            map.put("COD Held (VND)", dto.getCodHeld());

            for (Map.Entry<String, Object> e : map.entrySet()) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(e.getKey());
                Object v = e.getValue();
                if (v instanceof BigDecimal) row.createCell(1).setCellValue(((BigDecimal) v).doubleValue());
                else if (v instanceof Number) row.createCell(1).setCellValue(((Number) v).doubleValue());
                else row.createCell(1).setCellValue(v == null ? "" : v.toString());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public byte[] exportOfficesDetailedXlsx(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = getOfficeReportDetailed(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Offices Detailed");
            String[] headers = new String[] {"OfficeId","OfficeName","TotalOrders","Delivered","Failed","Returned","InProgress","SuccessRate(%)","ShippingRevenue(VND)","TotalCodCollected(VND)","CodSubmittedToCompany(VND)","TotalEmployees","TotalShippers"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int rIdx = 1;
            for (Map<String, Object> r : rows) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(r.get("officeId") == null ? "" : r.get("officeId").toString());
                row.createCell(1).setCellValue(r.get("officeName") == null ? "" : r.get("officeName").toString());
                row.createCell(2).setCellValue(((Number) (r.get("totalOrders") == null ? 0 : r.get("totalOrders"))).doubleValue());
                row.createCell(3).setCellValue(((Number) (r.get("delivered") == null ? 0 : r.get("delivered"))).doubleValue());
                row.createCell(4).setCellValue(((Number) (r.get("failed") == null ? 0 : r.get("failed"))).doubleValue());
                row.createCell(5).setCellValue(((Number) (r.get("returnedOrders") == null ? 0 : r.get("returnedOrders"))).doubleValue());
                row.createCell(6).setCellValue(((Number) (r.get("inProgress") == null ? 0 : r.get("inProgress"))).doubleValue());
                row.createCell(7).setCellValue(((Number) (r.get("successRate") == null ? 0 : r.get("successRate"))).doubleValue());
                BigDecimal sr = (BigDecimal) r.getOrDefault("shippingRevenue", BigDecimal.ZERO);
                BigDecimal tc = (BigDecimal) r.getOrDefault("totalCodCollected", BigDecimal.ZERO);
                BigDecimal sub = (BigDecimal) r.getOrDefault("codSubmittedToCompany", BigDecimal.ZERO);
                row.createCell(8).setCellValue(sr.doubleValue());
                row.createCell(9).setCellValue(tc.doubleValue());
                row.createCell(10).setCellValue(sub.doubleValue());
                row.createCell(11).setCellValue(((Number) (r.get("totalEmployees") == null ? 0 : r.get("totalEmployees"))).doubleValue());
                row.createCell(12).setCellValue(((Number) (r.get("totalShippers") == null ? 0 : r.get("totalShippers"))).doubleValue());
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public byte[] exportShippersDetailedXlsx(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = getShipperReportDetailed(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Shippers Detailed");
            String[] headers = new String[] {"ShipperId","ShipperName","Phone","BranchName","TotalOrders","Delivered","Failed","Returned","InProgress","SuccessRate(%)","CodCollected(VND)","CodSubmittedToCompany(VND)","CodHeldByShipper(VND)"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int rIdx = 1;
            for (Map<String, Object> r : rows) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(r.get("shipperId") == null ? "" : r.get("shipperId").toString());
                row.createCell(1).setCellValue(r.get("shipperName") == null ? "" : r.get("shipperName").toString());
                row.createCell(2).setCellValue(r.get("phone") == null ? "" : r.get("phone").toString());
                row.createCell(3).setCellValue(r.get("branchName") == null ? "" : r.get("branchName").toString());
                row.createCell(4).setCellValue(((Number) (r.get("totalOrders") == null ? 0 : r.get("totalOrders"))).doubleValue());
                row.createCell(5).setCellValue(((Number) (r.get("delivered") == null ? 0 : r.get("delivered"))).doubleValue());
                row.createCell(6).setCellValue(((Number) (r.get("failed") == null ? 0 : r.get("failed"))).doubleValue());
                row.createCell(7).setCellValue(((Number) (r.get("returnedOrders") == null ? 0 : r.get("returnedOrders"))).doubleValue());
                row.createCell(8).setCellValue(((Number) (r.get("inProgress") == null ? 0 : r.get("inProgress"))).doubleValue());
                row.createCell(9).setCellValue(((Number) (r.get("successRate") == null ? 0 : r.get("successRate"))).doubleValue());
                BigDecimal cc = (BigDecimal) r.getOrDefault("codCollected", BigDecimal.ZERO);
                BigDecimal cs = (BigDecimal) r.getOrDefault("codSubmittedToCompany", BigDecimal.ZERO);
                BigDecimal ch = (BigDecimal) r.getOrDefault("codHeldByShipper", BigDecimal.ZERO);
                row.createCell(10).setCellValue(cc.doubleValue());
                row.createCell(11).setCellValue(cs.doubleValue());
                row.createCell(12).setCellValue(ch.doubleValue());
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    public byte[] exportFinanceXlsx(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> report = getFinanceReport(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Finance");
            int rIdx = 0;
            Row title = sheet.createRow(rIdx++);
            title.createCell(0).setCellValue("Báo cáo tài chính");
            Row rangeRow = sheet.createRow(rIdx++);
            rangeRow.createCell(0).setCellValue("Từ ngày");
            rangeRow.createCell(1).setCellValue(start.toLocalDate().toString());
            rangeRow.createCell(2).setCellValue("Đến ngày");
            rangeRow.createCell(3).setCellValue(end.toLocalDate().toString());

            Row h = sheet.createRow(rIdx++);
            h.createCell(0).setCellValue("Key"); h.createCell(1).setCellValue("Value");
            Map<String, Object> codSummary = (Map<String, Object>) report.getOrDefault("codSummary", new HashMap<>());
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("Total Shipping Revenue (VND)", report.getOrDefault("shippingRevenue", BigDecimal.ZERO));
            summary.put("Total COD Collected (VND)", codSummary.getOrDefault("totalCodCollected", BigDecimal.ZERO));
            summary.put("COD Submitted To Company (VND)", codSummary.getOrDefault("codSubmittedToCompany", BigDecimal.ZERO));
            summary.put("COD Transferred To Shop (VND)", codSummary.getOrDefault("codTransferredToShop", BigDecimal.ZERO));
            summary.put("COD Held By Company (VND)", codSummary.getOrDefault("codHeldByCompany", BigDecimal.ZERO));

            for (Map.Entry<String, Object> e : summary.entrySet()) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(e.getKey());
                Object v = e.getValue();
                if (v instanceof BigDecimal) row.createCell(1).setCellValue(((BigDecimal) v).doubleValue());
                else row.createCell(1).setCellValue(v == null ? "" : v.toString());
            }

            rIdx++;
            Row cdh = sheet.createRow(rIdx++);
            String[] dayHeaders = new String[] {"Date","ShippingRevenue","CODCollected","CODSubmittedToCompany","CODTransferredToShop","CODHeldByCompany"};
            for (int i = 0; i < dayHeaders.length; i++) cdh.createCell(i).setCellValue(dayHeaders[i]);
            List<Map<String, Object>> codByDay = (List<Map<String, Object>>) report.getOrDefault("codByDay", Collections.emptyList());
            for (Map<String, Object> d : codByDay) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(d.getOrDefault("date", "").toString());
                row.createCell(1).setCellValue(((BigDecimal) d.getOrDefault("shippingRevenue", BigDecimal.ZERO)).doubleValue());
                row.createCell(2).setCellValue(((BigDecimal) d.getOrDefault("codCollected", BigDecimal.ZERO)).doubleValue());
                row.createCell(3).setCellValue(((BigDecimal) d.getOrDefault("codSubmittedToCompany", BigDecimal.ZERO)).doubleValue());
                row.createCell(4).setCellValue(((BigDecimal) d.getOrDefault("codTransferredToShop", BigDecimal.ZERO)).doubleValue());
                row.createCell(5).setCellValue(((BigDecimal) d.getOrDefault("codHeldByCompany", BigDecimal.ZERO)).doubleValue());
            }

            for (int i = 0; i < dayHeaders.length; i++) sheet.autoSizeColumn(i);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    public List<Map<String, Object>> getOfficeReportDetailed(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = reportRepo.reportByOfficeDetailed(start, end);
        List<Map<String, Object>> out = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("officeId", r[0] == null ? null : ((Number) r[0]).intValue());
            m.put("officeName", r[1] == null ? "" : r[1].toString());
            long totalOrders = ReportRepository.safeLong(r[2]);
            long delivered = ReportRepository.safeLong(r[3]);
            long failed = ReportRepository.safeLong(r[4]);
            long returned = ReportRepository.safeLong(r[5]);
            long inProgress = totalOrders - delivered - failed - returned;
            double successRate = (delivered + failed + returned) > 0 ? ((double) delivered) / ((double) (delivered + failed + returned)) * 100.0 : 0.0;
            m.put("totalOrders", totalOrders);
            m.put("delivered", delivered);
            m.put("failed", failed);
            m.put("returnedOrders", returned);
            m.put("inProgress", inProgress < 0 ? 0 : inProgress);
            m.put("successRate", Math.round(successRate * 100.0) / 100.0);
            m.put("shippingRevenue", ReportRepository.safeBigDecimal(r[7]));
            m.put("totalCodCollected", ReportRepository.safeBigDecimal(r[8]));
            m.put("codSubmittedToCompany", ReportRepository.safeBigDecimal(r[9]));
            m.put("totalEmployees", ReportRepository.safeLong(r[10]));
            m.put("totalShippers", ReportRepository.safeLong(r[11]));
            return m;
        }).toList();
        return out;
    }

    public List<Map<String, Object>> getShipperReportDetailed(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = reportRepo.reportByShipperDetailed(start, end);
        List<Map<String, Object>> out = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("shipperId", r[0] == null ? null : ((Number) r[0]).intValue());
            m.put("shipperName", r[1] == null ? "" : r[1].toString());
            m.put("phone", r[2] == null ? "" : r[2].toString());
            m.put("branchName", r[3] == null ? "" : r[3].toString());
            long totalOrders = ReportRepository.safeLong(r[4]);
            long delivered = ReportRepository.safeLong(r[5]);
            long failed = ReportRepository.safeLong(r[6]);
            long returned = ReportRepository.safeLong(r[7]);
            long inProgress = ReportRepository.safeLong(r[8]);
            double successRate = (delivered + failed + returned) > 0 ? ((double) delivered) / ((double) (delivered + failed + returned)) * 100.0 : 0.0;
            m.put("totalOrders", totalOrders);
            m.put("delivered", delivered);
            m.put("failed", failed);
            m.put("returnedOrders", returned);
            m.put("inProgress", inProgress);
            m.put("successRate", Math.round(successRate * 100.0) / 100.0);
            BigDecimal codCollected = ReportRepository.safeBigDecimal(r[9]);
            BigDecimal codSubmitted = ReportRepository.safeBigDecimal(r[10]);
            m.put("codCollected", codCollected);
            m.put("codSubmittedToCompany", codSubmitted);
            m.put("codHeldByShipper", codCollected.subtract(codSubmitted));
            return m;
        }).toList();
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFinanceReport(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> out = new HashMap<>();
        List<Object[]> byDay = reportRepo.financeReportByDay(start, end);
        List<Map<String, Object>> codByDay = byDay.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            LocalDate d = ReportRepository.toLocalDate(r[0]);
            m.put("date", d != null ? d.toString() : null);
            BigDecimal shippingRevenue = ReportRepository.safeBigDecimal(r[1]);
            BigDecimal codCollected = ReportRepository.safeBigDecimal(r[2]);
            BigDecimal codSubmitted = ReportRepository.safeBigDecimal(r[3]);
            BigDecimal codTransferred = ReportRepository.safeBigDecimal(r[4]);
            m.put("shippingRevenue", shippingRevenue);
            m.put("codCollected", codCollected);
            m.put("codSubmittedToCompany", codSubmitted);
            m.put("codTransferredToShop", codTransferred);
            m.put("codHeldByCompany", codCollected.subtract(codTransferred));
            return m;
        }).toList();

        List<Object[]> byBranch = reportRepo.financeReportByBranch(start, end);
        List<Map<String, Object>> revenueByBranch = byBranch.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("officeId", r[0] == null ? null : ((Number) r[0]).intValue());
            m.put("officeName", r[1] == null ? "" : r[1].toString());
            BigDecimal shippingRevenue = ReportRepository.safeBigDecimal(r[2]);
            BigDecimal codCollected = ReportRepository.safeBigDecimal(r[3]);
            BigDecimal codSubmitted = ReportRepository.safeBigDecimal(r[4]);
            BigDecimal codTransferred = ReportRepository.safeBigDecimal(r[5]);
            m.put("shippingRevenue", shippingRevenue);
            m.put("codCollected", codCollected);
            m.put("codSubmittedToCompany", codSubmitted);
            m.put("codTransferredToShop", codTransferred);
            m.put("codHeldByCompany", codCollected.subtract(codTransferred));
            return m;
        }).toList();

        BigDecimal totalShippingRevenue = byDay.stream().map(r -> ReportRepository.safeBigDecimal(r[1])).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCodCollected = byDay.stream().map(r -> ReportRepository.safeBigDecimal(r[2])).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCodTransferred = byDay.stream().map(r -> ReportRepository.safeBigDecimal(r[4])).reduce(BigDecimal.ZERO, BigDecimal::add);

        out.put("shippingRevenue", totalShippingRevenue);
        out.put("revenueByDay", codByDay.stream().map(d -> { Map<String, Object> m = new HashMap<>(); m.put("date", d.get("date")); m.put("shippingRevenue", d.get("shippingRevenue")); return m; }).toList());
        out.put("revenueByBranch", revenueByBranch);

        Map<String, Object> codSummary = new HashMap<>();
        codSummary.put("totalCodCollected", totalCodCollected);
        codSummary.put("codSubmittedToCompany", byBranch.stream().map(r -> ReportRepository.safeBigDecimal(r[4])).reduce(BigDecimal.ZERO, BigDecimal::add));
        codSummary.put("codTransferredToShop", totalCodTransferred);
        codSummary.put("codHeldByCompany", totalCodCollected.subtract(totalCodTransferred));

        out.put("codSummary", codSummary);
        out.put("codByDay", codByDay);
        out.put("codByBranch", revenueByBranch);

        return out;
    }
}
