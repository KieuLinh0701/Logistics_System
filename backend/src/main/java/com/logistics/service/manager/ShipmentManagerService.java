package com.logistics.service.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.dto.manager.shipment.ManagerShipmentPerformanceDto;
import com.logistics.entity.*;
import com.logistics.entity.id.ShipmentOrderId;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.*;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.*;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentAddEditRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.response.manager.GetOrdersByShipmentIdManagerResponse;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.ShipmentSpecification;
import com.logistics.utils.ShipmentUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.output.ByteArrayOutputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.logistics.utils.OrderUtils.*;
import static com.logistics.utils.ShipmentUtils.translateShipmentStatus;
import static com.logistics.utils.ShipmentUtils.translateShipmentType;

@Service
@RequiredArgsConstructor
public class ShipmentManagerService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentManagerService.class);

    private final ShipmentRepository repository;
    private final ShipmentRepository shipmentRepository;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final OfficeRepository officeRepository;
    private final ShipperAssignmentRepository shipperAssignmentRepository;
    private final EmployeeManagerService employeeManagerService;
    private final NotificationService notificationService;

    public ListResponse<ManagerShipmentListDto> list(int userId,
                                                     ManagerShipmentSearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
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

        String direction = request.getDirection();

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(direction.equals("INBOUND")
                        ? ShipmentSpecification.toOffice(userOffice.getId())
                        : ShipmentSpecification.fromOffice(userOffice.getId()))
                .and(ShipmentSpecification.search(search))
                .and(ShipmentSpecification.status(status))
                .and(ShipmentSpecification.type(type))
                .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<Shipment> pageData = repository.findAll(spec, pageable);

        List<ManagerShipmentListDto> list = pageData.getContent()
                .stream()
                .map(ShipmentMapper::toManagerShipmentListDto)
                .toList();

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<ManagerShipmentListDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public GetOrdersByShipmentIdManagerResponse getOrdersByShipmentId(
            int userId,
            int shipmentId,
            ManagerOrdersShipmentSearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Shipment shipment = repository.findById(shipmentId)
                .filter(s -> {
                    boolean isFromOffice = s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId());
                    boolean isToOffice = s.getToOffice() != null && s.getToOffice().getId().equals(userOffice.getId());
                    return isFromOffice || isToOffice;
                })
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        List<ManagerShipmentDetailDto> orders = shipment.getShipmentOrders()
                .stream()
                .map(ShipmentOrder::getOrder)
                .filter(o -> search == null || o.getTrackingNumber().contains(search))
                .map(OrderMapper::toManagerShipmentDetailDto)
                .toList();

        int total = orders.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);
        List<ManagerShipmentDetailDto> pagedList = orders.subList(fromIndex, toIndex);

        Pagination pagination = new Pagination(total, page, limit, (int) Math.ceil((double) total / limit));

        ListResponse<ManagerShipmentDetailDto> data = new ListResponse<>();
        data.setList(pagedList);
        data.setPagination(pagination);

        return GetOrdersByShipmentIdManagerResponse.builder()
                .orders(data)
                .status(shipment.getStatus())
                .type(shipment.getType())
                .build();
    }

    public List<Integer> getAllOrderIdsByShipmentId(
            int userId,
            int shipmentId,
            ManagerOrdersShipmentSearchRequest request) {
        String search = request.getSearch();

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Shipment shipment = repository.findById(shipmentId)
                .filter(s -> {
                    boolean isFromOffice = s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId());
                    boolean isToOffice = s.getToOffice() != null && s.getToOffice().getId().equals(userOffice.getId());
                    return isFromOffice || isToOffice;
                })
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        return shipment.getShipmentOrders()
                .stream()
                .map(ShipmentOrder::getOrder)
                .filter(o -> search == null || o.getTrackingNumber().contains(search))
                .filter(Order::getPendingDestinationConfirm)
                .map(Order::getId)
                .toList();
    }

    public void cancelShipment(Integer userId, Integer shipmentId) {
        Shipment shipment = repository.findById(shipmentId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (!ShipmentUtils.canManagerCancelShipment(shipment.getStatus())) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_CANNOT_CANCEL);
        }

        if (shipment.getFromOffice() == null || !userOffice.getId().equals(shipment.getFromOffice().getId())) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_ACCESS_DENIED);
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        repository.save(shipment);
    }

    public ListResponse<ManagerShipmentListDto> getPendingAndInTransitShipments(
            Integer userId,
            SearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        String type = request.getType();
        String sort = request.getSort();

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(ShipmentSpecification.fromOffice(userOffice.getId()))
                .and(ShipmentSpecification.search(search))
                .and((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (type == null || ShipmentType.TRANSFER.name().equals(type)) {
                        predicates.add(cb.and(
                                cb.equal(root.get("type"), ShipmentType.TRANSFER),
                                root.get("status").in(ShipmentStatus.PENDING)
                        ));
                    }
                    if (type == null || ShipmentType.DELIVERY.name().equals(type)) {
                        predicates.add(cb.and(
                                cb.equal(root.get("type"), ShipmentType.DELIVERY),
                                root.get("status").in(ShipmentStatus.PENDING, ShipmentStatus.IN_TRANSIT)
                        ));
                    }
                    return cb.or(predicates.toArray(new Predicate[0]));
                });
        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<Shipment> pageData = repository.findAll(spec, pageable);

        List<ManagerShipmentListDto> list = pageData.getContent()
                .stream()
                .map(ShipmentMapper::toManagerShipmentListDto)
                .toList();

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<ManagerShipmentListDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    @Transactional
    public void create(int userId, ManagerShipmentAddEditRequest request) {
        // Validate
        if (request.getType() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, "Loại chuyến");
        }

        if (!request.getType().isBlank()) {
            try {
                ShipmentType.valueOf(request.getType());
            } catch (IllegalArgumentException e) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_INVALID_TYPE);
            }
        }

        ShipmentType type = ShipmentType.valueOf(request.getType());

        // Bưu cục
        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        // Lấy xe nếu được chọn
        Vehicle vehicle = null;
        if (request.getVehicleId() != null) {
            if (type.equals(ShipmentType.DELIVERY)) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_INVALID_DELIVERY_TYPE);
            }
            vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new AppException(VehicleErrorCode.VEHICLE_NOT_FOUND));

            if (!office.getId().equals(vehicle.getOffice().getId())) {
                throw new AppException(VehicleErrorCode.VEHICLE_OFFICE_MISMATCH);
            }
        }

        // Lấy employee nếu được chọn
        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

            if (!office.getId().equals(employee.getOffice().getId())) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_OFFICE_MISMATCH);
            }

            if (type.equals(ShipmentType.DELIVERY) &&
                    (employee.getAccountRole() == null ||
                            employee.getAccountRole().getRole() == null ||
                            !"Shipper".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_SHIPPER_INVALID);
            }

            if (type.equals(ShipmentType.TRANSFER) &&
                    (employee.getAccountRole() == null ||
                            employee.getAccountRole().getRole() == null ||
                            !"Driver".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_DRIVER_INVALID);
            }
        }

        // Lấy bưu cục đích nếu được chọn
        Office toOffice = null;
        if (request.getToOfficeId() != null) {
            if (type.equals(ShipmentType.DELIVERY)) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_DELIVERY_INVALID_OFFICE_DESTINATION);
            }
            toOffice = officeRepository.findById(request.getToOfficeId())
                    .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));

            if (office.getId().equals(toOffice.getId())) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_INVALID_OFFICE_DESTINATION);
            }
        }

        // Tạo Shipment
        Shipment shipment = new Shipment();
        shipment.setType(type);
        shipment.setVehicle(vehicle);
        shipment.setEmployee(employee);
        shipment.setFromOffice(office);
        shipment.setToOffice(toOffice);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setCreatedBy(office.getManager());

        shipmentRepository.save(shipment);

        // Gửi thông báo nếu có employee được gán
        if (employee != null && employee.getUser() != null) {
            notificationService.create(
                    "Bạn được phân công chuyến hàng mới",
                    "Bạn đã được gán vào chuyến hàng " + shipment.getCode(),
                    "shipment",
                    employee.getUser().getId(),
                    null,
                    "shipments",
                    shipment.getCode());
        }
    }

    @Transactional
    public void update(int userId, Integer shipmentId, ManagerShipmentAddEditRequest request) {
        // Lấy shipment hiện tại
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        Office office = employeeManagerService.getManagedOfficeByUserId(userId);

        if (!office.getId().equals(shipment.getFromOffice().getId())) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_ACCESS_DENIED);
        }

        // Kiểm tra loại shipment
        if (request.getType() == null || request.getType().isBlank()) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, "Loại chuyến");
        }
        ShipmentType type = ShipmentType.valueOf(request.getType());

        shipment.setType(type);

        // Xử lý Vehicle
        Vehicle vehicle = null;
        if (request.getVehicleId() != null) {
            if (type.equals(ShipmentType.DELIVERY)) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_INVALID_DELIVERY_TYPE);
            }
            vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new AppException(VehicleErrorCode.VEHICLE_NOT_FOUND));

            if (!vehicle.getOffice().getId().equals(office.getId())) {
                throw new AppException(VehicleErrorCode.VEHICLE_OFFICE_MISMATCH);
            }
        }
        shipment.setVehicle(vehicle);

        // Xử lý Employee
        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

            if (!employee.getOffice().getId().equals(office.getId())) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_OFFICE_MISMATCH);
            }

            if (type.equals(ShipmentType.DELIVERY)
                    && (employee.getAccountRole() == null
                    || !"Shipper".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_SHIPPER_INVALID);
            }

            if (type.equals(ShipmentType.TRANSFER)
                    && (employee.getAccountRole() == null
                    || !"Driver".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                throw new AppException(EmployeeErrorCode.EMPLOYEE_DRIVER_INVALID);
            }
        }
        shipment.setEmployee(employee);

        // Xử lý bưu cục đích
        Office toOffice = null;
        if (request.getToOfficeId() != null) {
            if (type.equals(ShipmentType.DELIVERY)) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_DELIVERY_INVALID_OFFICE_DESTINATION);
            }
            toOffice = officeRepository.findById(request.getToOfficeId())
                    .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));

            if (office.getId().equals(toOffice.getId())) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_INVALID_OFFICE_DESTINATION);
            }
        }
        shipment.setToOffice(toOffice);

        // Tính tổng trọng lượng hiện tại
        BigDecimal totalWeight = shipment.getShipmentOrders().stream()
                .map(so -> so.getOrder().getWeight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Kiểm tra xe có đủ sức chứa với các recipientaddress hiện tại
        if (vehicle != null && totalWeight.compareTo(vehicle.getCapacity()) > 0) {
            throw new AppException(VehicleErrorCode.VEHICLE_CAPACITY_EXCEEDED);
        }

        // Kiểm tra các recipientaddress có phân công khu vực hợp lệ (nếu có employee)
        if (employee != null && type.equals(ShipmentType.DELIVERY)) {
            LocalDateTime now = LocalDateTime.now();
            boolean allOrdersValid = true;
            StringBuilder msg = new StringBuilder();
            for (var so : shipment.getShipmentOrders()) {
                var order = so.getOrder();
                Integer targetCityCode = order.getRecipientAddress().getCityCode();

                boolean hasActiveAssignment = shipperAssignmentRepository
                        .findActiveAssignments(employee.getUser().getId(), now)
                        .stream()
                        .anyMatch(sa -> sa.getCityCode().equals(targetCityCode));
                if (!hasActiveAssignment) {
                    allOrdersValid = false;
                    msg.append(order.getTrackingNumber()).append(", ");
                }
            }

            if (!allOrdersValid) {
                if (!msg.isEmpty()) {
                    msg.setLength(msg.length() - 2);
                }
                throw new AppException(ShipmentErrorCode.SHIPMENT_ORDER_OUT_OF_SERVICE_AREA);
            }
        }

        shipmentRepository.save(shipment);

        if (employee != null && employee.getUser() != null) {
            notificationService.create(
                    "Chuyến hàng được cập nhật",
                    "Bạn đã được gán vào chuyến hàng " + shipment.getCode(),
                    "shipment",
                    employee.getUser().getId(),
                    null,
                    "shipments",
                    shipment.getCode());
        }
    }

    public ListResponse<ManagerShipmentPerformanceDto> getShipmentsByEmployeeId(int userId,
                                                                                int employeeId,
                                                                                SearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        String status = request.getStatus();
        String sort = request.getSort();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        if (!emp.getOffice().getId().equals(userOffice.getId())) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_ACCESS_DENIED);
        }

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(ShipmentSpecification.employeeId(emp.getId()))
                .and(ShipmentSpecification.searchByCode(search))
                .and(ShipmentSpecification.status(status))
                .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<Shipment> pageData = repository.findAll(spec, pageable);

        List<ManagerShipmentPerformanceDto> list = pageData.getContent()
                .stream()
                .map(shipment -> {

                    long orderCount = shipment.getShipmentOrders() != null
                            ? shipment.getShipmentOrders().size()
                            : 0;

                    long totalWeight = shipment.getShipmentOrders() != null
                            ? shipment.getShipmentOrders()
                            .stream()
                            .map(so -> so.getOrder())
                            .filter(o -> o != null && o.getWeight() != null)
                            .mapToLong(o -> o.getWeight().longValue())
                            .sum()
                            : 0;

                    return ShipmentMapper.toManagerShipmentPerformanceDto(
                            shipment,
                            orderCount,
                            totalWeight);
                })
                .toList();

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<ManagerShipmentPerformanceDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public byte[] exportShipmentsByEmployeeId(int userId, int employeeId, SearchRequest request) {
        String search = request.getSearch();
        String status = request.getStatus();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        if (!emp.getOffice().getId().equals(userOffice.getId())) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_ACCESS_DENIED);
        }

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(ShipmentSpecification.employeeId(emp.getId()))
                .and(ShipmentSpecification.searchByCode(search))
                .and(ShipmentSpecification.status(status))
                .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<Shipment> shipments = repository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Shipments");

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
                    "Mã chuyến",
                    "Trạng thái",
                    "Thời gian bắt đầu",
                    "Thời gian kết thúc",
                    "Tổng đơn",
                    "Tổng trọng lượng (Kg)"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (Shipment s : shipments) {
                Row row = sheet.createRow(rowIdx++);

                long orderCount = s.getShipmentOrders() != null ? s.getShipmentOrders().size() : 0;
                double totalWeight = s.getShipmentOrders() != null
                        ? s.getShipmentOrders().stream()
                        .map(so -> so.getOrder())
                        .filter(o -> o != null && o.getWeight() != null)
                        .mapToDouble(o -> o.getWeight().doubleValue())
                        .sum()
                        : 0;

                row.createCell(0).setCellValue(s.getCode() != null ? s.getCode() : "");
                row.createCell(1).setCellValue(translateShipmentStatus(s.getStatus()));
                row.createCell(2).setCellValue(s.getStartTime() != null ? s.getStartTime().format(dtf) : "N/A");
                row.createCell(3).setCellValue(s.getEndTime() != null ? s.getEndTime().format(dtf) : "N/A");
                row.createCell(4).setCellValue(orderCount);
                row.createCell(5).setCellValue(totalWeight);
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

    public byte[] exportShipmentPerformance(Integer userId, Integer employeeId,
                                            SearchRequest request) {
        // Lấy toàn bộ danh sách không phân trang
        List<ManagerShipmentPerformanceDto> shipments = getShipmentsByEmployeeIdForExport(userId, employeeId, request);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Shipments");

            // Header style
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle
                    .setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"Mã chuyến", "Loại chuyến", "Trạng thái", "Biển số xe",
                    "Khối lượng", "Thời gian bắt đầu", "Thời gian kết thúc",
                    "Tổng đơn", "Tổng trọng lượng"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            DecimalFormat df = new DecimalFormat("#,###");
            df.setGroupingUsed(true);

            int rowIdx = 1;
            for (ManagerShipmentPerformanceDto dto : shipments) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(dto.getCode() != null ? dto.getCode() : "");
                row.createCell(1).setCellValue(dto.getStatus() != null ? dto.getStatus() : "");
                row.createCell(2).setCellValue(dto.getStartTime() != null ? dtf.format(dto.getStartTime()) : "");
                row.createCell(3).setCellValue(dto.getEndTime() != null ? dtf.format(dto.getEndTime()) : "");
                row.createCell(4).setCellValue(df.format(dto.getOrderCount()));
                row.createCell(5).setCellValue(df.format(dto.getTotalWeight()));
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

    public List<ManagerShipmentPerformanceDto> getShipmentsByEmployeeIdForExport(
            int userId,
            int employeeId,
            SearchRequest request) {
        String search = request.getSearch();
        String status = request.getStatus();
        String sort = request.getSort();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        if (!emp.getOffice().getId().equals(userOffice.getId())) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_ACCESS_DENIED);
        }

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(ShipmentSpecification.employeeId(emp.getId()))
                .and(ShipmentSpecification.searchByCode(search))
                .and(ShipmentSpecification.status(status))
                .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch ((sort != null ? sort.toLowerCase() : "")) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.unsorted();
        };

        // Lấy toàn bộ list không phân trang
        List<Shipment> shipments = repository.findAll(spec, sortOpt);

        // Map sang DTO
        return shipments.stream().map(shipment -> {
            long totalOrders = shipment.getShipmentOrders() != null
                    ? shipment.getShipmentOrders().size()
                    : 0;

            long totalWeight = shipment.getShipmentOrders() != null
                    ? shipment.getShipmentOrders()
                    .stream()
                    .map(so -> so.getOrder())
                    .filter(o -> o != null && o.getWeight() != null)
                    .mapToLong(o -> o.getWeight().longValue())
                    .sum()
                    : 0;

            return ShipmentMapper.toManagerShipmentPerformanceDto(
                    shipment,
                    totalOrders,
                    totalWeight);
        }).toList();
    }

    public byte[] export(int userId, ManagerShipmentSearchRequest request) {
        String search = request.getSearch();
        String status = request.getStatus();
        String type = request.getType();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        String direction = request.getDirection();

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(direction.equals("INBOUND")
                        ? ShipmentSpecification.toOffice(userOffice.getId())
                        : ShipmentSpecification.fromOffice(userOffice.getId()))
                .and(ShipmentSpecification.search(search))
                .and(ShipmentSpecification.status(status))
                .and(ShipmentSpecification.type(type))
                .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<Shipment> shipments = repository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Shipments");

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
                    "Mã chuyến",
                    "Loại chuyến",
                    "Trạng thái",
                    "Biển số xe", "Tải trọng (Kg)",
                    "Bưu cục đến", "Mã bưu cục đến",
                    "Nhân viên phụ trách", "Mã NV phụ trách", "SĐT NV phụ trách", "Email NV phụ trách",
                    "Nhân viên tạo chuyến", "Mã NV tạo", "SĐT NV tạo", "Email NV tạo",
                    "Thời gian bắt đầu",
                    "Thời gian kết thúc",
                    "Thời gian tạo",
                    "Thời gian cập nhật"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (Shipment s : shipments) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(s.getCode() != null ? s.getCode() : "");
                row.createCell(1).setCellValue(translateShipmentType(s.getType()));
                row.createCell(2).setCellValue(translateShipmentStatus(s.getStatus()));

                // Phương tiện
                row.createCell(3).setCellValue(s.getVehicle() != null ? s.getVehicle().getLicensePlate() : "N/A");
                row.createCell(4).setCellValue(s.getVehicle() != null && s.getVehicle().getCapacity() != null
                        ? s.getVehicle().getCapacity().doubleValue() : 0);

                // Bưu cục đến
                row.createCell(5).setCellValue(s.getToOffice() != null ? s.getToOffice().getName() : "N/A");
                row.createCell(6).setCellValue(s.getToOffice() != null && s.getToOffice().getPostalCode() != null
                        ? s.getToOffice().getPostalCode() : "N/A");

                // Nhân viên phụ trách
                if (s.getEmployee() != null) {
                    if (s.getEmployee().getUser() != null) {
                        String lastName = s.getEmployee().getUser().getLastName() != null ? s.getEmployee().getUser().getLastName() : "";
                        String firstName = s.getEmployee().getUser().getFirstName() != null ? s.getEmployee().getUser().getFirstName() : "";
                        row.createCell(7).setCellValue((lastName + " " + firstName).trim());

                        row.createCell(9).setCellValue(s.getEmployee().getUser().getPhoneNumber() != null ? s.getEmployee().getUser().getPhoneNumber() : "");

                        if (s.getEmployee().getUser().getAccount() != null) {
                            row.createCell(10).setCellValue(s.getEmployee().getUser().getAccount().getEmail() != null ? s.getEmployee().getUser().getAccount().getEmail() : "");
                        } else {
                            row.createCell(10).setCellValue("");
                        }
                    } else {
                        row.createCell(7).setCellValue("N/A");
                        row.createCell(9).setCellValue("");
                        row.createCell(10).setCellValue(""); // Thêm để đồng bộ
                    }
                    row.createCell(8).setCellValue(s.getEmployee().getCode() != null ? s.getEmployee().getCode() : "");
                } else {
                    row.createCell(7).setCellValue("N/A");
                    row.createCell(8).setCellValue("");
                    row.createCell(9).setCellValue("");
                    row.createCell(10).setCellValue("");
                }

                // Nhân viên tạo chuyến
                if (s.getCreatedBy() != null) {
                    if (s.getCreatedBy().getUser() != null) {
                        String lastName = s.getCreatedBy().getUser().getLastName() != null ? s.getCreatedBy().getUser().getLastName() : "";
                        String firstName = s.getCreatedBy().getUser().getFirstName() != null ? s.getCreatedBy().getUser().getFirstName() : "";
                        row.createCell(11).setCellValue((lastName + " " + firstName).trim());
                        row.createCell(13).setCellValue(s.getCreatedBy().getUser().getPhoneNumber() != null ? s.getCreatedBy().getUser().getPhoneNumber() : "");

                        if (s.getCreatedBy().getUser().getAccount() != null) {
                            row.createCell(14).setCellValue(s.getCreatedBy().getUser().getAccount().getEmail() != null ? s.getCreatedBy().getUser().getAccount().getEmail() : "");
                        } else {
                            row.createCell(14).setCellValue("");
                        }
                    } else {
                        row.createCell(11).setCellValue("N/A");
                        row.createCell(13).setCellValue("");
                        row.createCell(14).setCellValue("");
                    }
                    row.createCell(12).setCellValue(s.getCreatedBy().getCode() != null ? s.getCreatedBy().getCode() : "");
                } else {
                    row.createCell(11).setCellValue("N/A");
                    row.createCell(12).setCellValue("");
                    row.createCell(13).setCellValue("");
                    row.createCell(14).setCellValue("");
                }

                // Thời gian
                row.createCell(15).setCellValue(s.getStartTime() != null ? s.getStartTime().format(dtf) : "N/A");
                row.createCell(16).setCellValue(s.getEndTime() != null ? s.getEndTime().format(dtf) : "N/A");
                row.createCell(17).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().format(dtf) : "");
                row.createCell(18).setCellValue(s.getUpdatedAt() != null ? s.getUpdatedAt().format(dtf) : "N/A");
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

    public byte[] exportOrdersByShipmentId(int userId, int shipmentId, ManagerOrdersShipmentSearchRequest request) {
        String search = request.getSearch();

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Shipment shipment = repository.findById(shipmentId)
                .filter(s -> {
                    boolean isFromOffice = s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId());
                    boolean isToOffice = s.getToOffice() != null && s.getToOffice().getId().equals(userOffice.getId());
                    return isFromOffice || isToOffice;
                })
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        List<ManagerShipmentDetailDto> orders = shipment.getShipmentOrders()
                .stream()
                .map(ShipmentOrder::getOrder)
                .filter(o -> search == null || o.getTrackingNumber().contains(search))
                .map(OrderMapper::toManagerShipmentDetailDto)
                .toList();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders_" + shipment.getCode());

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
                    "Mã đơn",
                    "Trạng thái",
                    "Người thanh toán", "Trạng thái thanh toán",
                    "Phí thu hộ (VNĐ)",
                    "Trọng lượng (Kg)",
                    "Tên người nhận", "SĐT người nhận", "Địa chỉ người nhận",
                    "Bưu cục đích", "Mã bưu cục đích"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ManagerShipmentDetailDto o : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(o.getTrackingNumber() != null ? o.getTrackingNumber() : "Chưa có mã");
                row.createCell(1).setCellValue(translateOrderStatus(OrderStatus.valueOf(o.getStatus())));
                row.createCell(2).setCellValue(translateOrderPayerType(OrderPayerType.valueOf(o.getPayer())));
                row.createCell(3).setCellValue(translateOrderPaymentStatus(OrderPaymentStatus.valueOf(o.getPaymentStatus())));

                // Phí thu hộ: tính giống FE
                double cod = o.getCod() != null ? o.getCod().doubleValue() : 0;
                double totalFee = o.getTotalFee() != null ? o.getTotalFee().doubleValue() : 0;
                double fee = "CUSTOMER".equals(o.getPayer()) ? cod + totalFee : cod;
                row.createCell(4).setCellValue(fee);

                row.createCell(5).setCellValue(o.getWeight() != null ? o.getWeight().doubleValue() : 0);

                // Người nhận
                row.createCell(6).setCellValue(o.getRecipientName() != null ? o.getRecipientName() : "");
                row.createCell(7).setCellValue(o.getRecipientPhone() != null ? o.getRecipientPhone() : "");
                row.createCell(8).setCellValue(o.getRecipientFullAddress());

                // Bưu cục đích
                if (o.getToOffice() != null) {
                    row.createCell(9).setCellValue(o.getToOffice().getName() != null ? o.getToOffice().getName() : "N/A");
                    row.createCell(10).setCellValue(o.getToOffice().getPostalCode() != null ? o.getToOffice().getPostalCode() : "N/A");
                } else {
                    row.createCell(9).setCellValue("N/A");
                    row.createCell(10).setCellValue("N/A");
                }
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
}