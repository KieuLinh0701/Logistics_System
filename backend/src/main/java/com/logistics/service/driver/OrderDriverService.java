package com.logistics.service.driver;

import com.logistics.entity.*;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.VehicleStatus;
import com.logistics.enums.ShipmentType;
import com.logistics.id.ShipmentOrderId;
import com.logistics.repository.*;
import com.logistics.request.driver.PickUpRequest;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderDriverService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentOrderRepository shipmentOrderRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Employee getCurrentEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin nhân viên (driver)");
        }
        return employees.get(0);
    }

    public ApiResponse<Map<String, Object>> getContext() {
        try {
            Employee employee = getCurrentEmployee();
            Office office = employee.getOffice();
            
            // Lấy danh sách xe available tại office
            List<Vehicle> vehicles = vehicleRepository.findByOfficeId(office.getId())
                    .stream()
                    .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("office", Map.of(
                    "id", office.getId(),
                    "name", office.getName() != null ? office.getName() : "",
                    "address", buildOfficeAddress(office)
            ));
            data.put("vehicles", vehicles.stream().map(v -> Map.of(
                    "id", v.getId(),
                    "licensePlate", v.getLicensePlate(),
                    "type", v.getType().name(),
                    "capacity", v.getCapacity()
            )).collect(Collectors.toList()));

            return new ApiResponse<>(true, "Thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getPendingOrders(int page, int limit) {
        try {
            Employee employee = getCurrentEmployee();
            Office fromOffice = employee.getOffice();

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

            Specification<Order> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                // Chỉ lấy đơn ở trạng thái AT_ORIGIN_OFFICE
                predicates.add(cb.equal(root.get("status"), OrderStatus.AT_ORIGIN_OFFICE));
                // Chỉ lấy đơn từ office của driver
                predicates.add(cb.equal(root.get("fromOffice").get("id"), fromOffice.getId()));

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<Order> orderPage = orderRepository.findAll(spec, pageable);

            List<Map<String, Object>> orders = orderPage.getContent().stream()
                    .map(this::mapOrderSummary)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("orders", orders);
            data.put("pagination", Map.of(
                    "page", page,
                    "limit", limit,
                    "total", orderPage.getTotalElements(),
                    "totalPages", orderPage.getTotalPages()
            ));

            return new ApiResponse<>(true, "Thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> pickUp(PickUpRequest request) {
        try {
            Employee employee = getCurrentEmployee();
            Office fromOffice = employee.getOffice();
            User driverUser = userRepository.findById(SecurityUtils.getAuthenticatedUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

            List<Integer> orderIds = request.getOrderIds();
            if (orderIds == null || orderIds.isEmpty()) {
                return new ApiResponse<>(false, "Thiếu danh sách đơn", null);
            }

            // Kiểm tra và cập nhật trạng thái xe
            Vehicle vehicle = null;
            if (request.getVehicleId() != null) {
                vehicle = vehicleRepository.findById(request.getVehicleId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phương tiện"));
                
                if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                    return new ApiResponse<>(false, "Phương tiện không khả dụng", null);
                }
                
                if (!vehicle.getOffice().getId().equals(fromOffice.getId())) {
                    return new ApiResponse<>(false, "Phương tiện không thuộc bưu cục của bạn", null);
                }

                vehicle.setStatus(VehicleStatus.IN_USE);
                vehicleRepository.save(vehicle);
            }

            // Xác định toOffice và validate orders trước khi tạo shipment
            Office toOffice = null;
            List<Order> validOrders = new ArrayList<>();

            for (Integer orderId : orderIds) {
                Order order = orderRepository.findById(orderId)
                        .orElse(null);
                
                if (order == null) continue;
                
                // Kiểm tra đơn ở trạng thái AT_ORIGIN_OFFICE
                if (order.getStatus() != OrderStatus.AT_ORIGIN_OFFICE) continue;
                
                // Kiểm tra đơn thuộc fromOffice của driver
                if (!Objects.equals(order.getFromOffice().getId(), fromOffice.getId())) continue;

                validOrders.add(order);
                
                // Lấy toOffice từ đơn đầu tiên có toOffice
                if (toOffice == null && order.getToOffice() != null) {
                    toOffice = order.getToOffice();
                }
            }

            if (validOrders.isEmpty()) {
                return new ApiResponse<>(false, "Không có đơn hàng hợp lệ để nhận", null);
            }

            // Validate toOffice 
            if (toOffice == null) {
                return new ApiResponse<>(false, "Các đơn hàng không có bưu cục đích, không thể tạo chuyến vận chuyển", null);
            }

            // Tạo shipment với đầy đủ thông tin
            Shipment shipment = new Shipment();
            shipment.setVehicle(vehicle);
            shipment.setEmployee(employee);
            shipment.setFromOffice(fromOffice);
            shipment.setToOffice(toOffice);
            shipment.setStatus(ShipmentStatus.PENDING);
            shipment.setType(ShipmentType.TRANSFER);
            shipment.setStartTime(LocalDateTime.now());
            
            // Tạo mã shipment
            shipment = shipmentRepository.save(shipment);
            String code = "SM_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + shipment.getId();
            shipment.setCode(code);
            shipment = shipmentRepository.save(shipment);

            // Cập nhật đơn và tạo ShipmentOrder
            for (Order order : validOrders) {
                order.setStatus(OrderStatus.PICKED_UP);
                orderRepository.save(order);

                // Tạo ShipmentOrder
                ShipmentOrder shipmentOrder = new ShipmentOrder();
                ShipmentOrderId shipmentOrderId = new ShipmentOrderId(shipment.getId(), order.getId());
                shipmentOrder.setId(shipmentOrderId);
                shipmentOrder.setShipment(shipment);
                shipmentOrder.setOrder(order);
                shipmentOrderRepository.save(shipmentOrder);

                // Ghi OrderHistory
                OrderHistory history = new OrderHistory();
                history.setOrder(order);
                history.setFromOffice(fromOffice);
                history.setToOffice(order.getToOffice());
                history.setShipment(shipment);
                history.setAction(OrderHistoryActionType.PICKED_UP);
                history.setNote("Driver " + employee.getUser().getFullName() + " đã nhận hàng để vận chuyển");
                orderHistoryRepository.save(history);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("shipmentId", shipment.getId());
            data.put("shipmentCode", shipment.getCode());

            return new ApiResponse<>(true, "Đã nhận hàng, tạo chuyến Pending", data);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    private Map<String, Object> mapOrderSummary(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("trackingNumber", order.getTrackingNumber());
        map.put("senderName", order.getSenderName());
        map.put("senderPhone", order.getSenderPhone());
        map.put("recipientName", order.getRecipientName());
        map.put("recipientPhone", order.getRecipientPhone());
        map.put("weight", order.getWeight());
        map.put("cod", order.getCod());
        map.put("shippingFee", order.getShippingFee());
        map.put("status", order.getStatus().name());
        map.put("createdAt", order.getCreatedAt());

        if (order.getToOffice() != null) {
            map.put("toOffice", Map.of(
                    "id", order.getToOffice().getId(),
                    "name", order.getToOffice().getName() != null ? order.getToOffice().getName() : ""
            ));
        }

        if (order.getServiceType() != null) {
            map.put("serviceType", Map.of(
                    "id", order.getServiceType().getId(),
                    "name", order.getServiceType().getName() != null ? order.getServiceType().getName() : ""
            ));
        }

        return map;
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

