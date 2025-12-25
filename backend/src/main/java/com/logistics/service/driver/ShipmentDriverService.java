package com.logistics.service.driver;

import com.logistics.entity.*;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.VehicleStatus;
import com.logistics.repository.*;
import com.logistics.request.driver.FinishShipmentRequest;
import com.logistics.request.driver.UpdateVehicleTrackingRequest;
import com.logistics.response.ApiResponse;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
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
    private com.logistics.service.assignment.AutoAssignService autoAssignService;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleTrackingRepository vehicleTrackingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OfficeRepository officeRepository;

    private Employee getCurrentEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin nhân viên (driver)");
        }
        return employees.get(0);
    }

    @Transactional
    public ApiResponse<String> startShipment(Integer shipmentId) {
        try {
            Employee employee = getCurrentEmployee();

            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng"));

            if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
                return new ApiResponse<>(false, "Bạn không có quyền thực hiện chuyến hàng này", null);
            }

            if (shipment.getStatus() != ShipmentStatus.PENDING) {
                return new ApiResponse<>(false, "Chuyến hàng không ở trạng thái PENDING", null);
            }

            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            shipment.setStartTime(LocalDateTime.now());
            shipmentRepository.save(shipment);

            // Cập nhật trạng thái đơn hàng từ PICKED_UP -> IN_TRANSIT
            List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipmentId);
            for (ShipmentOrder so : shipmentOrders) {
                Order order = so.getOrder();
                
                // Kiểm tra đơn hàng phải ở trạng thái PICKED_UP
                if (order.getStatus() != OrderStatus.PICKED_UP) {
                    return new ApiResponse<>(false, "Đơn hàng " + order.getTrackingNumber() + " không ở trạng thái PICKED_UP", null);
                }
                
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

            return new ApiResponse<>(true, "Đã bắt đầu vận chuyển", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> finishShipment(FinishShipmentRequest request) {
        try {
            Employee employee = getCurrentEmployee();

            Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng"));

            if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
                return new ApiResponse<>(false, "Bạn không có quyền thực hiện chuyến hàng này", null);
            }

            ShipmentStatus newStatus = request.getStatus();
            if (newStatus != ShipmentStatus.COMPLETED && newStatus != ShipmentStatus.CANCELLED) {
                return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
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

            // Cập nhật trạng thái đơn hàng và ghi lịch sử
            List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(request.getShipmentId());
            for (ShipmentOrder so : shipmentOrders) {
                Order order = so.getOrder();

                // Ghi OrderHistory
                OrderHistory history = new OrderHistory();
                history.setOrder(order);
                history.setFromOffice(order.getFromOffice());
                history.setToOffice(order.getToOffice());
                history.setShipment(shipment);
                
                if (newStatus == ShipmentStatus.COMPLETED) {
                    // Kiểm tra đơn hàng phải ở trạng thái IN_TRANSIT
                    if (order.getStatus() != OrderStatus.IN_TRANSIT) {
                        return new ApiResponse<>(false, "Đơn hàng " + order.getTrackingNumber() + " không ở trạng thái IN_TRANSIT", null);
                    }
                    history.setAction(OrderHistoryActionType.IMPORTED);
                    history.setNote("Đơn đã đến bưu cục đích");
                    order.setStatus(OrderStatus.AT_DEST_OFFICE);
                } else {
                    history.setAction(OrderHistoryActionType.RETURNED);
                    history.setNote("Chuyến vận chuyển bị hủy");
                }
                
                orderHistoryRepository.save(history);
                orderRepository.save(order);
                // kích hoạt auto-assign khi đơn về bưu cục
                autoAssignService.autoAssignOnArrival(order.getId());
            }

            return new ApiResponse<>(true, "Đã hoàn tất chuyến hàng", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getShipments(int page, int limit) {
        try {
            Employee employee = getCurrentEmployee();

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

            Specification<Shipment> spec = (root, query, cb) -> {
                return cb.equal(root.get("employee").get("id"), employee.getId());
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

                        // Lấy danh sách đơn hàng
                        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
                        List<Map<String, Object>> orders = shipmentOrders.stream()
                                .map(so -> {
                                    Order order = so.getOrder();
                                    Map<String, Object> orderMap = new HashMap<>();
                                    orderMap.put("id", order.getId());
                                    orderMap.put("trackingNumber", order.getTrackingNumber());
                                    if (order.getToOffice() != null) {
                                        orderMap.put("toOffice", Map.of(
                                                "id", order.getToOffice().getId(),
                                                "name", order.getToOffice().getName() != null ? order.getToOffice().getName() : ""
                                        ));
                                    }
                                    return orderMap;
                                })
                                .collect(Collectors.toList());
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

            return new ApiResponse<>(true, "Thành công", data);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getRoute() {
        try {
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
                return new ApiResponse<>(true, "Không có chuyến hàng đang hoạt động", emptyData);
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
                return new ApiResponse<>(true, "Chuyến hàng không có đơn", emptyData);
            }

            // Nhóm orders theo toOffice
            Map<Integer, Map<String, Object>> officeGroups = new HashMap<>();
            for (ShipmentOrder so : shipmentOrders) {
                Order order = so.getOrder();
                if (order.getToOffice() == null) continue;

                Integer officeId = order.getToOffice().getId();
                officeGroups.computeIfAbsent(officeId, k -> {
                    Map<String, Object> group = new HashMap<>();
                    group.put("office", Map.of(
                            "id", order.getToOffice().getId(),
                            "name", order.getToOffice().getName() != null ? order.getToOffice().getName() : "",
                            "address", buildOfficeAddress(order.getToOffice())
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

            return new ApiResponse<>(true, "Thành công", data);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getHistory(int page, int limit) {
        try {
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

                        // Lấy danh sách đơn hàng
                        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
                        List<Map<String, Object>> orders = shipmentOrders.stream()
                                .map(so -> {
                                    Order order = so.getOrder();
                                    Map<String, Object> orderMap = new HashMap<>();
                                    orderMap.put("id", order.getId());
                                    orderMap.put("trackingNumber", order.getTrackingNumber());
                                    if (order.getToOffice() != null) {
                                        orderMap.put("toOffice", Map.of(
                                                "id", order.getToOffice().getId(),
                                                "name", order.getToOffice().getName() != null ? order.getToOffice().getName() : ""
                                        ));
                                    }
                                    return orderMap;
                                })
                                .collect(Collectors.toList());
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

            return new ApiResponse<>(true, "Thành công", data);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> updateVehicleTracking(UpdateVehicleTrackingRequest request) {
        try {
            Employee employee = getCurrentEmployee();

            Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng"));

            if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
                return new ApiResponse<>(false, "Bạn không có quyền cập nhật vị trí cho chuyến hàng này", null);
            }

            if (shipment.getVehicle() == null) {
                return new ApiResponse<>(false, "Chuyến hàng không có phương tiện", null);
            }

            // Thay vì tạo nhiều bản ghi, cập nhật bản ghi tracking gần nhất cho shipment này (nếu có),
            // mục tiêu: chỉ giữ 1 bản ghi đang được cập nhật liên tục
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

            return new ApiResponse<>(true, "Đã cập nhật vị trí", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getVehicleTracking(Integer shipmentId) {
        try {
            Employee employee = getCurrentEmployee();

            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến hàng"));

            if (shipment.getEmployee() == null || !shipment.getEmployee().getId().equals(employee.getId())) {
                return new ApiResponse<>(false, "Bạn không có quyền xem vị trí chuyến hàng này", null);
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

            return new ApiResponse<>(true, "Thành công", data);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
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
            builder.append("Phường ").append(office.getWardCode());
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

