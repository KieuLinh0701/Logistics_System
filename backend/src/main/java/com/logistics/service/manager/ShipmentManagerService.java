package com.logistics.service.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.dto.manager.shipment.ManagerShipmentPerformanceDto;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Shipment;
import com.logistics.entity.Vehicle;
import com.logistics.enums.*;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.ShipmentRepository;
import com.logistics.repository.ShipperAssignmentRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentAddEditRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.ShipmentSpecification;
import com.logistics.utils.AddressUtils;
import com.logistics.utils.ShipmentUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.logistics.utils.OrderUtils.*;
import static com.logistics.utils.ShipmentUtils.translateShipmentStatus;
import static com.logistics.utils.ShipmentUtils.translateShipmentType;

@Service
@RequiredArgsConstructor
public class ShipmentManagerService {

    private final ShipmentRepository repository;
    private final ShipmentRepository shipmentRepository;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final OfficeRepository officeRepository;
    private final ShipperAssignmentRepository shipperAssignmentRepository;
    private final EmployeeManagerService employeeManagerService;
    private final NotificationService notificationService;

    public ApiResponse<ListResponse<ManagerShipmentListDto>> list(int userId,
            ManagerShipmentSearchRequest request) {
        try {
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

            Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                    .and(ShipmentSpecification.fromOffice(userOffice.getId()))
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

            return new ApiResponse<>(true, "Lấy danh sách chuyến hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Đã xảy ra lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ListResponse<ManagerShipmentDetailDto>> getOrdersByShipmentId(
            int userId,
            int shipmentId,
            ManagerOrdersShipmentSearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            Shipment shipment = repository.findById(shipmentId)
                    .filter(s -> s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId()))
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy chuyến hàng hoặc bạn không có quyền đối với chuyến hàng này"));

            List<ManagerShipmentDetailDto> orders = shipment.getShipmentOrders()
                    .stream()
                    .map(so -> so.getOrder())
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

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng của chuyến hàng thành công", data);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Đã xảy ra lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> cancelShipment(Integer userId, Integer shipmentId) {
        Shipment shipment = repository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng"));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (!ShipmentUtils.canManagerCancelShipment(shipment.getStatus())) {
            throw new RuntimeException("Chuyến hàng đã thực hiện, không thể hủy");
        }

        if (shipment.getFromOffice() == null || !userOffice.getId().equals(shipment.getFromOffice().getId())) {
            return new ApiResponse<>(false, "Bạn không có quyền hủy chuyến hàng này", false);
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        repository.save(shipment);

        return new ApiResponse<>(true, "Hủy chuyến hàng thành công", true);
    }

    public ApiResponse<ListResponse<ManagerShipmentListDto>> getPendingShipments(Integer userId,
            SearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String type = request.getType();
            String sort = request.getSort();

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                    .and(ShipmentSpecification.fromOffice(userOffice.getId()))
                    .and(ShipmentSpecification.search(search))
                    .and(ShipmentSpecification.status(ShipmentStatus.PENDING.name()))
                    .and(ShipmentSpecification.type(type));

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

            return new ApiResponse<>(true, "Lấy danh sách chuyến hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Đã xảy ra lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> create(int userId, ManagerShipmentAddEditRequest request) {
        try {
            // Validate
            if (request.getType() == null) {
                return new ApiResponse<>(false, "Loại chuyến là bắt buộc!", false);
            }

            if (request.getType() != null && !request.getType().isBlank()) {
                try {
                    ShipmentType.valueOf(request.getType());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Loại chuyến hàng không hợp lệ: " + request.getType());
                }
            }

            ShipmentType type = ShipmentType.valueOf(request.getType());

            // Bưu cục
            Office office = employeeManagerService.getManagedOfficeByUserId(userId);

            // Lấy xe nếu được chọn
            Vehicle vehicle = null;
            if (request.getVehicleId() != null) {
                if (type.equals(ShipmentType.DELIVERY)) {
                    return new ApiResponse<>(false, "Loại chuyến là chuyến giao hàng không cần chọn xe!", false);
                }
                vehicle = vehicleRepository.findById(request.getVehicleId())
                        .orElseThrow(() -> new RuntimeException("Xe được chọn không tồn tại!"));

                if (!office.getId().equals(vehicle.getOffice().getId())) {
                    return new ApiResponse<>(false, "Phương tiện được chọn không thuộc bưu cục của bạn!", false);
                }
            }

            // Lấy employee nếu được chọn
            Employee employee = null;
            if (request.getEmployeeId() != null) {
                employee = employeeRepository.findById(request.getEmployeeId())
                        .orElseThrow(() -> new RuntimeException("Nhân viên được chọn không tồn tại!"));

                if (!office.getId().equals(employee.getOffice().getId())) {
                    return new ApiResponse<>(false, "Nhân viên được chọn không thuộc bưu cục của bạn!", false);
                }

                if (type.equals(ShipmentType.DELIVERY) &&
                        (employee.getAccountRole() == null ||
                                employee.getAccountRole().getRole() == null ||
                                !"Shipper".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                    return new ApiResponse<>(false,
                            "Nhân viên được chọn cho chuyến giao hàng không phải là nhân viên giao hàng!", false);
                }

                if (type.equals(ShipmentType.TRANSFER) &&
                        (employee.getAccountRole() == null ||
                                employee.getAccountRole().getRole() == null ||
                                !"Driver".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                    return new ApiResponse<>(false, "Nhân viên được chọn cho chuyến trung chuyển không phải là tài xế!",
                            false);
                }
            }

            // Lấy bưu cục đích nếu được chọn
            Office toOffice = null;
            if (request.getToOfficeId() != null) {
                if (type.equals(ShipmentType.DELIVERY)) {
                    return new ApiResponse<>(false, "Loại chuyến là chuyến giao hàng không cần chọn bưu cục đến!",
                            false);
                }
                toOffice = officeRepository.findById(request.getToOfficeId())
                        .orElseThrow(() -> new RuntimeException("Bưu cục được chọn không tồn tại!"));

                if (office.getId().equals(toOffice.getId())) {
                    return new ApiResponse<>(false, "Bưu cục đến không được trùng với bưu cục xuât phát!", false);
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

            return new ApiResponse<>(true, "Tạo chuyến hàng thành công!", true);

        } catch (Exception ex) {
            return new ApiResponse<>(false, ex.getMessage(), false);
        }
    }

    @Transactional
    public ApiResponse<Boolean> update(int userId, Integer shipmentId, ManagerShipmentAddEditRequest request) {
        try {
            // Lấy shipment hiện tại
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new RuntimeException("Chuyến hàng không tồn tại!"));

            Office office = employeeManagerService.getManagedOfficeByUserId(userId);

            if (!office.getId().equals(shipment.getFromOffice().getId())) {
                return new ApiResponse<>(false, "Bạn không có quyền cập nhật chuyến hàng này!", false);
            }

            // Kiểm tra loại shipment
            if (request.getType() == null || request.getType().isBlank()) {
                return new ApiResponse<>(false, "Loại chuyến là bắt buộc!", false);
            }
            ShipmentType type = ShipmentType.valueOf(request.getType());

            shipment.setType(type);

            // Xử lý Vehicle
            Vehicle vehicle = null;
            if (request.getVehicleId() != null) {
                if (type.equals(ShipmentType.DELIVERY)) {
                    return new ApiResponse<>(false, "Chuyến giao hàng không cần chọn xe!", false);
                }
                vehicle = vehicleRepository.findById(request.getVehicleId())
                        .orElseThrow(() -> new RuntimeException("Xe được chọn không tồn tại!"));

                if (!vehicle.getOffice().getId().equals(office.getId())) {
                    return new ApiResponse<>(false, "Xe không thuộc bưu cục của bạn!", false);
                }
            }
            shipment.setVehicle(vehicle);

            // Xử lý Employee
            Employee employee = null;
            if (request.getEmployeeId() != null) {
                employee = employeeRepository.findById(request.getEmployeeId())
                        .orElseThrow(() -> new RuntimeException("Nhân viên được chọn không tồn tại!"));

                if (!employee.getOffice().getId().equals(office.getId())) {
                    return new ApiResponse<>(false, "Nhân viên không thuộc bưu cục của bạn!", false);
                }

                if (type.equals(ShipmentType.DELIVERY)
                        && (employee.getAccountRole() == null
                                || !"Shipper".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                    return new ApiResponse<>(false, "Nhân viên được chọn không phải Shipper!", false);
                }

                if (type.equals(ShipmentType.TRANSFER)
                        && (employee.getAccountRole() == null
                                || !"Driver".equalsIgnoreCase(employee.getAccountRole().getRole().getName()))) {
                    return new ApiResponse<>(false, "Nhân viên được chọn không phải Driver!", false);
                }
            }
            shipment.setEmployee(employee);

            // Xử lý bưu cục đích
            Office toOffice = null;
            if (request.getToOfficeId() != null) {
                if (type.equals(ShipmentType.DELIVERY)) {
                    return new ApiResponse<>(false, "Chuyến giao hàng không cần chọn bưu cục đến!", false);
                }
                toOffice = officeRepository.findById(request.getToOfficeId())
                        .orElseThrow(() -> new RuntimeException("Bưu cục được chọn không tồn tại!"));

                if (office.getId().equals(toOffice.getId())) {
                    return new ApiResponse<>(false, "Bưu cục đến không được trùng với bưu cục xuất phát!", false);
                }
            }
            shipment.setToOffice(toOffice);

            // Tính tổng trọng lượng hiện tại
            BigDecimal totalWeight = shipment.getShipmentOrders().stream()
                    .map(so -> so.getOrder().getWeight())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Kiểm tra xe có đủ sức chứa với các recipientaddress hiện tại
            if (vehicle != null && totalWeight.compareTo(vehicle.getCapacity()) > 0) {
                return new ApiResponse<>(false,
                        "Tổng trọng lượng các đơn hiện tại vượt quá sức chứa của xe. Vui lòng chọn xe khác.",
                        false);
            }

            // Kiểm tra các recipientaddress có phân công khu vực hợp lệ (nếu có employee)
            if (employee != null && type.equals(ShipmentType.DELIVERY)) {
                LocalDateTime now = LocalDateTime.now();
                boolean allOrdersValid = true;
                StringBuilder msg = new StringBuilder();
                for (var so : shipment.getShipmentOrders()) {
                    var order = so.getOrder();
                    Integer targetCityCode = order.getRecipientAddress().getCityCode();
                    Integer targetWardCode = order.getRecipientAddress().getWardCode();

                    boolean hasActiveAssignment = shipperAssignmentRepository
                            .findActiveAssignments(employee.getUser().getId(), now)
                            .stream()
                            .anyMatch(sa -> sa.getCityCode().equals(targetCityCode) &&
                                    sa.getWardCode().equals(targetWardCode));
                    if (!hasActiveAssignment) {
                        allOrdersValid = false;
                        msg.append(order.getTrackingNumber()).append(", ");
                    }
                }

                if (!allOrdersValid) {
                    if (msg.length() > 0) {
                        msg.setLength(msg.length() - 2);
                    }
                    return new ApiResponse<>(false,
                            "Một số đơn không nằm trong khu vực phân công của Nhân viên viên giao hàng. " + msg,
                            false);
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

            return new ApiResponse<>(true, "Cập nhật chuyến hàng thành công!", true);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), false);
        }
    }

    public ApiResponse<ListResponse<ManagerShipmentPerformanceDto>> getShipmentsByEmployeeId(int userId,
            int employeeId,
            SearchRequest request) {
        try {
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
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên này"));

            if (!emp.getOffice().getId().equals(userOffice.getId())) {
                return new ApiResponse<>(false,
                        "Bạn chỉ có thể xem danh sách chuyến hàng của nhân viên trong bưu cục của mình.", null);
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

            return new ApiResponse<>(true, "Lấy danh sách chuyến hàng của nhân viên thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Đã xảy ra lỗi: " + e.getMessage(), null);
        }
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên này"));

        if (!emp.getOffice().getId().equals(userOffice.getId())) {
            throw new RuntimeException("Bạn chỉ có thể xem danh sách chuyến hàng của nhân viên trong bưu cục của mình.");
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
            throw new RuntimeException("Lỗi khi xuất Excel", e);
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
            font.setColor(new XSSFColor(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle
                    .setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0x1C, (byte) 0x3D, (byte) 0x90 }, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = { "Mã chuyến", "Loại chuyến", "Trạng thái", "Biển số xe",
                    "Khối lượng", "Thời gian bắt đầu", "Thời gian kết thúc",
                    "Tổng đơn", "Tổng trọng lượng" };
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
            throw new RuntimeException("Lỗi khi xuất Excel", e);
        }
    }

    public List<ManagerShipmentPerformanceDto> getShipmentsByEmployeeIdForExport(int userId,
            int employeeId,
            SearchRequest request) {
        try {
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
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên này"));

            if (!emp.getOffice().getId().equals(userOffice.getId())) {
                throw new RuntimeException(
                        "Bạn chỉ có thể xem danh sách chuyến hàng của nhân viên trong bưu cục của mình.");
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
            List<ManagerShipmentPerformanceDto> list = shipments.stream().map(shipment -> {
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

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách chuyến hàng: " + e.getMessage(), e);
        }
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

        Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                .and(ShipmentSpecification.fromOffice(userOffice.getId()))
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
            throw new RuntimeException("Lỗi khi xuất Excel", e);
        }
    }

    public byte[] exportOrdersByShipmentId(int userId, int shipmentId, ManagerOrdersShipmentSearchRequest request) {
        String search = request.getSearch();

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Shipment shipment = repository.findById(shipmentId)
                .filter(s -> s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId()))
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chuyến hàng hoặc bạn không có quyền đối với chuyến hàng này"));

        List<ManagerShipmentDetailDto> orders = shipment.getShipmentOrders()
                .stream()
                .map(so -> so.getOrder())
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
                if (o.getRecipient() != null) {
                    row.createCell(6).setCellValue(o.getRecipient().getName() != null ? o.getRecipient().getName() : "");
                    row.createCell(7).setCellValue(o.getRecipient().getPhone() != null ? o.getRecipient().getPhone() : "");
                    row.createCell(8).setCellValue(o.getRecipient().getFullAddress());
                } else {
                    row.createCell(6).setCellValue("");
                    row.createCell(7).setCellValue("");
                    row.createCell(8).setCellValue("");
                }

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
            throw new RuntimeException("Lỗi khi xuất Excel", e);
        }
    }
}