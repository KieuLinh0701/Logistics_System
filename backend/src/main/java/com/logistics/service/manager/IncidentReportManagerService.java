package com.logistics.service.manager;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dto.manager.incidentReport.ManagerIncidentReportDetailDto;
import com.logistics.dto.manager.incidentReport.ManagerIncidentReportListDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.entity.Address;
import com.logistics.entity.IncidentReport;
import com.logistics.entity.Office;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.ShippingRequestAttachment;
import com.logistics.entity.User;
import com.logistics.enums.IncidentStatus;
import com.logistics.enums.ShippingRequestAttachmentType;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.mapper.IncidentReportMapper;
import com.logistics.mapper.ShippingRequestMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.IncidentReportRepository;
import com.logistics.repository.ShippingRequestAttachmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.incidentReport.ManagerIncidentUpdateRequest;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestForm;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.IncidentReportSpecification;
import com.logistics.utils.IncidentReportUtils;
import com.logistics.utils.ShippingRequestUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentReportManagerService {

    private final IncidentReportRepository incidentRepository;

    private final AddressRepository addressRepository;

    private final Cloudinary cloudinary;

    private final ShippingRequestAttachmentRepository shippingRequestAttachmentRepository;

    private final EmployeeManagerService employeeManagerService;

    private final NotificationService notificationService;

    public ApiResponse<ListResponse<ManagerIncidentReportListDto>> list(int userId,
            SearchRequest request) {
        try {
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

            return new ApiResponse<>(true, "Lấy danh sách sự cố thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ManagerIncidentReportDetailDto> getById(int userId, int id) {
        try {
            IncidentReport incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố"));

            if (!checkPermission(userId, incident)) {
                return new ApiResponse<>(false, "Không có quyền xem sự cố này", null);
            }

            ManagerIncidentReportDetailDto data = IncidentReportMapper.toDetailDto(incident);

            return new ApiResponse<>(true, "Lấy chi tiết báo cáo thành công", data);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private boolean checkPermission(int userId, IncidentReport incident) {
        if (incident == null) {
            return false;
        }

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (userOffice == null) {
            return false;
        }

        if (incident.getOffice() == null || incident.getOffice().getId() == null) {
            return false;
        }

        return incident.getOffice().getId().equals(userOffice.getId());
    }

    public ApiResponse<Boolean> processing(int userId, int id,
            ManagerIncidentUpdateRequest request) {

        try {
            IncidentReport incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Báo cáo sự cố không tồn tại"));
            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            System.out.println("status" + request.getStatus());
            System.out.println("resi" + request.getResolution());
            System.out.println("UserId: " + userId);
            System.out.println("Office id: " + userOffice.getId());
            System.out.println("Manager id: " + userOffice.getManager().getUser().getId());

            System.out.println(
                    "Incident office id: " + (incident.getOffice() != null ? incident.getOffice().getId() : "null"));
            if (!checkPermission(userId, incident)) {
                return new ApiResponse<>(false, "Không có quyền xử lý báo cáo này", null);
            }

            User user = userOffice.getManager().getUser();

            System.out.println("Incident current status: " + incident.getStatus());
            System.out.println("Requested status: " + request.getStatus());

            validateForm(request);

            IncidentStatus newStatus = IncidentStatus.valueOf(request.getStatus());

            if (!IncidentReportUtils.canManagerChangeStatus(incident.getStatus(),
                    newStatus)
                    && isBlank(request.getStatus())) {
                throw new RuntimeException("Trạng thái yêu cầu chuyển không hợp lệ");
            }

            incident.setStatus(newStatus);
            incident.setResolution(request.getResolution());
            incident.setHandledAt(LocalDateTime.now());
            incident.setHandler(user);
            incident = incidentRepository.save(incident);

            if (incident.getShipper() != null && incident.getShipper().getId() != null) {
                sendNotification(incident);
            }

            return new ApiResponse<>(true, "Phản hồi yêu cầu thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
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
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));
        }

        IncidentStatus status;
        try {
            status = IncidentStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new RuntimeException("Trạng thái báo cáo không hợp lệ");
        }

        if (!isBlank(request.getResolution()) && request.getResolution().length() > 1000) {
            throw new RuntimeException("Nội dung xử lý không được vượt quá 1000 ký tự");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

}