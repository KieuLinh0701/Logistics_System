package com.logistics.service.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Shipment;
import com.logistics.entity.Vehicle;
import com.logistics.enums.EmployeeShift;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;
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
import com.logistics.utils.ShipmentUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

            // Kiểm tra xe có đủ sức chứa với các order hiện tại
            if (vehicle != null && totalWeight.compareTo(vehicle.getCapacity()) > 0) {
                return new ApiResponse<>(false,
                        "Tổng trọng lượng các đơn hiện tại vượt quá sức chứa của xe. Vui lòng chọn xe khác.",
                        false);
            }

            // Kiểm tra các order có phân công khu vực hợp lệ (nếu có employee)
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

}