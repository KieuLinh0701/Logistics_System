package com.logistics.service.driver;

import com.logistics.entity.*;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.VehicleStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.ShipmentErrorCode;
import com.logistics.exception.enums.VehicleErrorCode;
import com.logistics.repository.*;
import com.logistics.request.driver.FinishShipmentRequest;
import com.logistics.request.driver.UpdateVehicleTrackingRequest;
import com.logistics.service.common.NotificationService;
import com.logistics.service.common.OrderDestinationService;
import com.logistics.utils.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShipmentDriverService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentOrderRepository shipmentOrderRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleTrackingRepository vehicleTrackingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private OrderDestinationService orderDestinationService;

    private Employee getCurrentEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.get(0);
    }

    @Transactional
    public void startShipment(Integer shipmentId) {
        Employee employee = getCurrentEmployee();

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_PERMISSION_DENIED);
        }

        if (shipment.getStatus() != ShipmentStatus.PENDING) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_PENDING);
        }

        // Kiểm tra và cập nhật Vehicle -> IN_USE nếu có
        if (shipment.getVehicle() != null) {
            Vehicle vehicle = shipment.getVehicle();
            if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                throw new AppException(VehicleErrorCode.VEHICLE_NOT_AVAILABLE);
            }
            vehicle.setStatus(VehicleStatus.IN_USE);
            vehicleRepository.save(vehicle);
        }

        // Lấy danh sách orders cho shipment và cập nhật shipment -> IN_TRANSIT
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipmentId);

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setStartTime(LocalDateTime.now());
        shipmentRepository.save(shipment);

        // Cập nhật trạng thái tất cả đơn trong shipment -> IN_TRANSIT
        for (ShipmentOrder so : shipmentOrders) {
            Order order = so.getOrder();
            order.setStatus(OrderStatus.IN_TRANSIT);
            orderRepository.save(order);

            // Ghi OrderHistory
            OrderHistory history = new OrderHistory();
            history.setOrder(order);
            history.setFromOffice(order.getFromOffice());
            history.setToOffice(order.getToOffice());
            history.setShipment(shipment);
            history.setAction(OrderHistoryActionType.EXPORTED);
            history.setNote("Đơn hàng đang được vận chuyển giữa bưu cục");
            orderHistoryRepository.save(history);
        }
    }

    @Transactional
    public void finishShipment(FinishShipmentRequest request) {
        Employee employee = getCurrentEmployee();

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_PERMISSION_DENIED);
        }

        ShipmentStatus newStatus = request.getStatus();
        if (newStatus != ShipmentStatus.COMPLETED && newStatus != ShipmentStatus.CANCELLED) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_INVALID_STATUS);
        }

        shipment.setStatus(newStatus);
        shipment.setEndTime(LocalDateTime.now());
        shipmentRepository.save(shipment);

        // Cập nhật trạng thái xe về Available
        if (shipment.getVehicle() != null) {
            Vehicle vehicle = shipment.getVehicle();
            if (vehicle.getStatus() == VehicleStatus.IN_USE) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
            }
        }

        // Driver chỉ cập nhật ShipmentStatus, không cập nhật OrderStatus
        // OrderStatus sẽ do Manager xử lý ở chức năng riêng

        // Khi COMPLETED: kiểm tra và set pendingDestinationConfirm cho các đơn trong shipment
        if (newStatus == ShipmentStatus.COMPLETED && shipment.getToOffice() != null) {
            List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
            for (ShipmentOrder so : shipmentOrders) {
                Order order = so.getOrder();
                if (orderDestinationService.isDestinationOffice(order, shipment.getToOffice())) {
                    order.setPendingDestinationConfirm(true);
                    orderRepository.save(order);
                }
            }
        }
    }

    public Map<String, Object> getShipments(int page, int limit) {
        Employee employee = getCurrentEmployee();

        Office employeeOffice = employee.getOffice();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Shipment> spec = (root, query, cb) -> {
            // Hiển thị các chuyến đã gán cho driver hoặc chưa gán nhưng thuộc bưu cục của driver
            var predicates = new ArrayList<Predicate>();
            Predicate assignedToMe = cb.equal(root.get("employee").get("id"), employee.getId());
            predicates.add(assignedToMe);

            if (employeeOffice != null) {
                Predicate unassignedAndSameOffice = cb.and(
                        cb.isNull(root.get("employee")),
                        cb.equal(root.get("fromOffice").get("id"), employeeOffice.getId())
                );
                predicates.add(unassignedAndSameOffice);
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };

        Page<Shipment> shipmentPage = shipmentRepository.findAll(spec, pageable);

        List<Map<String, Object>> shipments = shipmentPage.getContent().stream()
                .map(shipment -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", shipment.getId());
                    map.put("code", shipment.getCode());
                    map.put("status", shipment.getStatus().name());
                    map.put("startTime", shipment.getStartTime());
                    map.put("endTime", shipment.getEndTime());
                    map.put("createdAt", shipment.getCreatedAt());

                    if (shipment.getVehicle() != null) {
                        map.put("vehicle", Map.of(
                                "id", shipment.getVehicle().getId(),
                                "licensePlate", shipment.getVehicle().getLicensePlate(),
                                "type", shipment.getVehicle().getType().name()
                        ));
                    }

                    if (shipment.getEmployee() != null) {
                        map.put("employee", Map.of(
                                "id", shipment.getEmployee().getId(),
                                "userId", shipment.getEmployee().getUser() != null ? shipment.getEmployee().getUser().getId() : null
                        ));
                    }

                    if (shipment.getFromOffice() != null) {
                        map.put("fromOffice", Map.of(
                                "id", shipment.getFromOffice().getId(),
                                "name", shipment.getFromOffice().getName() != null ? shipment.getFromOffice().getName() : ""
                        ));
                    }

                    if (shipment.getToOffice() != null) {
                        map.put("toOffice", Map.of(
                                "id", shipment.getToOffice().getId(),
                                "name", shipment.getToOffice().getName() != null ? shipment.getToOffice().getName() : ""
                        ));
                    }

                    // Lấy danh sách đơn hàng
                    List<ShipmentOrder> soList = shipmentOrderRepository.findByShipmentId(shipment.getId());
                    List<Map<String, Object>> orders = soList.stream().map(so -> {
                        Order order = so.getOrder();
                        Map<String, Object> om = new HashMap<>();
                        om.put("id", order.getId());
                        om.put("trackingNumber", order.getTrackingNumber());
                        om.put("status", order.getStatus() != null ? order.getStatus().name() : null);
                        if (order.getToOffice() != null) {
                            om.put("toOffice", Map.of(
                                    "id", order.getToOffice().getId(),
                                    "name", order.getToOffice().getName() != null ? order.getToOffice().getName() : ""
                            ));
                        }
                        return om;
                    }).collect(Collectors.toList());

                    map.put("orders", orders);
                    map.put("orderCount", orders.size());

                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("shipments", shipments);
        data.put("pagination", Map.of(
                "page", page,
                "limit", limit,
                "total", shipmentPage.getTotalElements(),
                "totalPages", shipmentPage.getTotalPages()
        ));

        return data;
    }

    public Map<String, Object> getRoute() {
        Employee employee = getCurrentEmployee();

        // Tìm shipment đang hoạt động (PENDING hoặc IN_TRANSIT)
        Specification<Shipment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("employee").get("id"), employee.getId()));
            predicates.add(root.get("status").in(ShipmentStatus.PENDING, ShipmentStatus.IN_TRANSIT));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Shipment> activeShipments = shipmentRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (activeShipments.isEmpty()) {
            // quyết định nghiệp vụ: không có chuyến hàng đang hoạt động
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("routeInfo", null);
            emptyData.put("deliveryStops", Collections.emptyList());
            return emptyData;
        }

        // Ưu tiên chuyến hàng đang có đơn đã được gán.
        Shipment activeShipment = null;
        List<ShipmentOrder> shipmentOrders = new ArrayList<>();
        for (Shipment s : activeShipments) {
            List<ShipmentOrder> soList = shipmentOrderRepository.findByShipmentId(s.getId());
            if (soList != null && !soList.isEmpty()) {
                activeShipment = s;
                shipmentOrders = soList;
                break;
            }
        }
        // Nếu không có chuyến nào có đơn, chọn chuyến gần nhất
        if (activeShipment == null) {
            activeShipment = activeShipments.get(0);
            shipmentOrders = shipmentOrderRepository.findByShipmentId(activeShipment.getId());
        }

        if (shipmentOrders.isEmpty()) {
            // quyết định nghiệp vụ: chuyến hàng tồn tại nhưng chưa có đơn
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("routeInfo", null);
            emptyData.put("deliveryStops", Collections.emptyList());
            return emptyData;
        }

        // Nhóm orders theo toOffice
        Map<Integer, Map<String, Object>> officeGroups = new HashMap<>();
        for (ShipmentOrder so : shipmentOrders) {
            Order order = so.getOrder();
            // Nếu recipientaddress.toOffice là null thì dùng shipment.toOffice (một số chuyến có thể chỉ có 1 bưu cục đích)
            Office targetOffice = order.getToOffice() != null ? order.getToOffice() : activeShipment.getToOffice();
            if (targetOffice == null) continue;

            Integer officeId = targetOffice.getId();
            officeGroups.computeIfAbsent(officeId, k -> {
                Map<String, Object> group = new HashMap<>();
                group.put("office", Map.of(
                        "id", targetOffice.getId(),
                        "name", targetOffice.getName() != null ? targetOffice.getName() : "",
                        "address", buildOfficeAddress(targetOffice)
                ));
                group.put("orders", new ArrayList<>());
                return group;
            });

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) officeGroups.get(officeId).get("orders");
            orders.add(Map.of(
                    "id", order.getId(),
                    "trackingNumber", order.getTrackingNumber()
            ));
        }

        // Tạo delivery stops
        List<Map<String, Object>> deliveryStops = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> group : officeGroups.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> office = (Map<String, Object>) group.get("office");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) group.get("orders");

            Map<String, Object> stop = new HashMap<>();
            stop.put("id", office.get("id"));
            stop.put("officeName", office.get("name"));
            stop.put("officeAddress", office.get("address"));
            stop.put("orderCount", orders.size());
            stop.put("orders", orders);
            stop.put("status", "pending");
            deliveryStops.add(stop);
            index++;
        }

        // Tạo routeInfo
        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("id", activeShipment.getId());
        routeInfo.put("code", activeShipment.getCode());
        routeInfo.put("name", "Tuyến vận chuyển #" + activeShipment.getId());
        routeInfo.put("status", activeShipment.getStatus().name());
        routeInfo.put("totalStops", deliveryStops.size());
        routeInfo.put("totalOrders", shipmentOrders.size());
        routeInfo.put("startTime", activeShipment.getStartTime());

        if (activeShipment.getFromOffice() != null) {
            routeInfo.put("fromOffice", Map.of(
                    "id", activeShipment.getFromOffice().getId(),
                    "name", activeShipment.getFromOffice().getName() != null ? activeShipment.getFromOffice().getName() : ""
            ));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("routeInfo", routeInfo);
        data.put("deliveryStops", deliveryStops);

        return data;
    }

    public Map<String, Object> getHistory(int page, int limit) {
        Employee employee = getCurrentEmployee();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Shipment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("employee").get("id"), employee.getId()));
            predicates.add(root.get("status").in(ShipmentStatus.COMPLETED, ShipmentStatus.CANCELLED));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Shipment> shipmentPage = shipmentRepository.findAll(spec, pageable);

        List<Map<String, Object>> shipments = shipmentPage.getContent().stream()
                .map(shipment -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", shipment.getId());
                    map.put("code", shipment.getCode());
                    map.put("status", shipment.getStatus().name());
                    map.put("startTime", shipment.getStartTime());
                    map.put("endTime", shipment.getEndTime());
                    map.put("createdAt", shipment.getCreatedAt());

                    if (shipment.getVehicle() != null) {
                        map.put("vehicle", Map.of(
                                "id", shipment.getVehicle().getId(),
                                "licensePlate", shipment.getVehicle().getLicensePlate(),
                                "type", shipment.getVehicle().getType().name()
                        ));
                    }

                    if (shipment.getFromOffice() != null) {
                        map.put("fromOffice", Map.of(
                                "id", shipment.getFromOffice().getId(),
                                "name", shipment.getFromOffice().getName() != null ? shipment.getFromOffice().getName() : ""
                        ));
                    }

                    if (shipment.getToOffice() != null) {
                        map.put("toOffice", Map.of(
                                "id", shipment.getToOffice().getId(),
                                "name", shipment.getToOffice().getName() != null ? shipment.getToOffice().getName() : ""
                        ));
                    }

                    // Trả chi tiết đơn ở dạng read-only cho driver trong lịch sử
                    List<ShipmentOrder> soListHist = shipmentOrderRepository.findByShipmentId(shipment.getId());
                    List<Map<String, Object>> ordersHist = soListHist.stream().map(so -> {
                        Order order = so.getOrder();
                        Map<String, Object> om = new HashMap<>();
                        om.put("id", order.getId());
                        om.put("trackingNumber", order.getTrackingNumber());
                        om.put("status", order.getStatus() != null ? order.getStatus().name() : null);
                        if (order.getToOffice() != null) {
                            om.put("toOffice", Map.of(
                                    "id", order.getToOffice().getId(),
                                    "name", order.getToOffice().getName() != null ? order.getToOffice().getName() : ""
                            ));
                        }
                        return om;
                    }).collect(Collectors.toList());

                    map.put("orders", ordersHist);
                    map.put("orderCount", ordersHist.size());

                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("shipments", shipments);
        data.put("pagination", Map.of(
                "page", page,
                "limit", limit,
                "total", shipmentPage.getTotalElements(),
                "totalPages", shipmentPage.getTotalPages()
        ));

        return data;
    }

    @Transactional
    public void updateVehicleTracking(UpdateVehicleTrackingRequest request) {
        Employee employee = getCurrentEmployee();

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_PERMISSION_DENIED);
        }

        if (shipment.getVehicle() == null) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND, "Chuyến hàng không có phương tiện");
        }

        // Cập nhật bản ghi tracking gần nhất cho shipment này (nếu có)
        List<VehicleTracking> existing = vehicleTrackingRepository.findByShipmentIdOrderByRecordedAtDesc(shipment.getId());
        if (existing != null && !existing.isEmpty()) {
            VehicleTracking t = existing.get(0);
            t.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            t.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            t.setSpeed(BigDecimal.valueOf(request.getSpeed()));
            t.setRecordedAt(LocalDateTime.now());
            vehicleTrackingRepository.save(t);
        } else {
            VehicleTracking tracking = new VehicleTracking();
            tracking.setVehicle(shipment.getVehicle());
            tracking.setShipment(shipment);
            tracking.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            tracking.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            tracking.setSpeed(BigDecimal.valueOf(request.getSpeed()));
            tracking.setRecordedAt(LocalDateTime.now());
            vehicleTrackingRepository.save(tracking);
        }

        // Cập nhật vị trí hiện tại của xe
        Vehicle vehicle = shipment.getVehicle();
        vehicle.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        vehicle.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        vehicleRepository.save(vehicle);
    }

    public Map<String, Object> getVehicleTracking(Integer shipmentId) {
        Employee employee = getCurrentEmployee();

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_PERMISSION_DENIED);
        }

        List<VehicleTracking> trackings = vehicleTrackingRepository.findByShipmentIdOrderByRecordedAtDesc(shipmentId);

        List<Map<String, Object>> trackingList = trackings.stream()
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("latitude", t.getLatitude().doubleValue());
                map.put("longitude", t.getLongitude().doubleValue());
                map.put("speed", t.getSpeed().doubleValue());
                map.put("recordedAt", t.getRecordedAt());
                return map;
            })
            .collect(Collectors.toList());


        Map<String, Object> data = new HashMap<>();
        data.put("trackings", trackingList);

        return data;
    }

    private String buildOfficeAddress(Office office) {
        if (office == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (office.getDetail() != null && !office.getDetail().isBlank()) {
            builder.append(office.getDetail());
        }
        if (office.getWardCode() != null) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Phuong ").append(office.getWardCode());
        }
        if (office.getCityCode() != null) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("TP ").append(office.getCityCode());
        }
        return builder.toString();
    }
}

