package com.logistics.service.admin;

import com.logistics.dto.admin.AdminFinancialPoint;
import com.logistics.dto.admin.AdminShipperReportDto;
import com.logistics.dto.admin.AdminOfficeReportDto;
import com.logistics.dto.admin.AdminShopReportDto;
import com.logistics.repository.ReportRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
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
            throw new RuntimeException("Export operations xlsx failed: " + e.getMessage(), e);
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
            throw new RuntimeException("Export office xlsx failed: " + e.getMessage(), e);
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
            throw new RuntimeException("Export shop xlsx failed: " + e.getMessage(), e);
        }
    }

    
}
