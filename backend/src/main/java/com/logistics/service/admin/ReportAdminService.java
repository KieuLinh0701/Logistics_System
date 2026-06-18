package com.logistics.service.admin;

import com.logistics.dto.admin.AdminFinancialPoint;
import com.logistics.dto.admin.AdminShipperReportDto;
import com.logistics.dto.admin.AdminOfficeReportDto;
import com.logistics.dto.admin.AdminShopReportDto;
import com.logistics.dto.admin.AdminOverviewDto;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.repository.ReportRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

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
        Long totalOffices = r[0] == null ? 0L : ((Number) r[0]).longValue();
        Long totalEmployees = r[1] == null ? 0L : ((Number) r[1]).longValue();
        Long totalShippers = r[2] == null ? 0L : ((Number) r[2]).longValue();
        Long totalOrders = r[3] == null ? 0L : ((Number) r[3]).longValue();
        Long delivered = r[4] == null ? 0L : ((Number) r[4]).longValue();
        Long failed = r[5] == null ? 0L : ((Number) r[5]).longValue();
        Long returned = r[6] == null ? 0L : ((Number) r[6]).longValue();
        BigDecimal shippingRevenue = r[7] == null ? BigDecimal.ZERO : new BigDecimal(r[7].toString());
        BigDecimal totalCodCollected = r[8] == null ? BigDecimal.ZERO : new BigDecimal(r[8].toString());
        BigDecimal codTransferred = r[9] == null ? BigDecimal.ZERO : new BigDecimal(r[9].toString());

        Long inProgress = totalOrders - delivered - failed - returned;
        double successRate = totalOrders > 0 ? ((double) delivered) / ((double) totalOrders) * 100.0 : 0.0;
        BigDecimal codHeld = totalCodCollected.subtract(codTransferred);

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
                Date d = (Date) r[0];
                row.createCell(0).setCellValue(d != null ? d.toString() : "");
                row.createCell(1).setCellValue(r[1] == null ? 0.0 : ((Number) r[1]).doubleValue());
                row.createCell(2).setCellValue(r[2] == null ? 0.0 : ((Number) r[2]).doubleValue());
                row.createCell(3).setCellValue(r[3] == null ? 0.0 : ((Number) r[3]).doubleValue());
                row.createCell(4).setCellValue(r[4] == null ? 0.0 : ((Number) r[4]).doubleValue());
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
            long totalOrders = r[2] == null ? 0L : ((Number) r[2]).longValue();
            long delivered = r[3] == null ? 0L : ((Number) r[3]).longValue();
            long failed = r[4] == null ? 0L : ((Number) r[4]).longValue();
            long returned = r[5] == null ? 0L : ((Number) r[5]).longValue();
            long inProgress = totalOrders - delivered - failed - returned;
            double successRate = (delivered + failed + returned) > 0 ? ((double) delivered) / ((double) (delivered + failed + returned)) * 100.0 : 0.0;
            m.put("totalOrders", totalOrders);
            m.put("delivered", delivered);
            m.put("failed", failed);
            m.put("returnedOrders", returned);
            m.put("inProgress", inProgress < 0 ? 0 : inProgress);
            m.put("successRate", Math.round(successRate * 100.0) / 100.0);
            m.put("shippingRevenue", r[7] == null ? BigDecimal.ZERO : new BigDecimal(r[7].toString()));
            m.put("totalCodCollected", r[8] == null ? BigDecimal.ZERO : new BigDecimal(r[8].toString()));
            m.put("codSubmittedToCompany", r[9] == null ? BigDecimal.ZERO : new BigDecimal(r[9].toString()));
            m.put("totalEmployees", r[10] == null ? 0L : ((Number) r[10]).longValue());
            m.put("totalShippers", r[11] == null ? 0L : ((Number) r[11]).longValue());
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
            long totalOrders = r[4] == null ? 0L : ((Number) r[4]).longValue();
            long delivered = r[5] == null ? 0L : ((Number) r[5]).longValue();
            long failed = r[6] == null ? 0L : ((Number) r[6]).longValue();
            long returned = r[7] == null ? 0L : ((Number) r[7]).longValue();
            long inProgress = r[8] == null ? 0L : ((Number) r[8]).longValue();
            double successRate = (delivered + failed + returned) > 0 ? ((double) delivered) / ((double) (delivered + failed + returned)) * 100.0 : 0.0;
            m.put("totalOrders", totalOrders);
            m.put("delivered", delivered);
            m.put("failed", failed);
            m.put("returnedOrders", returned);
            m.put("inProgress", inProgress);
            m.put("successRate", Math.round(successRate * 100.0) / 100.0);
            m.put("codCollected", r[9] == null ? BigDecimal.ZERO : new BigDecimal(r[9].toString()));
            m.put("codSubmittedToCompany", r[10] == null ? BigDecimal.ZERO : new BigDecimal(r[10].toString()));
            m.put("codHeldByShipper", ((r[9] == null ? BigDecimal.ZERO : new BigDecimal(r[9].toString())).subtract(r[10] == null ? BigDecimal.ZERO : new BigDecimal(r[10].toString()))));
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
            java.sql.Date d = (java.sql.Date) r[0];
            m.put("date", d != null ? d.toLocalDate().toString() : null);
            m.put("shippingRevenue", r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString()));
            m.put("codCollected", r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString()));
            m.put("codSubmittedToCompany", r[3] == null ? BigDecimal.ZERO : new BigDecimal(r[3].toString()));
            m.put("codTransferredToShop", r[4] == null ? BigDecimal.ZERO : new BigDecimal(r[4].toString()));
            m.put("codHeldByCompany", ((r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString())).subtract(r[4] == null ? BigDecimal.ZERO : new BigDecimal(r[4].toString()))));
            return m;
        }).toList();

        List<Object[]> byBranch = reportRepo.financeReportByBranch(start, end);
        List<Map<String, Object>> revenueByBranch = byBranch.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("officeId", r[0] == null ? null : ((Number) r[0]).intValue());
            m.put("officeName", r[1] == null ? "" : r[1].toString());
            m.put("shippingRevenue", r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString()));
            m.put("codCollected", r[3] == null ? BigDecimal.ZERO : new BigDecimal(r[3].toString()));
            m.put("codSubmittedToCompany", r[4] == null ? BigDecimal.ZERO : new BigDecimal(r[4].toString()));
            m.put("codTransferredToShop", r[5] == null ? BigDecimal.ZERO : new BigDecimal(r[5].toString()));
            m.put("codHeldByCompany", ((r[3] == null ? BigDecimal.ZERO : new BigDecimal(r[3].toString())).subtract(r[5] == null ? BigDecimal.ZERO : new BigDecimal(r[5].toString()))));
            return m;
        }).toList();

        BigDecimal totalShippingRevenue = byDay.stream().map(r -> r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCodCollected = byDay.stream().map(r -> r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCodTransferred = byDay.stream().map(r -> r[4] == null ? BigDecimal.ZERO : new BigDecimal(r[4].toString())).reduce(BigDecimal.ZERO, BigDecimal::add);

        out.put("shippingRevenue", totalShippingRevenue);
        out.put("revenueByDay", codByDay.stream().map(d -> { Map<String, Object> m = new HashMap<>(); m.put("date", d.get("date")); m.put("shippingRevenue", d.get("shippingRevenue")); return m; }).toList());
        out.put("revenueByBranch", revenueByBranch);

        Map<String, Object> codSummary = new HashMap<>();
        codSummary.put("totalCodCollected", totalCodCollected);
        codSummary.put("codSubmittedToCompany", byBranch.stream().map(r -> r[4] == null ? BigDecimal.ZERO : new BigDecimal(r[4].toString())).reduce(BigDecimal.ZERO, BigDecimal::add));
        codSummary.put("codTransferredToShop", totalCodTransferred);
        codSummary.put("codHeldByCompany", totalCodCollected.subtract(totalCodTransferred));

        out.put("codSummary", codSummary);
        out.put("codByDay", codByDay);
        out.put("codByBranch", revenueByBranch);

        return out;
    }
}
