package com.logistics.service.manager;

import com.logistics.dto.manager.incidentReport.ManagerIncidentReportDetailDto;
import com.logistics.dto.manager.incidentReport.ManagerIncidentReportListDto;
import com.logistics.entity.IncidentReport;
import com.logistics.entity.Office;
import com.logistics.entity.User;
import com.logistics.enums.IncidentStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.IncidentErrorCode;
import com.logistics.mapper.IncidentReportMapper;
import com.logistics.repository.IncidentReportRepository;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.incidentReport.ManagerIncidentUpdateRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.IncidentReportSpecification;
import com.logistics.utils.IncidentReportUtils;
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
import java.util.ArrayList;
import java.util.List;

import static com.logistics.utils.IncidentReportUtils.*;

@Service
@RequiredArgsConstructor
public class IncidentReportManagerService {

    private final IncidentReportRepository incidentRepository;

    private final EmployeeManagerService employeeManagerService;

    private final NotificationService notificationService;

    public ListResponse<ManagerIncidentReportListDto> list(
            int userId,
            SearchRequest request) {

            int page = request.getPage();
            int limit = request.getLimit();
            String priority = request.getPriority();
            String search = request.getSearch();
            String status = request.getStatus();
            String type = request.getType();
            String sort = request.getSort();
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            Specification<IncidentReport> spec = IncidentReportSpecification.unrestricted()
                    .and(IncidentReportSpecification.officeId(userOffice.getId()))
                    .and(IncidentReportSpecification.managerSearch(search))
                    .and(IncidentReportSpecification.status(status))
                    .and(IncidentReportSpecification.incidentType(type))
                    .and(IncidentReportSpecification.priority(priority))
                    .and(IncidentReportSpecification.createdAtBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("createdAt").descending();
                case "oldest" -> Sort.by("createdAt").ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<IncidentReport> pageData = incidentRepository.findAll(spec, pageable);

            List<ManagerIncidentReportListDto> list = pageData.getContent()
                    .stream()
                    .map(IncidentReportMapper::toListDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerIncidentReportListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public ManagerIncidentReportDetailDto getById(int userId, int id) {
            IncidentReport incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new AppException(IncidentErrorCode.INCIDENT_NOT_FOUND));

            checkPermission(userId, incident);

            return IncidentReportMapper.toDetailDto(incident);
    }

    private void checkPermission(int userId, IncidentReport incident) {
        if (incident == null || incident.getOffice() == null || incident.getOffice().getId() == null) {
            throw new AppException(IncidentErrorCode.INCIDENT_ACCESS_DENIED);
        }

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);
        if (userOffice == null) {
            throw new AppException(IncidentErrorCode.INCIDENT_ACCESS_DENIED);
        }

        if (!incident.getOffice().getId().equals(userOffice.getId())) {
            throw new AppException(IncidentErrorCode.INCIDENT_ACCESS_DENIED);
        }
    }

    public void processing(
            int userId,
            int id,
            ManagerIncidentUpdateRequest request) {

            IncidentReport incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new AppException(IncidentErrorCode.INCIDENT_NOT_FOUND));
            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            System.out.println("status" + request.getStatus());
            System.out.println("resi" + request.getResolution());
            System.out.println("UserId: " + userId);
            System.out.println("Office id: " + userOffice.getId());
            System.out.println("Manager id: " + userOffice.getManager().getUser().getId());

            System.out.println(
                    "Incident office id: " + (incident.getOffice() != null ? incident.getOffice().getId() : "null"));
            checkPermission(userId, incident);

            User user = userOffice.getManager().getUser();

            System.out.println("Incident current status: " + incident.getStatus());
            System.out.println("Requested status: " + request.getStatus());

            validateForm(request);

            IncidentStatus newStatus = IncidentStatus.valueOf(request.getStatus());

            if (!IncidentReportUtils.canManagerChangeStatus(incident.getStatus(),
                    newStatus)
                    && isBlank(request.getStatus())) {
                throw new AppException(IncidentErrorCode.INCIDENT_INVALID_TRANSFER_REQUEST_STATUS);
            }

            incident.setStatus(newStatus);
            incident.setResolution(request.getResolution());
            incident.setHandledAt(LocalDateTime.now());
            incident.setHandler(user);
            incident = incidentRepository.save(incident);

            if (incident.getShipper() != null && incident.getShipper().getId() != null) {
                sendNotification(incident);
            }
    }

    public byte[] export(int userId, SearchRequest request) {
        String priority = request.getPriority();
        String search = request.getSearch();
        String status = request.getStatus();
        String type = request.getType();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<IncidentReport> spec = IncidentReportSpecification.unrestricted()
                .and(IncidentReportSpecification.officeId(userOffice.getId()))
                .and(IncidentReportSpecification.managerSearch(search))
                .and(IncidentReportSpecification.status(status))
                .and(IncidentReportSpecification.incidentType(type))
                .and(IncidentReportSpecification.priority(priority))
                .and(IncidentReportSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<IncidentReport> incidents = incidentRepository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Incidents");

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
                    "Mã sự cố",
                    "Độ ưu tiên",
                    "Loại sự cố",
                    "Trạng thái",
                    "Tiêu đề",
                    "Mã đơn hàng",
                    "Thời gian báo cáo",
                    "Thời gian cập nhật",
                    "Thời gian xử lý"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (IncidentReport ir : incidents) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(ir.getCode() != null ? ir.getCode() : "");
                row.createCell(1).setCellValue(translateIncidentPriority(ir.getPriority()));
                row.createCell(2).setCellValue(translateIncidentType(ir.getIncidentType()));
                row.createCell(3).setCellValue(translateIncidentStatus(ir.getStatus()));
                row.createCell(4).setCellValue(ir.getTitle() != null ? ir.getTitle() : "");
                row.createCell(5).setCellValue(
                        ir.getOrder() != null && ir.getOrder().getTrackingNumber() != null
                                ? ir.getOrder().getTrackingNumber() : "N/A");
                row.createCell(6).setCellValue(ir.getCreatedAt() != null ? ir.getCreatedAt().format(dtf) : "");
                row.createCell(7).setCellValue(ir.getUpdatedAt() != null ? ir.getUpdatedAt().format(dtf) : "N/A");
                row.createCell(8).setCellValue(ir.getHandledAt() != null ? ir.getHandledAt().format(dtf) : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    private void sendNotification(IncidentReport incident) {
        Integer userId = incident.getShipper().getId();

        String title;
        String message;

        switch (incident.getStatus()) {
            case PROCESSING -> {
                title = "Yêu cầu đang xử lý";
                message = "Yêu cầu mã " + incident.getCode() + " đang được bưu cục xử lý.";
            }
            case RESOLVED -> {
                title = "Yêu cầu đã xử lý xong";
                message = "Yêu cầu mã " + incident.getCode() + " đã được bưu cục xử lý thành công.";
            }
            case REJECTED -> {
                title = "Yêu cầu bị từ chối";
                message = "Yêu cầu mã " + incident.getCode() + " đã bị bưu cục từ chối.";
            }
            default -> {
                title = "Cập nhật yêu cầu";
                message = "Yêu cầu mã " + incident.getCode() + " đã được cập nhật trạng thái: "
                        + incident.getStatus();
            }
        }

        notificationService.create(
                title,
                message,
                "incident_report",
                userId,
                null,
                "incidents",
                incident.getCode());
    }

    private void validateForm(ManagerIncidentUpdateRequest request) {
        List<String> missing = new ArrayList<>();

        if (isBlank(request.getStatus())) {
            missing.add("Trạng thái");
        }

        if (!missing.isEmpty()) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", missing));
        }

        IncidentStatus status;
        try {
            status = IncidentStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new AppException(IncidentErrorCode.INCIDENT_INVALID_STATUS);
        }

        if (!isBlank(request.getResolution()) && request.getResolution().length() > 1000) {
            throw new AppException(IncidentErrorCode.INCIDENT_INVALID_RESPONSE);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

}