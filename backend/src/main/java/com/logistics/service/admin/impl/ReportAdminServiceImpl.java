package com.logistics.service.admin.impl;

import com.logistics.dto.admin.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.repository.ReportRepository;
import com.logistics.service.admin.ReportAdminService;
import com.logistics.utils.ExcelExportHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ReportAdminServiceImpl implements ReportAdminService {

    private final ReportRepository reportRepo;

    public ReportAdminServiceImpl(ReportRepository reportRepo) {
        this.reportRepo = reportRepo;
    }

    @Override
    public List<AdminFinancialPoint> getFinancialByDate(LocalDateTime start, LocalDateTime end) {
        return reportRepo.sumByDate(start, end);
    }

    @Override
    public List<AdminShipperReportDto> getShipperReport(LocalDateTime start, LocalDateTime end) {
        return reportRepo.reportByShipper(start, end);
    }

    @Override
    public List<AdminFinancialPoint> getTransferredByDate(LocalDateTime start, LocalDateTime end) {
        return reportRepo.transferredByDate(start, end);
    }

    @Override
    public List<AdminFinancialPoint> getShippingFeeByDate(LocalDateTime start, LocalDateTime end) {
        return reportRepo.shippingFeeByDate(start, end);
    }

    @Override
    public List<Object[]> getOrderOperationSummary(LocalDateTime start, LocalDateTime end) {
        try {
            return reportRepo.orderOperationSummary(start, end);
        } catch (Exception ex) {
            log.error("[REPORT] getOrderOperationSummary ERROR start={} end={}", start, end, ex);
            throw ex;
        }
    }

    @Override
    public List<AdminOfficeReportDto> getOfficeReport(LocalDateTime start, LocalDateTime end) {
        return reportRepo.reportByOffice(start, end);
    }

    @Override
    public List<AdminShopReportDto> getShopReport(LocalDateTime start, LocalDateTime end) {
        return reportRepo.reportByShop(start, end);
    }

    @Override
    public AdminOverviewDto getOverview(LocalDateTime start, LocalDateTime end) {
        try {
            Object[] r = reportRepo.overviewSummary(start, end);
            if (r == null) {
                r = new Object[10];
            }
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
        } catch (Exception ex) {
            log.error("[REPORT] getOverview ERROR start={} end={}", start, end, ex);
            throw ex;
        }
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

    /* ============================================================
     *  EXPORT EXCEL — đồng nhất theo ExcelExportHelper
     * ============================================================ */

    @Override
    public byte[] exportOperationsXlsx(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = reportRepo.orderOperationSummary(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo vận hành");

            List<String> headers = Arrays.asList(
                    "Ngày", "Tổng đơn", "Giao thành công", "Thất bại", "Trả về", "Tỉ lệ thành công (%)");

            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo vận hành theo ngày",
                    ExcelExportHelper.toLocalDate(start), ExcelExportHelper.toLocalDate(end), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);
            XSSFCellStyle percentStyle = ExcelExportHelper.createPercentStyle(workbook);
            XSSFCellStyle dateStyle = ExcelExportHelper.createDateStyle(workbook);

            for (Object[] r : rows) {
                Row row = sheet.createRow(rowIdx++);
                LocalDate d = ReportRepository.toLocalDate(r[0]);
                long totalOrders = ReportRepository.safeLong(r[1]);
                long delivered = ReportRepository.safeLong(r[2]);
                long failed = ReportRepository.safeLong(r[3]);
                long returning = ReportRepository.safeLong(r[5]);
                long returned = ReportRepository.safeLong(r[6]);
                long returnCount = returning + returned;
                double successRate = totalOrders > 0 ? ((double) delivered / (double) totalOrders) * 100.0 : 0.0;

                if (d != null) {
                    row.createCell(0).setCellValue(d);
                    row.getCell(0).setCellStyle(dateStyle);
                } else {
                    row.createCell(0).setBlank();
                }
                row.createCell(1).setCellValue(totalOrders);
                row.getCell(1).setCellStyle(integerStyle);
                row.createCell(2).setCellValue(delivered);
                row.getCell(2).setCellStyle(integerStyle);
                row.createCell(3).setCellValue(failed);
                row.getCell(3).setCellStyle(integerStyle);
                row.createCell(4).setCellValue(returnCount);
                row.getCell(4).setCellStyle(integerStyle);
                row.createCell(5).setCellValue(Math.round(successRate * 100.0) / 100.0);
                row.getCell(5).setCellStyle(percentStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    @Override
    public byte[] exportOfficeXlsx(LocalDateTime start, LocalDateTime end) {
        List<AdminOfficeReportDto> rows = reportRepo.reportByOffice(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo theo bưu cục");

            List<String> headers = Arrays.asList("Mã bưu cục", "Tên bưu cục", "Tổng đơn");
            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo tổng hợp theo bưu cục",
                    ExcelExportHelper.toLocalDate(start), ExcelExportHelper.toLocalDate(end), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);

            for (AdminOfficeReportDto r : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getOfficeId() == null ? "" : r.getOfficeId().toString());
                row.getCell(0).setCellStyle(textStyle);
                row.createCell(1).setCellValue(r.getOfficeName() == null ? "" : r.getOfficeName());
                row.getCell(1).setCellStyle(textStyle);
                row.createCell(2).setCellValue(r.getTotalOrders() == null ? 0L : r.getTotalOrders());
                row.getCell(2).setCellStyle(integerStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    @Override
    public byte[] exportShopXlsx(LocalDateTime start, LocalDateTime end) {
        List<AdminShopReportDto> rows = reportRepo.reportByShop(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo theo cửa hàng");

            List<String> headers = Arrays.asList(
                    "Mã cửa hàng", "Tên cửa hàng", "Số đơn", "Tổng giá trị đơn (VND)", "Tổng phí vận chuyển (VND)");

            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo tổng hợp theo cửa hàng",
                    ExcelExportHelper.toLocalDate(start), ExcelExportHelper.toLocalDate(end), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);
            XSSFCellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);

            for (AdminShopReportDto r : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getShopId() == null ? "" : r.getShopId().toString());
                row.getCell(0).setCellStyle(textStyle);
                row.createCell(1).setCellValue(r.getShopName() == null ? "" : r.getShopName());
                row.getCell(1).setCellStyle(textStyle);
                row.createCell(2).setCellValue(r.getOrdersCount() == null ? 0L : r.getOrdersCount());
                row.getCell(2).setCellStyle(integerStyle);
                row.createCell(3).setCellValue(r.getTotalOrderValue() == null ? 0.0 : r.getTotalOrderValue().doubleValue());
                row.getCell(3).setCellStyle(currencyStyle);
                row.createCell(4).setCellValue(r.getTotalShippingFee() == null ? 0.0 : r.getTotalShippingFee().doubleValue());
                row.getCell(4).setCellStyle(currencyStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    @Override
    public byte[] exportOverviewXlsx(LocalDateTime start, LocalDateTime end) {
        AdminOverviewDto dto = getOverview(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo tổng quan");

            List<String> headers = Arrays.asList("Chỉ tiêu", "Giá trị");
            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo tổng quan hệ thống",
                    ExcelExportHelper.toLocalDate(start), ExcelExportHelper.toLocalDate(end), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);
            XSSFCellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);
            XSSFCellStyle percentStyle = ExcelExportHelper.createPercentStyle(workbook);

            addOverviewRow(sheet, rowIdx++, "Tổng số bưu cục", dto.getTotalOffices(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Tổng số nhân viên", dto.getTotalEmployees(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Tổng số shipper", dto.getTotalShippers(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Tổng đơn hàng", dto.getTotalOrders(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Giao thành công", dto.getDelivered(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Thất bại", dto.getFailed(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Trả về", dto.getReturnedOrders(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Đang xử lý", dto.getInProgress(), integerStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Tỉ lệ thành công (%)", dto.getSuccessRate(), percentStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Doanh thu vận chuyển (VND)", dto.getShippingRevenue(), currencyStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "Tổng COD thu hộ (VND)", dto.getTotalCodCollected(), currencyStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "COD đã chuyển shop (VND)", dto.getCodTransferred(), currencyStyle, textStyle);
            addOverviewRow(sheet, rowIdx++, "COD công ty đang giữ (VND)", dto.getCodHeld(), currencyStyle, textStyle);

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    private static void addOverviewRow(Sheet sheet, int rowIdx, String label, Object value,
                                       XSSFCellStyle valueStyle, XSSFCellStyle textStyle) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.getCell(0).setCellStyle(textStyle);

        CellRef cellRef = CellRef.of(row, 1, value, valueStyle);
        cellRef.write();
    }

    private static class CellRef {
        private final Row row;
        private final int col;
        private final Object value;
        private final XSSFCellStyle style;

        private CellRef(Row row, int col, Object value, XSSFCellStyle style) {
            this.row = row;
            this.col = col;
            this.value = value;
            this.style = style;
        }

        static CellRef of(Row row, int col, Object value, XSSFCellStyle style) {
            return new CellRef(row, col, value, style);
        }

        void write() {
            org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
            if (value == null) {
                cell.setBlank();
            } else if (value instanceof BigDecimal bd) {
                cell.setCellValue(bd.doubleValue());
            } else if (value instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    @Override
    public byte[] exportOfficesDetailedXlsx(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = getOfficeReportDetailed(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo chi tiết bưu cục");

            List<String> headers = Arrays.asList(
                    "Mã bưu cục", "Tên bưu cục",
                    "Tổng đơn", "Giao thành công", "Thất bại", "Trả về", "Đang xử lý",
                    "Tỉ lệ thành công (%)",
                    "Doanh thu vận chuyển (VND)",
                    "Tổng COD thu hộ (VND)",
                    "COD nộp công ty (VND)",
                    "Tổng nhân viên", "Tổng shipper");

            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo chi tiết bưu cục",
                    ExcelExportHelper.toLocalDate(start), ExcelExportHelper.toLocalDate(end), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);
            XSSFCellStyle percentStyle = ExcelExportHelper.createPercentStyle(workbook);
            XSSFCellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);

            for (Map<String, Object> r : rows) {
                Row row = sheet.createRow(rowIdx++);
                ExcelExportHelper.writeTextCell(row, 0, r.get("officeId") == null ? "" : r.get("officeId").toString(), textStyle);
                ExcelExportHelper.writeTextCell(row, 1, r.get("officeName") == null ? "" : r.get("officeName").toString(), textStyle);
                ExcelExportHelper.writeIntegerCell(row, 2, asLong(r.get("totalOrders")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 3, asLong(r.get("delivered")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 4, asLong(r.get("failed")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 5, asLong(r.get("returnedOrders")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 6, asLong(r.get("inProgress")), integerStyle);
                writePercent(row, 7, r.get("successRate"), percentStyle);
                writeCurrency(row, 8, (BigDecimal) r.getOrDefault("shippingRevenue", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 9, (BigDecimal) r.getOrDefault("totalCodCollected", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 10, (BigDecimal) r.getOrDefault("codSubmittedToCompany", BigDecimal.ZERO), currencyStyle);
                ExcelExportHelper.writeIntegerCell(row, 11, asLong(r.get("totalEmployees")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 12, asLong(r.get("totalShippers")), integerStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    @Override
    public byte[] exportShippersDetailedXlsx(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = getShipperReportDetailed(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo chi tiết shipper");

            List<String> headers = Arrays.asList(
                    "Mã shipper", "Tên shipper", "Số điện thoại", "Tên bưu cục",
                    "Tổng đơn", "Giao thành công", "Thất bại", "Trả về", "Đang xử lý",
                    "Tỉ lệ thành công (%)",
                    "COD đã thu (VND)", "COD đã nộp công ty (VND)", "COD shipper đang giữ (VND)");

            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo chi tiết shipper",
                    ExcelExportHelper.toLocalDate(start), ExcelExportHelper.toLocalDate(end), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);
            XSSFCellStyle percentStyle = ExcelExportHelper.createPercentStyle(workbook);
            XSSFCellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);

            for (Map<String, Object> r : rows) {
                Row row = sheet.createRow(rowIdx++);
                ExcelExportHelper.writeTextCell(row, 0, r.get("shipperId") == null ? "" : r.get("shipperId").toString(), textStyle);
                ExcelExportHelper.writeTextCell(row, 1, r.get("shipperName") == null ? "" : r.get("shipperName").toString(), textStyle);
                ExcelExportHelper.writeTextCell(row, 2, r.get("phone") == null ? "" : r.get("phone").toString(), textStyle);
                ExcelExportHelper.writeTextCell(row, 3, r.get("branchName") == null ? "" : r.get("branchName").toString(), textStyle);
                ExcelExportHelper.writeIntegerCell(row, 4, asLong(r.get("totalOrders")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 5, asLong(r.get("delivered")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 6, asLong(r.get("failed")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 7, asLong(r.get("returnedOrders")), integerStyle);
                ExcelExportHelper.writeIntegerCell(row, 8, asLong(r.get("inProgress")), integerStyle);
                writePercent(row, 9, r.get("successRate"), percentStyle);
                writeCurrency(row, 10, (BigDecimal) r.getOrDefault("codCollected", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 11, (BigDecimal) r.getOrDefault("codSubmittedToCompany", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 12, (BigDecimal) r.getOrDefault("codHeldByShipper", BigDecimal.ZERO), currencyStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] exportFinanceXlsx(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> report = getFinanceReport(start, end);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo tài chính");

            // Sheet 1: Tổng quan tài chính (không còn định dạng key/value thô)
            List<String> summaryHeaders = Arrays.asList("Chỉ tiêu", "Giá trị");
            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo tài chính",
                    ExcelExportHelper.toLocalDate(start), ExcelExportHelper.toLocalDate(end), summaryHeaders.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, summaryHeaders);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);

            Map<String, Object> codSummary = (Map<String, Object>) report.getOrDefault("codSummary", new HashMap<>());
            BigDecimal shippingRevenue = bigDecimalOf(report.get("shippingRevenue"));
            BigDecimal totalCodCollected = bigDecimalOf(codSummary.get("totalCodCollected"));
            BigDecimal codSubmittedToCompany = bigDecimalOf(codSummary.get("codSubmittedToCompany"));
            BigDecimal codTransferredToShop = bigDecimalOf(codSummary.get("codTransferredToShop"));
            BigDecimal codHeldByCompany = bigDecimalOf(codSummary.get("codHeldByCompany"));

            writeKeyValueRow(sheet, rowIdx++, "Tổng doanh thu vận chuyển (VND)", shippingRevenue, textStyle, currencyStyle);
            writeKeyValueRow(sheet, rowIdx++, "Tổng COD thu hộ (VND)", totalCodCollected, textStyle, currencyStyle);
            writeKeyValueRow(sheet, rowIdx++, "COD đã nộp công ty (VND)", codSubmittedToCompany, textStyle, currencyStyle);
            writeKeyValueRow(sheet, rowIdx++, "COD đã chuyển về shop (VND)", codTransferredToShop, textStyle, currencyStyle);
            writeKeyValueRow(sheet, rowIdx++, "COD công ty đang giữ (VND)", codHeldByCompany, textStyle, currencyStyle);

            rowIdx++; // dòng trống

            // Sheet 1 phần dưới: Bảng theo ngày
            List<String> dayHeaders = Arrays.asList(
                    "Ngày",
                    "Doanh thu vận chuyển (VND)",
                    "COD thu hộ (VND)",
                    "COD nộp công ty (VND)",
                    "COD chuyển shop (VND)",
                    "COD công ty đang giữ (VND)");
            rowIdx = ExcelExportHelper.writeHeaderRow(sheet, rowIdx, dayHeaders);

            XSSFCellStyle dateStyle = ExcelExportHelper.createDateStyle(workbook);
            XSSFCellStyle integerDayStyle = ExcelExportHelper.createIntegerStyle(workbook);

            List<Map<String, Object>> codByDay = (List<Map<String, Object>>) report.getOrDefault("codByDay", Collections.emptyList());
            for (Map<String, Object> d : codByDay) {
                Row row = sheet.createRow(rowIdx++);
                LocalDate dateVal = ReportRepository.toLocalDate(d.get("date"));
                if (dateVal != null) {
                    row.createCell(0).setCellValue(dateVal);
                    row.getCell(0).setCellStyle(dateStyle);
                } else {
                    row.createCell(0).setBlank();
                }
                writeCurrency(row, 1, (BigDecimal) d.getOrDefault("shippingRevenue", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 2, (BigDecimal) d.getOrDefault("codCollected", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 3, (BigDecimal) d.getOrDefault("codSubmittedToCompany", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 4, (BigDecimal) d.getOrDefault("codTransferredToShop", BigDecimal.ZERO), currencyStyle);
                writeCurrency(row, 5, (BigDecimal) d.getOrDefault("codHeldByCompany", BigDecimal.ZERO), currencyStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, dayHeaders.size());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal bigDecimalOf(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static void writePercent(Row row, int col, Object value, XSSFCellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value != null) {
            try {
                cell.setCellValue(Double.parseDouble(value.toString()));
            } catch (NumberFormatException ignored) {
                cell.setCellValue(0);
            }
        }
        if (style != null) cell.setCellStyle(style);
    }

    private static void writeCurrency(Row row, int col, BigDecimal value, XSSFCellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        if (style != null) cell.setCellStyle(style);
    }

    private static void writeKeyValueRow(Sheet sheet, int rowIdx, String label, BigDecimal value,
                                         XSSFCellStyle textStyle, XSSFCellStyle valueStyle) {
        Row row = sheet.createRow(rowIdx);
        org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(textStyle);
        if (value != null) {
            org.apache.poi.ss.usermodel.Cell valCell = row.createCell(1);
            valCell.setCellValue(value.doubleValue());
            valCell.setCellStyle(valueStyle);
        } else {
            org.apache.poi.ss.usermodel.Cell valCell = row.createCell(1);
            valCell.setBlank();
            valCell.setCellStyle(valueStyle);
        }
    }

    @Override
    public List<Map<String, Object>> getOfficeReportDetailed(LocalDateTime start, LocalDateTime end) {
        try {
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
        } catch (Exception ex) {
            log.error("[REPORT] getOfficeReportDetailed ERROR start={} end={}", start, end, ex);
            throw ex;
        }
    }

    @Override
    public List<Map<String, Object>> getShipperReportDetailed(LocalDateTime start, LocalDateTime end) {
        try {
            List<Object[]> rows = reportRepo.reportByShipperDetailed(start, end);
            List<Map<String, Object>> out = rows.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                Integer employeeId = r[0] == null ? null : ((Number) r[0]).intValue();
                Integer userId = r[1] == null ? null : ((Number) r[1]).intValue();
                String shipperName = r[2] == null ? "" : r[2].toString();
                String phone = r[3] == null ? "" : r[3].toString();
                Integer officeId = r[4] == null ? null : ((Number) r[4]).intValue();
                String officeName = r[5] == null ? "" : r[5].toString();
                long totalOrders = ReportRepository.safeLong(r[6]);
                long delivered = ReportRepository.safeLong(r[7]);
                long failed = ReportRepository.safeLong(r[8]);
                long returnCount = ReportRepository.safeLong(r[9]);
                long processing = ReportRepository.safeLong(r[10]);
                double successRate = ReportRepository.nzDouble(r[11]);
                BigDecimal codCollected = ReportRepository.safeBigDecimal(r[12]);

                m.put("shipperId", employeeId);
                m.put("userId", userId);
                m.put("shipperName", shipperName);
                m.put("phone", phone);
                m.put("officeId", officeId);
                m.put("branchName", officeName);
                m.put("totalOrders", totalOrders);
                m.put("delivered", delivered);
                m.put("failed", failed);
                m.put("returnedOrders", returnCount);
                m.put("inProgress", processing);
                m.put("successRate", successRate);
                m.put("codCollected", codCollected);
                m.put("codSubmittedToCompany", BigDecimal.ZERO);
                m.put("codHeldByShipper", codCollected);

                return m;
            }).toList();
            return out;
        } catch (Exception ex) {
            log.error("[REPORT] getShipperReportDetailed ERROR start={} end={}", start, end, ex);
            throw ex;
        }
    }

    @Override
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
        BigDecimal totalCodSubmitted = byBranch.stream().map(r -> ReportRepository.safeBigDecimal(r[4])).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCodTransferred = byDay.stream().map(r -> ReportRepository.safeBigDecimal(r[4])).reduce(BigDecimal.ZERO, BigDecimal::add);

        out.put("shippingRevenue", totalShippingRevenue);
        out.put("revenueByDay", codByDay.stream().map(d -> { Map<String, Object> m = new HashMap<>(); m.put("date", d.get("date")); m.put("shippingRevenue", d.get("shippingRevenue")); return m; }).toList());
        out.put("revenueByBranch", revenueByBranch);

        Map<String, Object> codSummary = new HashMap<>();
        codSummary.put("totalCodCollected", totalCodCollected);
        codSummary.put("codSubmittedToCompany", totalCodSubmitted);
        codSummary.put("codTransferredToShop", totalCodTransferred);
        codSummary.put("codHeldByCompany", totalCodCollected.subtract(totalCodTransferred));

        out.put("codSummary", codSummary);
        out.put("codByDay", codByDay);
        out.put("codByBranch", revenueByBranch);

        return out;
    }
}
