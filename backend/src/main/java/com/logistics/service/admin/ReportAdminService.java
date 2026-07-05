package com.logistics.service.admin;

import com.logistics.dto.admin.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportAdminService {
    List<AdminFinancialPoint> getFinancialByDate(LocalDateTime start, LocalDateTime end);
    List<AdminShipperReportDto> getShipperReport(LocalDateTime start, LocalDateTime end);
    List<AdminFinancialPoint> getTransferredByDate(LocalDateTime start, LocalDateTime end);
    List<AdminFinancialPoint> getShippingFeeByDate(LocalDateTime start, LocalDateTime end);
    List<Object[]> getOrderOperationSummary(LocalDateTime start, LocalDateTime end);
    List<AdminOfficeReportDto> getOfficeReport(LocalDateTime start, LocalDateTime end);
    List<AdminShopReportDto> getShopReport(LocalDateTime start, LocalDateTime end);
    AdminOverviewDto getOverview(LocalDateTime start, LocalDateTime end);
    byte[] exportOperationsXlsx(LocalDateTime start, LocalDateTime end);
    byte[] exportOfficeXlsx(LocalDateTime start, LocalDateTime end);
    byte[] exportShopXlsx(LocalDateTime start, LocalDateTime end);
    byte[] exportOverviewXlsx(LocalDateTime start, LocalDateTime end);
    byte[] exportOfficesDetailedXlsx(LocalDateTime start, LocalDateTime end);
    byte[] exportShippersDetailedXlsx(LocalDateTime start, LocalDateTime end);
    byte[] exportFinanceXlsx(LocalDateTime start, LocalDateTime end);
    List<Map<String, Object>> getOfficeReportDetailed(LocalDateTime start, LocalDateTime end);
    List<Map<String, Object>> getShipperReportDetailed(LocalDateTime start, LocalDateTime end);
    Map<String, Object> getFinanceReport(LocalDateTime start, LocalDateTime end);
}
