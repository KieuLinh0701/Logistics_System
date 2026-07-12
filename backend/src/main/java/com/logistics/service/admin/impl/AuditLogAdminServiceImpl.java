package com.logistics.service.admin.impl;

import com.logistics.dto.admin.AdminAuditLogDto;
import com.logistics.entity.AuditLog;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.mapper.AuditLogMapper;
import com.logistics.repository.AuditLogRepository;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.admin.AuditLogAdminService;
import com.logistics.specification.AuditLogSpecification;
import com.logistics.utils.ExcelExportHelper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.logistics.utils.AuditLogUtils.translateAuditLogAction;
import static com.logistics.utils.AuditLogUtils.translateAuditLogStatus;
import static com.logistics.utils.EntityTypeUtils.translateEntityType;

@Service
@RequiredArgsConstructor
public class AuditLogAdminServiceImpl implements AuditLogAdminService {

    private final AuditLogRepository repository;

    @Override
    public ListResponse<AdminAuditLogDto> list(AuditLogSearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        AuditLogStatus status = request.getStatus();
        EntityType entity = request.getEntity();
        String sort = request.getSort();
        AuditLogAction action = request.getAction();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Specification<AuditLog> spec = AuditLogSpecification.unrestricted()
                .and(AuditLogSpecification.searchAdmin(search))
                .and(AuditLogSpecification.status(status))
                .and(AuditLogSpecification.entityType(entity))
                .and(AuditLogSpecification.action(action))
                .and(AuditLogSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<AuditLog> pageData = repository.findAll(spec, pageable);

        List<AdminAuditLogDto> list = AuditLogMapper.toAdminAuditLogDtoList(pageData.getContent());

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<AdminAuditLogDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    @Override
    public byte[] export(AuditLogSearchRequest request) {

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Specification<AuditLog> spec = AuditLogSpecification.unrestricted()
                .and(AuditLogSpecification.searchAdmin(request.getSearch()))
                .and(AuditLogSpecification.status(request.getStatus()))
                .and(AuditLogSpecification.entityType(request.getEntity()))
                .and(AuditLogSpecification.action(request.getAction()))
                .and(AuditLogSpecification.createdAtBetween(startDate, endDate));

        List<AuditLog> logs = repository.findAll(spec, Sort.by("createdAt").descending());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo nhật ký hệ thống");

            List<String> headers = Arrays.asList(
                    "Thời gian",
                    "Họ tên NV",
                    "SĐT NV",
                    "Tên bưu cục",
                    "Mã bưu cục",
                    "SĐT bưu cục",
                    "Đối tượng",
                    "Mã đối tượng",
                    "Hành động",
                    "Mô tả",
                    "Trạng thái");

            ExcelExportHelper.writeTitleAndPeriod(sheet, "Báo cáo lịch sử hoạt động của hệ thống",
                    java.time.LocalDate.now(), java.time.LocalDate.now(), headers.size() - 1);
            int rowIdx = ExcelExportHelper.writeHeaderRow(sheet, 2, headers);

            XSSFCellStyle textStyle = ExcelExportHelper.createTextStyle(workbook);
            XSSFCellStyle dateTimeStyle = ExcelExportHelper.createDateStyle(workbook);
            dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("HH:mm:ss dd/MM/yyyy"));

            for (AuditLog log : logs) {
                Row row = sheet.createRow(rowIdx++);

                org.apache.poi.ss.usermodel.Cell dateCell = row.createCell(0);
                if (log.getCreatedAt() != null) {
                    dateCell.setCellValue(log.getCreatedAt());
                    dateCell.setCellStyle(dateTimeStyle);
                } else {
                    dateCell.setBlank();
                }

                ExcelExportHelper.writeTextCell(row, 1, log.getUser() != null ? log.getUser().getFullName() : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 2, log.getUser() != null ? log.getUser().getPhoneNumber() : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 3, log.getOffice() != null ? log.getOffice().getName() : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 4, log.getOffice() != null ? log.getOffice().getCode() : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 5, log.getOffice() != null ? log.getOffice().getPhoneNumber() : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 6, translateEntityType(log.getEntity()), textStyle);
                ExcelExportHelper.writeTextCell(row, 7, log.getId() != null ? log.getId().toString() : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 8, translateAuditLogAction(log.getAction()), textStyle);
                ExcelExportHelper.writeTextCell(row, 9, log.getDescription() != null ? log.getDescription() : "", textStyle);
                ExcelExportHelper.writeTextCell(row, 10, translateAuditLogStatus(log.getStatus()), textStyle);
            }

            ExcelExportHelper.autoSizeAllColumns(sheet, headers.size());

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR, e);
        }
    }
}
