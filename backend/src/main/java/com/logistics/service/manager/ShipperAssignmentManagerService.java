package com.logistics.service.manager;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.OfficeErrorCode;
import com.logistics.exception.enums.ShipperAssignmentErrorCode;
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
    public void create(
            int userId,
            ManagerShipperAssignmentEditRequest request) {
            validate(request, false);

            // 1. Lấy bưu cục của manager
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);
            if (office == null) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_USER_OFFICE_MISSING);
            }

            // 2. Lấy employee được chọn
            Employee employee = employeeRepository.findById(request.getSelectedEmployee())
                    .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

            // 3. Check employee thuộc bưu cục
            if (!office.getId().equals(employee.getOffice().getId())) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_OFFICE_MISMATCH);
            }

            // 4. Check role shipper
            if (employee.getAccountRole() == null
                    || employee.getAccountRole().getRole() == null
                    || !"shipper".equalsIgnoreCase(
                            employee.getAccountRole().getRole().getName())) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_SHIPPER_INVALID);
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
                throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_CONFLICT);
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
    }

    @Transactional
    public void update(
            int userId,
            Long assignmentId,
            ManagerShipperAssignmentEditRequest request) {
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);
            if (office == null) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_USER_OFFICE_MISSING);
            }

            ShipperAssignment assignment = shipperAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_NOT_FOUND));

            LocalDateTime now = LocalDateTime.now();
            boolean isStarted = !assignment.getStartAt().isAfter(now);
            validate(request, isStarted);

            if (assignment.getEndAt() != null && assignment.getEndAt().isBefore(now)) {
                throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_CLOSED);
            }

            Employee oldEmployee = assignment.getShipper().getEmployees().stream()
                    .filter(e -> e.getAccountRole() != null
                            && e.getAccountRole().getRole() != null
                            && "Shipper".equalsIgnoreCase(e.getAccountRole().getRole().getName()))
                    .findFirst().orElse(null);

            if (isStarted) {
                // Nếu đã bắt đầu, chỉ update endAt và notes
                if (request.getEndAt() == null || request.getEndAt().isBefore(now)) {
                    throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_END_TIME_INVALID);
                }
                assignment.setEndAt(request.getEndAt());
                assignment.setNotes(request.getNotes());
            } else {
                // Nếu chưa bắt đầu, có thể đổi nhân viên, thời gian, khu vực
                Employee employee = employeeRepository.findById(request.getSelectedEmployee())
                        .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

                if (!office.getId().equals(employee.getOffice().getId())) {
                    throw new AppException(EmployeeErrorCode.EMPLOYEE_OFFICE_MISMATCH);
                }

                boolean existed = shipperAssignmentRepository.existsActiveOverlap(
                        employee.getUser().getId(),
                        office.getCityCode(),
                        request.getWardCode(),
                        request.getStartAt(),
                        request.getEndAt(),
                        assignment.getId());

                if (existed) {
                    throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_CONFLICT);
                }

                if (request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
                    throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_INVALID_TIME_RANGE);
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
    }

    @Transactional
    public void deleteFutureAssignment(int userId, Long assignmentId) {
            ShipperAssignment assignment = shipperAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_NOT_FOUND));

            LocalDateTime now = LocalDateTime.now();
            if (!assignment.getStartAt().isAfter(now)) {
                throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_DELETION_FORBIDDEN);
            }

            Office office = employeeManagerService.getManagedOfficeByUserId(userId);

            Optional<Employee> shipperEmployeeOpt = assignment.getShipper().getEmployees().stream()
                    .filter(e -> e.getAccountRole() != null
                            && e.getAccountRole().getRole() != null
                            && "Shipper".equalsIgnoreCase(e.getAccountRole().getRole().getName()))
                    .findFirst();

            if (shipperEmployeeOpt.isEmpty()) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_SHIPPER_INVALID);
            }

            Employee shipperEmployee = shipperEmployeeOpt.get();
            if (!office.getId().equals(shipperEmployee.getOffice().getId())) {
                throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_CANNOT_DELETED);
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
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", missingFields));
        }

        LocalDateTime now = LocalDateTime.now();

        if (!isStarted && request.getSelectedEmployee() != null && request.getSelectedEmployee() <= 0) {
            throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_ID_INVALID);
        }

        if (!isStarted && request.getWardCode() != null && request.getWardCode() <= 0) {
            throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_WARD_CODE_INVALID);
        }

        if (!isStarted && request.getStartAt() != null && request.getStartAt().isBefore(now)) {
            throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_START_TIME_INVALID);
        }

        if (request.getEndAt() != null) {
            if (!isStarted && request.getStartAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
                throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_INVALID_TIME_RANGE);
            }
            if (request.getEndAt().isBefore(now)) {
                throw new AppException(ShipperAssignmentErrorCode.SHIPPER_ASSIGNMENT_END_TIME_INVALID);
            }
        }
    }

    public ListResponse<ManagerShipperAssignmentListDto> list(
            Integer userId,
            ManagerShipperAssignmentSearchRequest request) {
            int page = request.getPage();
            int limit = request.getLimit();
            String sort = request.getSort();

            Specification<ShipperAssignment> spec = buildSpecification(userId, request);

            Sort sortOpt = switch (sort != null ? sort.toLowerCase() : "") {
                case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
                case "oldest" -> Sort.by(Sort.Order.asc("createdAt"));
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<ShipperAssignment> pageData = shipperAssignmentRepository.findAll(spec, pageable);

            List<ManagerShipperAssignmentListDto> list = ShipperAssignmentMapper.toListDto(pageData.getContent());

            int total = (int) pageData.getTotalElements();
            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerShipperAssignmentListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
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
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
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