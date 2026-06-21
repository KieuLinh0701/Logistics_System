package com.logistics.service.admin;

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
import com.logistics.specification.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.logistics.utils.AuditLogUtils.translateAuditLogAction;
import static com.logistics.utils.AuditLogUtils.translateAuditLogStatus;
import static com.logistics.utils.EntityTypeUtils.translateEntityType;

@Service
@RequiredArgsConstructor
public class AuditLogAdminService {

    private final AuditLogRepository repository;

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
            Sheet sheet = workbook.createSheet("Audit Logs");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Thời gian",
                    "Họ tên NV",
                    "SĐT NV",
                    "Tên bưu cục",
                    "Mã code bưu cục",
                    "SĐT bưu cục",
                    "Đối tượng",
                    "Mã ĐT",
                    "Hành động",
                    "Mô tả",
                    "Trạng thái"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (AuditLog log : logs) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : "");
                row.createCell(1).setCellValue(log.getUser() != null ? log.getUser().getFullName() : "");
                row.createCell(2).setCellValue(log.getUser() != null ? log.getUser().getPhoneNumber() : "");
                row.createCell(3).setCellValue(log.getOffice() != null ? log.getOffice().getName() : "");
                row.createCell(4).setCellValue(log.getOffice() != null ? log.getOffice().getCode() : "");
                row.createCell(5).setCellValue(log.getOffice() != null ? log.getOffice().getPhoneNumber() : "");
                row.createCell(6).setCellValue(translateEntityType(log.getEntity()));
                row.createCell(7).setCellValue(log.getId() != null ? log.getId().toString() : "");
                row.createCell(8).setCellValue(translateAuditLogAction(log.getAction()));
                row.createCell(9).setCellValue(log.getDescription() != null ? log.getDescription() : "");
                row.createCell(10).setCellValue(translateAuditLogStatus(log.getStatus()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR, e);
        }
    }
}