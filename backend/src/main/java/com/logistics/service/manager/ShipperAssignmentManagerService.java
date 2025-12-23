package com.logistics.service.manager;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.manager.shipperAssignment.ManagerShipperAssignmentListDto;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.ShipperAssignment;
import com.logistics.entity.User;
import com.logistics.mapper.ShipperAssignmentMapper;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.ShipperAssignmentRepository;
import com.logistics.request.manager.shipperAssignment.ManagerShipperAssignmentEditRequest;
import com.logistics.request.manager.shipperAssignment.ManagerShipperAssignmentSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.utils.LocationUtils;

import ch.qos.logback.core.util.LocationUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipperAssignmentManagerService {

    private final EmployeeManagerService employeeManagerService;

    private final NotificationService notificationService;

    private final ShipperAssignmentRepository shipperAssignmentRepository;

    private final EmployeeRepository employeeRepository;

    @Transactional
    public ApiResponse<Boolean> create(
            int userId,
            ManagerShipperAssignmentEditRequest request) {

        try {
            validate(request, false);

            // 1. Lấy bưu cục của manager
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);
            if (office == null) {
                return new ApiResponse<>(false,
                        "Không xác định được bưu cục của bạn!",
                        false);
            }

            // 2. Lấy employee được chọn
            Employee employee = employeeRepository.findById(request.getSelectedEmployee())
                    .orElseThrow(() -> new RuntimeException("Nhân viên được chọn không tồn tại!"));

            // 3. Check employee thuộc bưu cục
            if (!office.getId().equals(employee.getOffice().getId())) {
                return new ApiResponse<>(false,
                        "Nhân viên được chọn không thuộc bưu cục của bạn!",
                        false);
            }

            // 4. Check role shipper
            if (employee.getAccountRole() == null
                    || employee.getAccountRole().getRole() == null
                    || !"shipper".equalsIgnoreCase(
                            employee.getAccountRole().getRole().getName())) {
                return new ApiResponse<>(false,
                        "Nhân viên được chọn không phải là nhân viên giao hàng!",
                        false);
            }

            LocalDateTime startAt = request.getStartAt();
            LocalDateTime endAt = request.getEndAt();

            // 5. CHECK TRÙNG
            boolean existed = shipperAssignmentRepository.existsActiveOverlap(
                    employee.getUser().getId(),
                    office.getCityCode(),
                    request.getWardCode(),
                    startAt,
                    endAt,
                    null);

            if (existed) {
                return new ApiResponse<>(false,
                        "Nhân viên giao hàng đã được phân công khu vực này trong khoảng thời gian đã chọn!",
                        false);
            }

            // 6. Tạo phân công
            ShipperAssignment assignment = new ShipperAssignment();
            assignment.setShipper(employee.getUser());
            assignment.setCityCode(office.getCityCode());
            assignment.setWardCode(request.getWardCode());
            assignment.setStartAt(startAt);
            assignment.setEndAt(endAt);
            assignment.setNotes(request.getNotes());

            shipperAssignmentRepository.save(assignment);

            // 7. Gửi thông báo
            if (employee.getUser() != null) {
                notificationService.create(
                        "Phân công giao hàng mới",
                        "Bạn đã được phân công phụ trách một khu vực giao hàng mới.",
                        "shipper_assignment",
                        employee.getUser().getId(),
                        null,
                        "shipper_assignment",
                        null);
            }

            return new ApiResponse<>(true,
                    "Tạo phân công giao hàng thành công!",
                    true);

        } catch (Exception ex) {
            return new ApiResponse<>(false,
                    ex.getMessage(),
                    false);
        }
    }

    @Transactional
    public ApiResponse<Boolean> update(
            int userId,
            Long assignmentId,
            ManagerShipperAssignmentEditRequest request) {

        try {
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);
            if (office == null) {
                return new ApiResponse<>(false, "Không xác định được bưu cục của bạn!", false);
            }

            ShipperAssignment assignment = shipperAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Phân công không tồn tại!"));

            LocalDateTime now = LocalDateTime.now();
            boolean isStarted = !assignment.getStartAt().isAfter(now);
            validate(request, isStarted);

            if (assignment.getEndAt() != null && assignment.getEndAt().isBefore(now)) {
                return new ApiResponse<>(false,
                        "Phân công đã kết thúc, không thể chỉnh sửa!",
                        false);
            }

            Employee oldEmployee = assignment.getShipper().getEmployees().stream()
                    .filter(e -> e.getAccountRole() != null
                            && e.getAccountRole().getRole() != null
                            && "Shipper".equalsIgnoreCase(e.getAccountRole().getRole().getName()))
                    .findFirst().orElse(null);

            if (isStarted) {
                // Nếu đã bắt đầu, chỉ update endAt và notes
                if (request.getEndAt() == null || request.getEndAt().isBefore(now)) {
                    return new ApiResponse<>(false,
                            "Thời gian kết thúc phải >= thời điểm hiện tại!",
                            false);
                }
                assignment.setEndAt(request.getEndAt());
                assignment.setNotes(request.getNotes());
            } else {
                // Nếu chưa bắt đầu, có thể đổi nhân viên, thời gian, khu vực
                Employee employee = employeeRepository.findById(request.getSelectedEmployee())
                        .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại!"));

                if (!office.getId().equals(employee.getOffice().getId())) {
                    return new ApiResponse<>(false,
                            "Nhân viên không thuộc bưu cục của bạn!",
                            false);
                }

                boolean existed = shipperAssignmentRepository.existsActiveOverlap(
                        employee.getUser().getId(),
                        office.getCityCode(),
                        request.getWardCode(),
                        request.getStartAt(),
                        request.getEndAt(),
                        assignment.getId());

                if (existed) {
                    return new ApiResponse<>(false,
                            "Nhân viên này đã có phân công giao hàng trùng thời gian tại khu vực này!",
                            false);
                }

                if (request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
                    return new ApiResponse<>(false,
                            "Thời gian kết thúc phải sau thời gian bắt đầu!",
                            false);
                }

                assignment.setShipper(employee.getUser());
                assignment.setCityCode(office.getCityCode());
                assignment.setWardCode(request.getWardCode());
                assignment.setStartAt(request.getStartAt());
                assignment.setEndAt(request.getEndAt());
                assignment.setNotes(request.getNotes());
            }

            shipperAssignmentRepository.save(assignment);

            // Gửi thông báo
            if (!isStarted && oldEmployee != null
                    && !oldEmployee.getUser().getId().equals(assignment.getShipper().getId())) {
                // Thông báo cho nhân viên cũ nếu đổi người
                notificationService.create(
                        "Phân công giao hàng bị thay đổi",
                        "Phân công giao hàng của bạn đã bị thay đổi sang nhân viên khác.",
                        "shipper_assignment",
                        oldEmployee.getUser().getId(),
                        null,
                        "shipper_assignment",
                        null);
            }

            // Thông báo cho nhân viên hiện tại
            notificationService.create(
                    "Cập nhật phân công giao hàng",
                    "Phân công giao hàng của bạn đã được cập nhật.",
                    "shipper_assignment",
                    assignment.getShipper().getId(),
                    null,
                    "shipper_assignment",
                    null);

            return new ApiResponse<>(true,
                    "Cập nhật phân công giao hàng thành công!",
                    true);

        } catch (Exception ex) {
            return new ApiResponse<>(false, ex.getMessage(), false);
        }
    }

    @Transactional
    public ApiResponse<Boolean> deleteFutureAssignment(int userId, Long assignmentId) {
        try {
            ShipperAssignment assignment = shipperAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Phân công không tồn tại!"));

            LocalDateTime now = LocalDateTime.now();
            if (!assignment.getStartAt().isAfter(now)) {
                return new ApiResponse<>(false,
                        "Phân công đã bắt đầu hoặc đã kết thúc, không thể xóa!",
                        false);
            }

            Office office = employeeManagerService.getManagedOfficeByUserId(userId);

            Optional<Employee> shipperEmployeeOpt = assignment.getShipper().getEmployees().stream()
                    .filter(e -> e.getAccountRole() != null
                            && e.getAccountRole().getRole() != null
                            && "Shipper".equalsIgnoreCase(e.getAccountRole().getRole().getName()))
                    .findFirst();

            if (shipperEmployeeOpt.isEmpty()) {
                return new ApiResponse<>(false,
                        "Nhân viên liên quan không phải là nhân viên giao hàng!",
                        false);
            }

            Employee shipperEmployee = shipperEmployeeOpt.get();
            if (!office.getId().equals(shipperEmployee.getOffice().getId())) {
                return new ApiResponse<>(false,
                        "Bạn không có quyền xóa phân công của nhân viên này!",
                        false);
            }

            shipperAssignmentRepository.delete(assignment);

            // Gửi thông báo
            notificationService.create(
                    "Phân công giao hàng bị hủy",
                    "Phân công giao hàng của bạn đã bị hủy.",
                    "shipper_assignment",
                    shipperEmployee.getUser().getId(),
                    null,
                    "shipper_assignment",
                    null);

            return new ApiResponse<>(true,
                    "Xóa phân công thành công!",
                    true);

        } catch (Exception ex) {
            return new ApiResponse<>(false, ex.getMessage(), false);
        }
    }

    private void validate(ManagerShipperAssignmentEditRequest request, boolean isStarted) {
        List<String> missingFields = new ArrayList<>();

        if (!isStarted) {
            if (request.getSelectedEmployee() == null)
                missingFields.add("Nhân viên giao hàng");
            if (request.getWardCode() == null)
                missingFields.add("Phường/Xã");
            if (request.getStartAt() == null)
                missingFields.add("Thời gian bắt đầu");
        } else {
            if (request.getEndAt() == null)
                missingFields.add("Thời gian kết thúc");
        }

        if (!missingFields.isEmpty()) {
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missingFields));
        }

        LocalDateTime now = LocalDateTime.now();

        if (!isStarted && request.getSelectedEmployee() != null && request.getSelectedEmployee() <= 0) {
            throw new RuntimeException("ID nhân viên phải lớn hơn 0");
        }

        if (!isStarted && request.getWardCode() != null && request.getWardCode() <= 0) {
            throw new RuntimeException("Mã phường/xã phải lớn hơn 0");
        }

        if (!isStarted && request.getStartAt() != null && request.getStartAt().isBefore(now)) {
            throw new RuntimeException("Thời gian bắt đầu phải lớn hơn hoặc bằng thời điểm hiện tại");
        }

        if (request.getEndAt() != null) {
            if (!isStarted && request.getStartAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
                throw new RuntimeException("Thời gian kết thúc phải lớn hơn hoặc bằng thời gian bắt đầu");
            }
            if (request.getEndAt().isBefore(now)) {
                throw new RuntimeException("Thời gian kết thúc phải lớn hơn thời điểm hiện tại");
            }
        }
    }

    public ApiResponse<ListResponse<ManagerShipperAssignmentListDto>> list(
            Integer userId,
            ManagerShipperAssignmentSearchRequest request) {

        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String sort = request.getSort();

            Specification<ShipperAssignment> spec = buildSpecification(userId, request);

            Sort sortOpt;
            switch (sort != null ? sort.toLowerCase() : "") {
                case "newest":
                    sortOpt = Sort.by(Sort.Order.desc("createdAt"));
                    break;
                case "oldest":
                    sortOpt = Sort.by(Sort.Order.asc("createdAt"));
                    break;
                default:
                    sortOpt = Sort.unsorted();
            }

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<ShipperAssignment> pageData = shipperAssignmentRepository.findAll(spec, pageable);

            List<ManagerShipperAssignmentListDto> list = ShipperAssignmentMapper.toListDto(pageData.getContent());

            int total = (int) pageData.getTotalElements();
            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerShipperAssignmentListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách phân công thành công", data);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public byte[] exportShipperAssignmentsExcel(Integer userId, ManagerShipperAssignmentSearchRequest request) {

        List<ShipperAssignment> assignments = shipperAssignmentRepository.findAll(
                buildSpecification(userId, request));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ShipperAssignments");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();

            Font font = workbook.createFont();
            font.setBold(true);

            byte[] fontRgb = new byte[] {
                (byte) 0xFF, // Red
                (byte) 0xFF, // Green
                (byte) 0xFF  // Blue
            };
            ((org.apache.poi.xssf.usermodel.XSSFFont) font).setColor(new XSSFColor(fontRgb, null));

            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            byte[] bgRgb = new byte[] {
                    (byte) 0x1C,
                    (byte) 0x3D,
                    (byte) 0x90
            };
            headerStyle.setFillForegroundColor(new XSSFColor(bgRgb, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] headers = { "Mã NV", "Họ Tên", "Email", "SĐT", "Xã/Phường", "Tỉnh/Thành phố", "Thời gian bắt đầu",
                    "Thời gian kết thúc",
                    "Ghi chú", "Ngày tạo" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            int rowIdx = 1;
            for (ShipperAssignment sa : assignments) {
                Row row = sheet.createRow(rowIdx++);
                ManagerShipperAssignmentListDto dto = ShipperAssignmentMapper.toListDto(sa);

                row.createCell(0).setCellValue(dto.getEmployee() != null ? dto.getEmployee().getCode() : "");
                row.createCell(1)
                        .setCellValue(dto.getEmployee() != null
                                ? dto.getEmployee().getLastName() + " " + dto.getEmployee().getFirstName()
                                : "");
                row.createCell(2).setCellValue(dto.getEmployee() != null ? dto.getEmployee().getEmail() : "");
                row.createCell(3).setCellValue(dto.getEmployee() != null ? dto.getEmployee().getPhoneNumber() : "");

                // Sử dụng LocationUtils để lấy tên
                row.createCell(4).setCellValue(LocationUtils.getWardNameByCode(dto.getCityCode(), dto.getWardCode()));
                row.createCell(5).setCellValue(LocationUtils.getCityNameByCode(dto.getCityCode()));

                row.createCell(6).setCellValue(dto.getStartAt() != null ? dtf.format(dto.getStartAt()) : "");
                row.createCell(7).setCellValue(dto.getEndAt() != null ? dtf.format(dto.getEndAt()) : "");
                row.createCell(8).setCellValue(dto.getNotes() != null ? dto.getNotes() : "");
                row.createCell(9).setCellValue(dto.getCreatedAt() != null ? dtf.format(dto.getCreatedAt()) : "");
            }

            // Auto-size cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất Excel", e);
        }
    }

    private Specification<ShipperAssignment> buildSpecification(Integer userId,
            ManagerShipperAssignmentSearchRequest request) {
        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        String search = request.getSearch();
        Integer wardCode = request.getWardCode();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? Instant.parse(request.getStartDate()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? Instant.parse(request.getEndDate()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;

        return (root, query, cb) -> {
            Join<ShipperAssignment, User> userJoin = root.join("shipper", JoinType.INNER);
            Join<User, Employee> empJoin = userJoin.join("employees", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy employee thuộc office
            predicates.add(cb.equal(empJoin.get("office").get("id"), office.getId()));

            // Lọc wardCode nếu có
            if (wardCode != null) {
                predicates.add(cb.equal(root.get("wardCode"), wardCode));
            }

            // Lọc theo search
            if (search != null && !search.isBlank()) {
                String likeSearch = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(empJoin.get("code")), likeSearch),
                        cb.like(cb.lower(userJoin.get("lastName")), likeSearch),
                        cb.like(cb.lower(userJoin.get("firstName")), likeSearch),
                        cb.like(cb.lower(userJoin.get("phoneNumber")), likeSearch),
                        cb.like(cb.lower(userJoin.get("account").get("email")), likeSearch)));
            }

            // Lọc theo date range
            if (startDate != null && endDate != null) {
                predicates.add(cb.and(
                        cb.lessThanOrEqualTo(root.get("startAt"), endDate),
                        cb.or(cb.isNull(root.get("endAt")),
                                cb.greaterThanOrEqualTo(root.get("endAt"), startDate))));
            } else if (startDate != null) {
                predicates.add(cb.or(cb.isNull(root.get("endAt")),
                        cb.greaterThanOrEqualTo(root.get("endAt"), startDate)));
            } else if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), endDate));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}