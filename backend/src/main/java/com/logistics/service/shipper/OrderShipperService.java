package com.logistics.service.shipper;

import com.logistics.entity.Address;
import com.logistics.entity.DeliveryAttempt;
import com.logistics.entity.Employee;
import com.logistics.entity.IncidentReport;
import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.PaymentSubmissionItem;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.User;
import com.logistics.entity.ShipperVehicle;
import com.logistics.enums.IncidentPriority;
import com.logistics.enums.IncidentStatus;
import com.logistics.enums.IncidentType;
import com.logistics.enums.OrderCreatorType;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.OrderPayerType;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.PaymentSubmissionStatus;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.OrderPickupType;
import com.logistics.repository.DeliveryAttemptRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.IncidentReportRepository;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.PickupAttemptRepository;
import com.logistics.request.shipper.CreateIncidentReportRequest;
import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.request.shipper.UpdateDeliveryStatusRequest;
import com.logistics.request.shipper.PickedUpRequest;
import com.logistics.request.shipper.DeliverOriginRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.response.NotificationResponse;
import com.logistics.utils.SecurityUtils;
import com.logistics.service.common.NotificationService;
import com.logistics.service.common.ConfigService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.AiRoutePlanRouteRepository;
import com.logistics.repository.ShipperVehicleRepository;
import com.logistics.service.assignment.AutoAssignService;
import com.logistics.entity.OrderProduct;
import com.logistics.entity.AiRoutePlanRoute;
import com.logistics.entity.AiRoutePlanStop;
import com.logistics.enums.AiRoutePlanStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderShipperService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private IncidentReportRepository incidentReportRepository;

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private PaymentSubmissionRepository paymentSubmissionRepository;

    @Autowired
    private AutoAssignService autoAssignService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private PickupAttemptRepository pickupAttemptRepository;

    @Autowired
    private DeliveryAttemptRepository deliveryAttemptRepository;

    @Autowired
    private ConfigService configService;

    @Autowired
    private AiRoutePlanRouteRepository aiRoutePlanRouteRepository;

    @Autowired
    private ShipperVehicleRepository shipperVehicleRepository;

    private void saveHistory(Order order, OrderHistoryActionType action, String note) {
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setFromOffice(order.getToOffice());
        history.setToOffice(order.getToOffice());
        history.setShipment(null);
        history.setAction(action);
        history.setNote(note);
        orderHistoryRepository.save(history);
    }

    private Employee getCurrentEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin nhân viên (shipper)");
        }
        return employees.get(0);
    }

    private ShipperVehicle getOrCreateVehicle(Employee employee) {
        return shipperVehicleRepository.findByShipperId(employee.getId())
                .orElseGet(() -> {
                    ShipperVehicle vehicle = new ShipperVehicle();
                    vehicle.setShipper(employee);
                    vehicle.setVehicleType(com.logistics.enums.ShipperVehicleType.MOTORBIKE);
                    vehicle.setMaxOrders(20);
                    vehicle.setMaxWeightKg(35);
                    vehicle.setCurrentOrders(0);
                    vehicle.setCurrentWeightKg(BigDecimal.ZERO);
                    vehicle.setBatteryLevel(null);
                    vehicle.setStatus(com.logistics.enums.ShipperVehicleStatus.ACTIVE);
                    vehicle.setNotes("Auto-created default vehicle");
                    return shipperVehicleRepository.save(vehicle);
                });
    }

    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateReturnedWeightKg(Order order) {
        List<OrderProduct> products = orderProductRepository.findByOrderIdWithProduct(order.getId());
        BigDecimal returnedWeight = BigDecimal.ZERO;
        for (OrderProduct op : products) {
            if (op.getReturnedQuantity() == null || op.getReturnedQuantity() <= 0) {
                continue;
            }
            BigDecimal unitWeight = op.getProduct() != null && op.getProduct().getWeight() != null
                    ? op.getProduct().getWeight()
                    : BigDecimal.ZERO;
            returnedWeight = returnedWeight.add(unitWeight.multiply(BigDecimal.valueOf(op.getReturnedQuantity())));
        }
        return normalizeWeight(returnedWeight);
    }

    private void applyVehicleWorkloadByStatus(Order order, Employee employee, OrderStatus newStatus) {
        if (order == null || employee == null || newStatus == null) {
            return;
        }
        ShipperVehicle vehicle = getOrCreateVehicle(employee);
        int currentOrders = vehicle.getCurrentOrders() != null ? vehicle.getCurrentOrders() : 0;
        BigDecimal currentWeightKg = normalizeWeight(vehicle.getCurrentWeightKg());
        BigDecimal orderWeightKg = normalizeWeight(order.getWeight());

        switch (newStatus) {
            case PICKED_UP -> {
                currentOrders += 1;
                currentWeightKg = currentWeightKg.add(orderWeightKg);
            }
            case DELIVERED, DELIVERY_RETRY -> {
                currentOrders = Math.max(0, currentOrders - 1);
                currentWeightKg = currentWeightKg.subtract(orderWeightKg);
            }
            case PARTIAL_DELIVERY, PARTIAL_RETURN -> {
                currentOrders = Math.max(0, currentOrders);
                currentWeightKg = calculateReturnedWeightKg(order);
            }
            case FAILED_DELIVERY -> {
                // Keep workload unchanged because package is still on vehicle.
            }
            default -> {
                return;
            }
        }

        if (currentWeightKg.compareTo(BigDecimal.ZERO) < 0) {
            currentWeightKg = BigDecimal.ZERO;
        }
        vehicle.setCurrentOrders(Math.max(0, currentOrders));
        vehicle.setCurrentWeightKg(normalizeWeight(currentWeightKg));
        shipperVehicleRepository.save(vehicle);
    }

    public ApiResponse<Map<String, Object>> getDashboard() {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // Lấy danh sách đơn trong ngày tại bưu cục của shipper (nội bộ filter)
        Specification<Order> todaySpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));
            // Chỉ bao gồm các đơn đã được gán cho nhân viên này
            predicates.add(cb.equal(root.get("employee").get("id"), employee.getId()));
            // Giữ filter theo createdAt nếu cần, nhưng phần hiển thị sẽ dùng assignedOrders
            predicates.add(cb.between(root.get("createdAt"), startOfDay, endOfDay));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Thống kê cơ bản: tính trên tất cả các đơn đã được gán cho shipper (không chỉ những đơn tạo trong ngày)
        Specification<Order> assignedSpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));
            predicates.add(cb.equal(root.get("employee").get("id"), employee.getId()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Order> assignedOrders = orderRepository.findAll(assignedSpec);

        // Lấy danh sách đơn hiển thị ở phần "Đơn hàng trong ngày".
        // Trước kia chỉ lấy dựa trên createdAt trong ngày, dẫn tới danh sách trống nếu đơn được gán trước ngày hôm nay.
        // Hiện tại dùng các đơn đã được gán cho shipper (assignedOrders) để hiển thị tiện lợi cho shipper.
        List<Order> todayOrders = assignedOrders.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .toList();

        int totalAssigned = (int) assignedOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERING
                || o.getStatus() == OrderStatus.DELIVERED
                || o.getStatus() == OrderStatus.FAILED_DELIVERY
                || o.getStatus() == OrderStatus.RETURNED)
            .count();

        int inProgress = (int) assignedOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERING)
            .count();

        int delivered = (int) assignedOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .count();

        int failed = (int) assignedOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.FAILED_DELIVERY
                || o.getStatus() == OrderStatus.RETURNED)
            .count();

        // COD shipper đã thu (PENDING)
        int codCollected = paymentSubmissionRepository
            .findByShipperIdAndStatusIn(employee.getUser().getId(),
                Arrays.asList(PaymentSubmissionStatus.PENDING))
            .stream()
            .mapToInt(ps -> ps.getActualAmount().intValue())
            .sum();

        List<Map<String, Object>> todayOrderSummaries = todayOrders.stream().map(this::mapOrderSummary).toList();

        // Lấy thông báo gần đây cho shipper (5 thông báo mới nhất)
        List<Map<String, Object>> notificationMaps = Collections.emptyList();
        try {
            NotificationSearchRequest nreq = new NotificationSearchRequest(1, 5, null, null);
            ApiResponse<NotificationResponse> nres = notificationService.getNotifications(employee.getUser().getId(), nreq);
            if (nres != null && nres.isSuccess() && nres.getData() != null) {
                notificationMaps = nres.getData().getNotifications().stream().map(dto -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", dto.getId());
                    m.put("title", dto.getTitle());
                    m.put("message", dto.getMessage());
                    m.put("type", dto.getType());
                    m.put("isRead", dto.getIsRead());
                    m.put("createdAt", dto.getCreatedAt());
                    m.put("creatorName", dto.getCreatorName());
                    return m;
                }).toList();
            }
        } catch (Exception e) { throw new RuntimeException(e); }

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssigned", totalAssigned);
        stats.put("inProgress", inProgress);
        stats.put("delivered", delivered);
        stats.put("failed", failed);
        stats.put("codCollected", codCollected);

        data.put("stats", stats);
        data.put("todayOrders", todayOrderSummaries);
        data.put("notifications", notificationMaps);

        return new ApiResponse<>(true, "Lấy dashboard shipper thành công", data);
    }

    public ApiResponse<Map<String, Object>> listOrders(int page, int limit, String status, String search) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));
            // Chỉ bao gồm các đơn đã được gán cho nhân viên này
            predicates.add(cb.equal(root.get("employee").get("id"), employee.getId()));

                // Đơn đã được shipper nhận / sẵn sàng để lấy / đang giao / đã giao
                predicates.add(root.get("status").in(
                    OrderStatus.PICKED_UP,
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.PICKUP_PENDING,
                    OrderStatus.PICKUP_RETRY,
                    OrderStatus.PICKUP_SUCCESS,
                    OrderStatus.DELIVERING,
                    OrderStatus.DELIVERED,
                    OrderStatus.DELIVERY_RETRY,
                    OrderStatus.FAILED_DELIVERY,
                    OrderStatus.RETURNED
                ));

            if (status != null && !status.isBlank()) {
                try {
                    OrderStatus os = OrderStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), os));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                Predicate byTracking = cb.like(cb.lower(root.get("trackingNumber")), like);
                Predicate byRecipient = cb.like(cb.lower(root.get("recipientName")), like);
                Predicate byPhone = cb.like(cb.lower(root.get("recipientPhone")), like);
                predicates.add(cb.or(byTracking, byRecipient, byPhone));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        List<Map<String, Object>> orders = orderPage.getContent()
                .stream()
                .map(this::mapOrderDetail)
                .toList();

        Pagination pagination = new Pagination(
                (int) orderPage.getTotalElements(),
                page,
                limit,
                orderPage.getTotalPages()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("orders", orders);
        result.put("pagination", pagination);

        return new ApiResponse<>(true, "Lấy danh sách đơn hàng shipper thành công", result);
    }

    public ApiResponse<Map<String, Object>> listUnassignedOrders(int page, int limit) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));

                // Đơn đã đến bưu cục nhưng chưa có shipper nhận (chỉ hiển thị AT_DEST_OFFICE)
                predicates.add(root.get("status").in(
                    OrderStatus.AT_DEST_OFFICE
                ));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<Map<String, Object>> orders = orderPage.getContent()
                .stream()
                .map(this::mapOrderDetail)
                .toList();

        Pagination pagination = new Pagination(
                (int) orderPage.getTotalElements(),
                page,
                limit,
                orderPage.getTotalPages()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("orders", orders);
        result.put("pagination", pagination);

        return new ApiResponse<>(true, "Lấy danh sách đơn chưa gán thành công", result);
    }

    // Mới: lấy danh sách các đơn mà người dùng chọn Yêu cầu lấy hàng (PICKUP_BY_COURIER)
    // và đã đánh dấu SẴN SÀNG LẤY (READY_FOR_PICKUP), chưa có shipper gán.
    public ApiResponse<Map<String, Object>> listPickupByCourierRequests(int page, int limit) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Order> spec = (root, query, cb) -> {
            // Chỉ các đơn do USER tạo
            Predicate createdByUser = cb.equal(root.get("createdByType"), OrderCreatorType.USER);

            // Điều kiện: (READY_FOR_PICKUP && pickupType = PICKUP_BY_COURIER && employee IS NULL)
            List<Predicate> availablePreds = new ArrayList<>();
            availablePreds.add(cb.equal(root.get("status"), OrderStatus.READY_FOR_PICKUP));
            availablePreds.add(cb.equal(root.get("pickupType"), OrderPickupType.PICKUP_BY_COURIER));
            availablePreds.add(cb.isNull(root.get("employee")));

            // Điều kiện: (employee = currentEmployee && status IN (PICKING_UP, PICKED_UP) && pickupType = PICKUP_BY_COURIER)
            List<Predicate> assignedPreds = new ArrayList<>();
            assignedPreds.add(cb.equal(root.get("employee").get("id"), employee.getId()));
            assignedPreds.add(root.get("status").in(
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.PICKING_UP,
                OrderStatus.PICKED_UP,
                OrderStatus.PICKUP_PENDING,
                OrderStatus.PICKUP_RETRY,
                OrderStatus.PICKUP_SUCCESS
            ));
            assignedPreds.add(cb.equal(root.get("pickupType"), OrderPickupType.PICKUP_BY_COURIER));

            // Thu hẹp theo bưu cục của shipper: chỉ theo fromOffice nếu có
            try {
                Predicate fromOfficeMatch = cb.equal(root.get("fromOffice").get("id"), officeId);
                availablePreds.add(fromOfficeMatch);
                assignedPreds.add(fromOfficeMatch);
            } catch (Exception e) { 
                throw new RuntimeException(e); 
            }
            // nếu không có fromOffice, bỏ qua điều kiện

            Predicate available = cb.and(availablePreds.toArray(new Predicate[0]));
            Predicate assignedToMe = cb.and(assignedPreds.toArray(new Predicate[0]));

            // Kết hợp: tạo predicate chính là createdByUser AND (available OR assignedToMe)
            return cb.and(createdByUser, cb.or(available, assignedToMe));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<Map<String, Object>> orders = orderPage.getContent()
                .stream()
                .map(this::mapOrderDetail)
                .toList();

        Pagination pagination = new Pagination(
                (int) orderPage.getTotalElements(),
                page,
                limit,
                orderPage.getTotalPages()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("orders", orders);
        result.put("pagination", pagination);

        return new ApiResponse<>(true, "Lấy danh sách yêu cầu lấy hàng thành công", result);
    }

    public ApiResponse<Map<String, Object>> getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice() != null ? employee.getOffice().getId() : null;

        boolean allowed = false;
        if (order.getEmployee() != null && order.getEmployee().getId() != null) {
            allowed = Objects.equals(order.getEmployee().getId(), employee.getId());
        } else if (order.getToOffice() != null && officeId != null) {
            allowed = Objects.equals(order.getToOffice().getId(), officeId);
        }

        if (!allowed) {
            return new ApiResponse<>(false, "Không có quyền xem đơn hàng này", null);
        }

        return new ApiResponse<>(true, "Lấy thông tin đơn hàng thành công", mapOrderDetail(order));
    }

    public ApiResponse<Map<String, Object>> getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice() != null ? employee.getOffice().getId() : null;

        boolean allowed = false;
        if (order.getEmployee() != null && order.getEmployee().getId() != null) {
            allowed = Objects.equals(order.getEmployee().getId(), employee.getId());
        } else if (order.getToOffice() != null && officeId != null) {
            allowed = Objects.equals(order.getToOffice().getId(), officeId);
        }

        if (!allowed) {
            return new ApiResponse<>(false, "Không có quyền xem đơn hàng này", null);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        data.put("trackingNumber", order.getTrackingNumber());
        return new ApiResponse<>(true, "Lấy đơn hàng theo mã vận đơn thành công", data);
    }

    public Map<String, Object> buildOrderDetail(Order order) {
        return mapOrderDetail(order);
    }
    
    //Bắt đầu luồng giao 1 phần: trả về danh sách sản phẩm và số lượng còn lại/đã giao/đã trả
    @Transactional
    public ApiResponse<Map<String, Object>> startPartialDelivery(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Employee employee = getCurrentEmployee();
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể thao tác giao 1 phần", null);
        }

        List<OrderProduct> products = orderProductRepository.findByOrderIdWithProduct(orderId);

        List<Map<String, Object>> items = products.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("productId", p.getProduct().getId());
            m.put("productName", p.getProduct().getName());
            m.put("quantity", p.getQuantity());
            m.put("deliveredQuantity", p.getDeliveredQuantity() == null ? 0 : p.getDeliveredQuantity());
            m.put("returnedQuantity", p.getReturnedQuantity() == null ? 0 : p.getReturnedQuantity());
            int remaining = p.getQuantity() - (p.getDeliveredQuantity() == null ? 0 : p.getDeliveredQuantity()) - (p.getReturnedQuantity() == null ? 0 : p.getReturnedQuantity());
            m.put("remaining", remaining);
            return m;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("products", items);
        return new ApiResponse<>(true, "Bắt đầu giao 1 phần", data);
    }

    @Transactional
    public ApiResponse<String> markProductDelivered(Integer orderProductId, Integer deliveredQuantity) {
        throw new UnsupportedOperationException("markProductDelivered is not supported. Use markProductDeliveredAtomic(...) which is atomic and concurrency-safe.");
    }

    @Transactional
    public ApiResponse<String> markProductDeliveredAtomic(Integer orderProductId, Integer deliveredQuantity) {
        if (deliveredQuantity == null || deliveredQuantity <= 0) {
            return new ApiResponse<>(false, "Số lượng giao không hợp lệ", null);
        }

        OrderProduct op = orderProductRepository.findById(orderProductId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm đơn hàng"));

        Order order = op.getOrder();
        Employee employee = getCurrentEmployee();
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể thao tác", null);
        }

        int affected = orderProductRepository.incrementDelivered(orderProductId, deliveredQuantity);
        if (affected == 0) {
            return new ApiResponse<>(false, "Số lượng giao vượt quá số lượng còn lại hoặc đã có thay đổi", null);
        }

        // reload to get updated quantities for history
        OrderProduct updated = orderProductRepository.findById(orderProductId).orElse(op);
        saveHistory(order, OrderHistoryActionType.PARTIAL_DELIVERY,
                "Giao " + deliveredQuantity + " x " + (updated.getProduct() != null ? updated.getProduct().getName() : "sản phẩm"));

        return new ApiResponse<>(true, "Cập nhật số lượng đã giao cho sản phẩm thành công", null);
    }

    @Transactional
    public ApiResponse<String> markProductReturned(Integer orderProductId, Integer returnedQuantity, String reason) {
        throw new UnsupportedOperationException("markProductReturned is not supported. Use markProductReturnedAtomic(...) which is atomic and concurrency-safe.");
    }

    @Transactional
    public ApiResponse<String> markProductReturnedAtomic(Integer orderProductId, Integer returnedQuantity, String reason) {
        if (returnedQuantity == null || returnedQuantity <= 0) {
            return new ApiResponse<>(false, "Số lượng trả lại không hợp lệ", null);
        }
        OrderProduct op = orderProductRepository.findById(orderProductId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm đơn hàng"));

        Order order = op.getOrder();
        Employee employee = getCurrentEmployee();
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể thao tác", null);
        }

        int affected = orderProductRepository.incrementReturned(orderProductId, returnedQuantity);
        if (affected == 0) {
            return new ApiResponse<>(false, "Số lượng trả vượt quá số lượng còn lại hoặc đã có thay đổi", null);
        }

        OrderProduct updated = orderProductRepository.findById(orderProductId).orElse(op);

        String note = "Trả lại " + returnedQuantity + " x " + (updated.getProduct() != null ? updated.getProduct().getName() : "sản phẩm");
        if (reason != null && !reason.isBlank()) note += ": " + reason;
        saveHistory(order, OrderHistoryActionType.PARTIAL_RETURN, note);

        return new ApiResponse<>(true, "Cập nhật số lượng trả lại cho sản phẩm thành công", null);
    }

    @Transactional
    public ApiResponse<String> finishPartialDelivery(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Employee employee = getCurrentEmployee();
        // logging removed
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể thao tác", null);
        }

        List<OrderProduct> products = orderProductRepository.findByOrderIdWithProduct(orderId);
        int totalQty = products.stream().mapToInt(p -> p.getQuantity() == null ? 0 : p.getQuantity()).sum();
        int totalDelivered = products.stream().mapToInt(p -> p.getDeliveredQuantity() == null ? 0 : p.getDeliveredQuantity()).sum();
        int totalReturned = products.stream().mapToInt(p -> p.getReturnedQuantity() == null ? 0 : p.getReturnedQuantity()).sum();

        if (totalDelivered >= totalQty && totalReturned == 0) {
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
            applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERED);
            saveHistory(order, OrderHistoryActionType.DELIVERED, "Đã giao toàn bộ đơn hàng");
            // Nếu có COD, tạo submission COD (tính toán từ delivered products)
            try {
                User shipperUser = employee.getUser();
                if (order.getCod() != null && order.getCod() > 0 && !hasExistingCodSubmission(order)) {
                    createPaymentSubmission(order, shipperUser, order.getCod(), "Thu COD sau khi giao");
                }
            } catch (Exception e) { throw new RuntimeException(e); }
            return new ApiResponse<>(true, "Đơn hàng đã được giao hoàn tất", null);
        }

        // Determine final status for partial delivery/return
        if (totalDelivered > 0) {
            order.setStatus(OrderStatus.PARTIAL_DELIVERY);
        } else if (totalReturned > 0) {
            order.setStatus(OrderStatus.PARTIAL_RETURN);
        } else {
            order.setStatus(OrderStatus.DELIVERING);
        }

        orderRepository.save(order);
        applyVehicleWorkloadByStatus(order, employee, order.getStatus());

        if (totalDelivered > 0) {
            saveHistory(order, OrderHistoryActionType.PARTIAL_DELIVERY, "Giao 1 phần: đã giao " + totalDelivered + " / " + totalQty);
        }
        if (totalReturned > 0 && totalDelivered == 0) {
            // only log PARTIAL_RETURN when no delivered items in this session
            saveHistory(order, OrderHistoryActionType.PARTIAL_RETURN, "Trả 1 phần: trả " + totalReturned + " / " + totalQty);
        }

        // Trigger COD submission when there is delivered quantity (partial financial event)
        if (totalDelivered > 0) {
            try {
                User shipperUser = employee.getUser();
                if (order.getCod() != null && order.getCod() > 0 && !hasExistingCodSubmission(order)) {
                    createPaymentSubmission(order, shipperUser, order.getCod(), "Thu COD sau khi giao");
                    try { order.setCodStatus(OrderCodStatus.PENDING); } catch (Exception e) {}
                    orderRepository.save(order);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new ApiResponse<>(true, "Hoàn tất xử lý giao 1 phần (cập nhật trạng thái và lịch sử)", null);
    }

        @Transactional
    public ApiResponse<String> claimOrderRequest(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền theo bưu cục: nếu là đơn lấy tại nhà thì so với fromOffice, ngược lại so với toOffice
        if (order.getPickupType() != null && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            if (order.getFromOffice() == null || !Objects.equals(order.getFromOffice().getId(), officeId)) {
                return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
            }
        } else {
            if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
                return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
            }
        }

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.AT_DEST_OFFICE && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            return new ApiResponse<>(false, "Chỉ có thể nhận đơn đã xác nhận, đã đến bưu cục đích hoặc sẵn sàng lấy", null);
        }

        // Khi shipper nhận yêu cầu lấy hàng tại nhà, gán shipper và chuyển trạng thái sang PICKING_UP
        order.setStatus(OrderStatus.PICKING_UP);
        // Gán employee hiện tại khi shipper nhận đơn
        order.setEmployee(employee);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.PICKING_UP, "Shipper nhận yêu cầu lấy hàng (bắt đầu lấy)");
        return new ApiResponse<>(true, "Nhận đơn thành công", null);
    }

    @Transactional
    public ApiResponse<String> claimOrder(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
            return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
        }

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.AT_DEST_OFFICE && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            return new ApiResponse<>(false, "Chỉ có thể nhận đơn đã xác nhận, đã đến bưu cục đích hoặc sẵn sàng lấy", null);
        }

        // Khi shipper nhận đơn từ danh sách chưa gán, đặt trạng thái là READY_FOR_PICKUP
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        // Gán employee hiện tại khi shipper nhận đơn
        order.setEmployee(employee);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.READY_FOR_PICKUP, "Shipper nhận đơn (sẵn sàng lấy)");
        return new ApiResponse<>(true, "Nhận đơn thành công", null);
    }

    @Transactional
    public ApiResponse<String> unclaimOrder(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Nếu đây là đơn PICKUP_BY_COURIER thì kiểm tra dựa trên fromOffice
        if (order.getPickupType() != null && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            if (order.getFromOffice() == null || !Objects.equals(order.getFromOffice().getId(), officeId)) {
                return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
            }
        } else {
            if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
                return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
            }
        }

        if (order.getStatus() != OrderStatus.PICKED_UP && order.getStatus() != OrderStatus.READY_FOR_PICKUP && order.getStatus() != OrderStatus.PICKING_UP) {
            return new ApiResponse<>(false, "Chỉ có thể hủy nhận đơn ở trạng thái đã lấy hàng hoặc sẵn sàng lấy hoặc đang lấy", null);
        }

        // Trả về trạng thái trước đó: nếu là đơn lấy tại nhà, quay lại READY_FOR_PICKUP
        if (order.getPickupType() != null && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            order.setStatus(OrderStatus.READY_FOR_PICKUP);
        } else {
            if (order.getFromOffice() != null && order.getToOffice() != null
                    && Objects.equals(order.getFromOffice().getId(), order.getToOffice().getId())) {
                order.setStatus(OrderStatus.CONFIRMED);
            } else {
                order.setStatus(OrderStatus.AT_DEST_OFFICE);
            }
        }

        orderRepository.save(order);
        if (order.getStatus() == OrderStatus.AT_DEST_OFFICE) {
            try { autoAssignService.autoAssignOnArrival(order.getId()); } catch (Exception ignored) {}
        }
        return new ApiResponse<>(true, "Hủy nhận đơn thành công", null);
    }

    @Transactional
    public ApiResponse<String> recordDeliveryAttempt(Integer id, UpdateDeliveryStatusRequest request) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
            return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
        }
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể thao tác", null);
        }
        if (order.getStatus() == OrderStatus.PARTIAL_DELIVERY || order.getStatus() == OrderStatus.PARTIAL_RETURN) {
            return new ApiResponse<>(false, "Đơn đang giao 1 phần, không đi qua luồng delivery retry", null);
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            return new ApiResponse<>(false, "Chỉ có thể báo khi đơn đang giao", null);
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.RETURNING || order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.DELIVERY_FAILED_FINAL) {
            return new ApiResponse<>(false, "Trạng thái đơn hàng không hợp lệ", null);
        }

        String status = request != null && request.getStatus() != null ? request.getStatus().trim().toUpperCase() : null;
        if (status == null || status.isBlank()) {
            return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
        }

        if ("SUCCESS".equals(status)) {
            return handleDeliverySuccess(order, employee, employee.getUser(), request);
        }
        if (!"FAILED".equals(status)) {
            return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
        }
        if (request.getFailReason() == null || request.getFailReason().isBlank()) {
            return new ApiResponse<>(false, "failReason là bắt buộc", null);
        }

        return handleDeliveryFailure(order, employee, employee.getUser(), request);
    }

    @Transactional
    public ApiResponse<String> updateDeliveryStatus(Integer id, UpdateDeliveryStatusRequest request) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
            return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
        }

        OrderStatus newStatus;
        try {
            String rawStatus = request.getStatus().trim().toUpperCase();
            if ("DELIVERY_FAILED".equals(rawStatus) || "FAILED".equals(rawStatus)) {
                rawStatus = "FAILED_DELIVERY";
            }
            newStatus = OrderStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
        }

        User shipperUser = employee.getUser();

        if (request.getStatus() != null && request.getStatus().equalsIgnoreCase("DELIVERY_FAILED_FINAL")) {
            return handleDeliveryFailedFinal(order, employee, shipperUser, request);
        }

        // Luồng cơ bản: PICKED_UP -> DELIVERING -> DELIVERED / DELIVERY_RETRY
        if (newStatus == OrderStatus.DELIVERED) {
            return handleDeliverySuccess(order, employee, shipperUser, request);
        }

        if (newStatus == OrderStatus.DELIVERY_RETRY) {
            return handleDeliveryFailure(order, employee, shipperUser, request);
        }

        // Các trạng thái còn lại giữ hành vi cũ
        order.setStatus(newStatus);
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        orderRepository.save(order);
        applyVehicleWorkloadByStatus(order, employee, newStatus);

        String statusMessage = switch (newStatus) {
            case DELIVERING -> "Đã bắt đầu giao hàng";
            case DELIVERED -> "Đã giao hàng thành công";
            case FAILED_DELIVERY -> "Giao hàng thất bại";
            default -> "Trạng thái đơn hàng đã được cập nhật";
        };

        notificationService.create(
                statusMessage,
                "Đơn hàng " + order.getTrackingNumber() + " - " + statusMessage,
                "order_status_changed",
                shipperUser.getId(),
                null,
                "recipientaddress",
                order.getTrackingNumber()
        );

        switch (newStatus) {
            case DELIVERING -> saveHistory(order, OrderHistoryActionType.DELIVERING, "Shipper bắt đầu giao hàng");
            case DELIVERED -> saveHistory(order, OrderHistoryActionType.DELIVERED, "Shipper đã giao hàng thành công");
            case FAILED_DELIVERY -> saveHistory(order, OrderHistoryActionType.FAILED_DELIVERY, "Shipper đã trả đơn về bưu cục");
            case RETURNED -> saveHistory(order, OrderHistoryActionType.RETURNED, "Shipper đã trả đơn về bưu cục");
            default -> {
            }
        }

        return new ApiResponse<>(true, "Cập nhật trạng thái giao hàng thành công", null);
    }

    @Transactional
    public ApiResponse<String> markPickedUp(Integer id, PickedUpRequest request) {
        Employee employee = getCurrentEmployee();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Chỉ shipper đã nhận (employee) mới có thể đánh dấu đã lấy
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể xác nhận đã lấy hàng", null);
        }

        // Kiểm tra trạng thái phù hợp (đang lấy hàng)
        if (order.getStatus() != OrderStatus.PICKING_UP
                && order.getStatus() != OrderStatus.READY_FOR_PICKUP
                && order.getStatus() != OrderStatus.PICKUP_SUCCESS) {
            return new ApiResponse<>(false, "Chỉ có thể xác nhận đã lấy khi đơn ở trạng thái đang lấy, sẵn sàng lấy hoặc đã ghi nhận lấy hàng", null);
        }

        order.setStatus(OrderStatus.PICKED_UP);
        try {
            order.setDeliveredAt(LocalDateTime.now());
        } catch (Exception e) { throw e; }

        // Ghi lại notes / ảnh / vị trí nếu có (lưu tạm vào notes để không thay đổi schema)
        if (request != null) {
            String extra = "";
            if (request.getLatitude() != null && request.getLongitude() != null) {
                extra += "Location:" + request.getLatitude() + "," + request.getLongitude() + ";";
            }
            if (request.getPhotoUrl() != null) {
                extra += "Photo:" + request.getPhotoUrl() + ";";
            }
            if (request.getNotes() != null) {
                extra += "Notes:" + request.getNotes();
            }
            if (!extra.isBlank()) {
                String old = order.getNotes() != null ? order.getNotes() + "\n" : "";
                order.setNotes(old + extra);
            }
        }

        orderRepository.save(order);
        applyVehicleWorkloadByStatus(order, employee, OrderStatus.PICKED_UP);
        saveHistory(order, OrderHistoryActionType.PICKED_UP, "Shipper xác nhận đã lấy hàng");

        // Gửi notification đơn giản
        try {
            notificationService.create("Đã lấy hàng", "Đơn " + order.getTrackingNumber() + " đã được shipper xác nhận lấy", "order_picked_up", employee.getUser().getId(), null, "recipientaddress", order.getTrackingNumber());
        } catch (Exception e) { throw e; }

        return new ApiResponse<>(true, "Xác nhận đã lấy hàng thành công", null);
    }

    @Transactional
    public ApiResponse<String> deliverToOrigin(Integer id, DeliverOriginRequest request) {
        Employee employee = getCurrentEmployee();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra shipper được gán
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể xác nhận giao tới bưu cục", null);
        }

        // Chỉ hợp lệ nếu đã pick up
        if (order.getStatus() != OrderStatus.PICKED_UP && order.getStatus() != OrderStatus.PICKING_UP) {
            return new ApiResponse<>(false, "Chỉ có thể nộp hàng vào bưu cục khi trạng thái là ĐÃ LẤY hoặc ĐANG LẤY", null);
        }

        // Nếu request chỉ định officeId thì dùng office đó
        try {
            if (request != null && request.getOfficeId() != null) {
                officeRepository.findById(request.getOfficeId()).ifPresent(order::setToOffice);
            }
        } catch (Exception e) { throw e; }

        order.setStatus(OrderStatus.AT_ORIGIN_OFFICE);
        orderRepository.save(order);

        saveHistory(order, OrderHistoryActionType.IMPORTED, "Shipper nộp hàng tại bưu cục nguồn");

        try { autoAssignService.autoAssignOnArrival(order.getId()); } catch (Exception e) { throw e; }

        try {
            notificationService.create("Đã đến bưu cục", "Đơn " + order.getTrackingNumber() + " đã được nộp tại bưu cục", "order_at_origin_office", employee.getUser().getId(), null, "recipientaddress", order.getTrackingNumber());
        } catch (Exception e) { throw e; }

        return new ApiResponse<>(true, "Đã nộp hàng tại bưu cục", null);
    }

    public ApiResponse<Map<String, Object>> getDeliveryHistory(int page, int limit, String status) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();
        Integer shipperUserId = employee.getUser().getId();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "deliveredAt"));

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));
            predicates.add(root.get("status").in(
                    OrderStatus.DELIVERED,
                    OrderStatus.DELIVERY_RETRY,
                    OrderStatus.AT_DEST_OFFICE,
                    OrderStatus.DELIVERY_FAILED_FINAL,
                    OrderStatus.RETURNING,
                    OrderStatus.RETURNED
            ));
            predicates.add(cb.equal(root.get("employee").get("id"), employee.getId()));

            if (status != null && !status.isBlank()) {
                try {
                    OrderStatus os = OrderStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), os));
                } catch (IllegalArgumentException ignored) {
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<Order> orderRows = new ArrayList<>(orderPage.getContent());
        Set<Integer> existingOrderIds = orderRows.stream().map(Order::getId).collect(Collectors.toSet());

        List<DeliveryAttempt> failedAttempts = deliveryAttemptRepository.findAll().stream()
                .filter(attempt -> attempt.getShipper() != null
                        && attempt.getShipper().getId() != null
                        && attempt.getShipper().getId().equals(shipperUserId)
                        && attempt.getStatus() == com.logistics.enums.DeliveryAttemptStatus.FAILED)
                .filter(attempt -> attempt.getOrder() != null)
                .filter(attempt -> attempt.getOrder().getToOffice() != null
                        && attempt.getOrder().getToOffice().getId() != null
                        && attempt.getOrder().getToOffice().getId().equals(officeId))
                .filter(attempt -> {
                    if (status == null || status.isBlank()) {
                        return true;
                    }
                    try {
                        OrderStatus os = OrderStatus.valueOf(status.toUpperCase());
                        return os == OrderStatus.DELIVERY_FAILED_FINAL || os == OrderStatus.FAILED_DELIVERY;
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                })
                .sorted((a, b) -> {
                    if (a.getAttemptedAt() == null && b.getAttemptedAt() == null) return 0;
                    if (a.getAttemptedAt() == null) return 1;
                    if (b.getAttemptedAt() == null) return -1;
                    return b.getAttemptedAt().compareTo(a.getAttemptedAt());
                })
                .toList();

        Map<Integer, DeliveryAttempt> firstFailedAttemptByOrderId = new LinkedHashMap<>();
        for (DeliveryAttempt attempt : failedAttempts) {
            Integer orderId = attempt.getOrder().getId();
            if (orderId != null && !firstFailedAttemptByOrderId.containsKey(orderId)) {
                firstFailedAttemptByOrderId.put(orderId, attempt);
                if (!existingOrderIds.contains(orderId)) {
                    orderRows.add(attempt.getOrder());
                    existingOrderIds.add(orderId);
                }
            }
        }

        List<Map<String, Object>> orders = orderRows.stream()
                .map(order -> {
                    Map<String, Object> mapped = mapOrderDetail(order);
                    if (firstFailedAttemptByOrderId.containsKey(order.getId()) && order.getStatus() != OrderStatus.DELIVERED) {
                        DeliveryAttempt attempt = firstFailedAttemptByOrderId.get(order.getId());
                        mapped.put("status", OrderStatus.DELIVERY_FAILED_FINAL.name());
                        mapped.put("displayDate", attempt.getAttemptedAt());
                    } else {
                        mapped.put("displayDate", order.getDeliveredAt());
                    }
                    return mapped;
                })
                .toList();

        long deliveredCount = orderRows.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long failedCount = firstFailedAttemptByOrderId.size();

        Pagination pagination = new Pagination(
                orders.size(),
                page,
                limit,
                Math.max(1, (int) Math.ceil((double) orders.size() / limit))
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssigned", orders.size());
        stats.put("inProgress", 0);
        stats.put("delivered", deliveredCount);
        stats.put("failed", failedCount);
        int codCollectedHistory = paymentSubmissionRepository
            .findByShipperIdAndStatusIn(employee.getUser().getId(),
                    List.of(PaymentSubmissionStatus.PENDING))
            .stream()
            .mapToInt(ps -> ps.getActualAmount().intValue())
            .sum();
        stats.put("codCollected", codCollectedHistory);

        Map<String, Object> result = new HashMap<>();
        result.put("orders", orders);
        result.put("pagination", pagination);
        result.put("stats", stats);

        return new ApiResponse<>(true, "Lấy lịch sử giao hàng thành công", result);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createIncidentReport(CreateIncidentReportRequest request) {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        IncidentReport incident = new IncidentReport();
        incident.setOrder(order);
        incident.setShipper(shipperUser);

        if (request.getIncidentType() != null) {
            incident.setIncidentType(IncidentType.valueOf(request.getIncidentType().toUpperCase()));
        } else {
            incident.setIncidentType(IncidentType.OTHER);
        }

        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            incident.setImages(request.getImages());
        }

        if (request.getPriority() != null) {
            incident.setPriority(IncidentPriority.valueOf(request.getPriority().toUpperCase()));
        } else {
            incident.setPriority(IncidentPriority.MEDIUM);
        }

        incident.setStatus(IncidentStatus.PENDING);

        // Gán office dựa trên bản ghi employee hiện tại của shipper (role SHIPPER)
        try {
            List<Employee> employees = employeeRepository.findByUserId(shipperUser.getId());
            Employee matched = employees.stream()
                    .filter(e -> e.getAccountRole() != null && e.getAccountRole().getRole() != null
                            && e.getAccountRole().getRole().getName() != null
                            && e.getAccountRole().getRole().getName().equalsIgnoreCase("SHIPPER")
                            && (e.getStatus() != null && (e.getStatus().name().equals("ACTIVE") || e.getStatus().name().equals("INACTIVE"))))
                    .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .orElse(null);
            if (matched != null) {
                incident.setOffice(matched.getOffice());
            }
        } catch (Exception ex) {
            System.err.println("Cảnh báo: không thể tra cứu văn phòng (office) của nhân viên shipper: " + ex.getMessage());
        }

        IncidentReport saved = incidentReportRepository.save(incident);
        Map<String, Object> data = new HashMap<>();
        data.put("id", saved.getId());

        return new ApiResponse<>(true, "Tạo báo cáo sự cố thành công", data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createIncidentReport(Integer orderId, String incidentType, String title, String description, String priority, org.springframework.web.multipart.MultipartFile[] images) {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        IncidentReport incident = new IncidentReport();
        incident.setOrder(order);
        incident.setShipper(shipperUser);

        if (incidentType != null) {
            try {
                incident.setIncidentType(IncidentType.valueOf(incidentType.toUpperCase()));
            } catch (Exception e) {
                incident.setIncidentType(IncidentType.OTHER);
            }
        } else {
            incident.setIncidentType(IncidentType.OTHER);
        }

        incident.setTitle(title);
        incident.setDescription(description);

        if (priority != null) {
            try {
                incident.setPriority(IncidentPriority.valueOf(priority.toUpperCase()));
            } catch (Exception e) {
                incident.setPriority(IncidentPriority.MEDIUM);
            }
        } else {
            incident.setPriority(IncidentPriority.MEDIUM);
        }

        incident.setStatus(IncidentStatus.PENDING);

        // upload images to Cloudinary
        if (images != null && images.length > 0) {
            List<String> urls = new ArrayList<>();
            try {
                for (org.springframework.web.multipart.MultipartFile f : images) {
                    if (f != null && !f.isEmpty()) {
                        try {
                            ObjectUtils a = null;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> uploadResult = (Map<String, Object>) this.cloudinary.uploader().upload(f.getBytes(), ObjectUtils.asMap("folder", "incident_reports", "resource_type", "image"));
                            if (uploadResult != null && uploadResult.get("secure_url") != null) {
                                urls.add(uploadResult.get("secure_url").toString());
                            }
                        } catch (Exception ex) {
                        }
                    }
                }
            } catch (Exception e) { }

            if (!urls.isEmpty()) {
                incident.setImages(urls);
            }
        }

        // Gán office dựa trên bản ghi employee hiện tại của shipper (role SHIPPER)
        try {
            List<Employee> employees = employeeRepository.findByUserId(shipperUser.getId());
            Employee matched = employees.stream()
                    .filter(e -> e.getAccountRole() != null && e.getAccountRole().getRole() != null
                            && e.getAccountRole().getRole().getName() != null
                            && e.getAccountRole().getRole().getName().equalsIgnoreCase("SHIPPER")
                            && (e.getStatus() != null && (e.getStatus().name().equals("ACTIVE") || e.getStatus().name().equals("INACTIVE"))))
                    .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .orElse(null);
            if (matched != null) {
                incident.setOffice(matched.getOffice());
            }
        } catch (Exception ex) {
            System.err.println("Cảnh báo: không thể tra cứu văn phòng (office) của nhân viên shipper: " + ex.getMessage());
        }

        IncidentReport saved = incidentReportRepository.save(incident);
        Map<String, Object> data = new HashMap<>();
        data.put("id", saved.getId());

        return new ApiResponse<>(true, "Tạo báo cáo sự cố thành công", data);
    }

    public ApiResponse<List<Map<String, Object>>> listIncidentReports() {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        List<IncidentReport> incidents = incidentReportRepository.findAll()
                .stream()
                .filter(ir -> ir.getShipper() != null && Objects.equals(ir.getShipper().getId(), shipperUser.getId()))
                .toList();

        List<Map<String, Object>> data = incidents.stream()
                .map(this::mapIncident)
                .toList();

        return new ApiResponse<>(true, "Lấy danh sách báo cáo sự cố thành công", data);
    }

    public ApiResponse<Map<String, Object>> getIncidentDetail(Integer id) {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        IncidentReport incident = incidentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo sự cố"));

        if (incident.getShipper() == null || !Objects.equals(incident.getShipper().getId(), shipperUser.getId())) {
            return new ApiResponse<>(false, "Không có quyền xem báo cáo này", null);
        }

        return new ApiResponse<>(true, "Lấy chi tiết báo cáo sự cố thành công", mapIncident(incident));
    }

    private Map<String, Object> mapOrderSummary(Order order) {
        String senderFullAddress = resolveSenderFullAddress(order);
        String recipientFullAddress = resolveRecipientFullAddress(order);

        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("trackingNumber", order.getTrackingNumber());
        map.put("senderName", order.getSenderName());
        map.put("senderPhone", order.getSenderPhone());
        map.put("senderAddress", senderFullAddress);
        map.put("senderFullAddress", senderFullAddress);
        if (order.getFromOffice() != null) {
            Map<String, Object> f = new HashMap<>();
            f.put("id", order.getFromOffice().getId());
            f.put("name", order.getFromOffice().getName());
            f.put("detail", order.getFromOffice().getDetail());
            f.put("latitude", order.getFromOffice().getLatitude());
            f.put("longitude", order.getFromOffice().getLongitude());
            map.put("fromOffice", f);
        } else {
            map.put("fromOffice", null);
        }
        map.put("recipientName", order.getRecipientName());
        map.put("recipientPhone", order.getRecipientPhone());
        map.put("recipientAddress", recipientFullAddress);
        map.put("recipientFullAddress", recipientFullAddress);
        map.put("payer", order.getPayer() != null ? order.getPayer().name() : null);
        map.put("cod", order.getCod());
        map.put("codAmount", order.getCod()); 
        map.put("shippingFee", order.getShippingFee());
        map.put("status", order.getStatus().name()); 
        map.put("priority", "normal");
        map.put("serviceType", order.getServiceType() != null ? order.getServiceType().getName() : null);
        return map;
    }

    private Map<String, Object> mapOrderDetail(Order order) {
        String senderFullAddress = resolveSenderFullAddress(order);
        String recipientFullAddress = resolveRecipientFullAddress(order);

        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("trackingNumber", order.getTrackingNumber());
        map.put("senderName", order.getSenderName());
        map.put("senderPhone", order.getSenderPhone());
        map.put("senderAddress", senderFullAddress);
        map.put("senderFullAddress", senderFullAddress);
        map.put("recipientName", order.getRecipientName());
        map.put("recipientPhone", order.getRecipientPhone());
        map.put("recipientAddress", recipientFullAddress);
        map.put("recipientFullAddress", recipientFullAddress);
        map.put("payer", order.getPayer() != null ? order.getPayer().name() : null);
        map.put("weight", order.getWeight());
        map.put("cod", order.getCod());
        map.put("codStatus", order.getCodStatus() != null ? order.getCodStatus().name() : null);
        // Bao gồm các bản ghi PaymentSubmission liên quan đến đơn hàng này
        try {
            List<PaymentSubmission> submissions = paymentSubmissionRepository.findByOrderIdWithItems(order.getId());
            List<Map<String, Object>> subs = submissions.stream().map(ps -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", ps.getId());
                m.put("code", ps.getCode());
                m.put("systemAmount", ps.getSystemAmount() != null ? ps.getSystemAmount().intValue() : 0);
                m.put("actualAmount", ps.getActualAmount() != null ? ps.getActualAmount().intValue() : 0);
                m.put("status", ps.getStatus() != null ? ps.getStatus().name() : null);
                m.put("notes", ps.getNotes());
                m.put("paidAt", ps.getPaidAt());
                return m;
            }).toList();
            map.put("paymentSubmissions", subs);
            } catch (Exception e) {
            map.put("paymentSubmissions", Collections.emptyList());
        }
        map.put("codAmount", order.getCod());
        map.put("shippingFee", order.getShippingFee());
        map.put("totalAmount", order.getTotalFee());
        map.put("status", order.getStatus().name());
        map.put("priority", "normal");
        map.put("serviceType", order.getServiceType() != null ? order.getServiceType().getName() : null);
        map.put("deliveryTime", order.getServiceType() != null ? order.getServiceType().getDeliveryTime() : null);
        map.put("notes", order.getNotes());
        map.put("createdAt", order.getCreatedAt());
        map.put("deliveredAt", order.getDeliveredAt());
        if (order.getFromOffice() != null) {
            Map<String, Object> f = new HashMap<>();
            f.put("id", order.getFromOffice().getId());
            f.put("name", order.getFromOffice().getName());
            f.put("detail", order.getFromOffice().getDetail());
            f.put("latitude", order.getFromOffice().getLatitude());
            f.put("longitude", order.getFromOffice().getLongitude());
            map.put("fromOffice", f);
        } else {
            map.put("fromOffice", null);
        }
        try {
            List<Map<String, Object>> attempts = pickupAttemptRepository
                .findByOrderIdOrderByAttemptedAtDesc(order.getId())
                .stream()
                .map(attempt -> {
                    Map<String, Object> a = new HashMap<>();
                    a.put("attemptNumber", attempt.getAttemptNumber());
                    a.put("status", attempt.getStatus() != null ? attempt.getStatus().name() : null);
                    a.put("failReason", attempt.getFailReason() != null ? attempt.getFailReason().name() : null);
                    a.put("note", attempt.getNote());
                    a.put("attemptedAt", attempt.getAttemptedAt());
                    a.put("shipperName", attempt.getShipper() != null ? attempt.getShipper().getFullName() : null);
                    return a;
                }).toList();
            map.put("pickupAttempts", attempts);
        } catch (Exception e) {
            map.put("pickupAttempts", Collections.emptyList());
        }
        try {
            map.put("maxPickupAttempts", configService.getInt("MAX_PICKUP_ATTEMPTS"));
        } catch (Exception e) {
            map.put("maxPickupAttempts", null);
        }
        // Bao gồm danh sách sản phẩm kèm số lượng đã giao / trả
        try {
            List<OrderProduct> ops = orderProductRepository.findByOrderIdWithProduct(order.getId());
            List<Map<String, Object>> prodMaps = ops.stream().map(op -> {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id", op.getId());
                pm.put("productId", op.getProduct() != null ? op.getProduct().getId() : null);
                pm.put("productName", op.getProduct() != null ? op.getProduct().getName() : null);
                pm.put("quantity", op.getQuantity());
                pm.put("price", op.getPrice());
                pm.put("deliveredQuantity", op.getDeliveredQuantity() == null ? 0 : op.getDeliveredQuantity());
                pm.put("returnedQuantity", op.getReturnedQuantity() == null ? 0 : op.getReturnedQuantity());
                pm.put("remaining", (op.getQuantity() == null ? 0 : op.getQuantity()) - (op.getDeliveredQuantity() == null ? 0 : op.getDeliveredQuantity()) - (op.getReturnedQuantity() == null ? 0 : op.getReturnedQuantity()));
                return pm;
            }).toList();
            map.put("orderProducts", prodMaps);
        } catch (Exception e) {
            map.put("orderProducts", Collections.emptyList());
        }
        return map;
    }

    private Map<String, Object> mapIncident(IncidentReport incident) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", incident.getId());
        map.put("orderId", incident.getOrder() != null ? incident.getOrder().getId() : null);
        map.put("trackingNumber", incident.getOrder() != null ? incident.getOrder().getTrackingNumber() : null);
        map.put("shipperId", incident.getShipper() != null ? incident.getShipper().getId() : null);
        map.put("handledBy", incident.getHandler() != null ? incident.getHandler().getId() : null);
        map.put("incidentType", incident.getIncidentType() != null ? incident.getIncidentType().name() : null);
        map.put("title", incident.getTitle());
        map.put("description", incident.getDescription());
        map.put("priority", incident.getPriority() != null ? incident.getPriority().name() : null);
        map.put("status", incident.getStatus() != null ? incident.getStatus().name() : null);
        map.put("createdAt", incident.getCreatedAt());
        map.put("handledAt", incident.getHandledAt());
        map.put("officeId", incident.getOffice() != null ? incident.getOffice().getId() : null);
        return map;
    }

    // Tạo bản ghi đối soát tiền mặt (COD + phí dịch vụ) sau khi giao/hoàn
    private boolean isCodPaymentSubmission(Order order, PaymentSubmission submission) {
        if (order == null || submission == null) {
            return false;
        }

        if (submission.getItems() != null && !submission.getItems().isEmpty()) {
            if (order.getCod() == null || order.getCod() <= 0) {
                return false;
            }
            BigDecimal submissionAmount = submission.getSystemAmount() != null ? submission.getSystemAmount() : submission.getActualAmount();
            if (submissionAmount == null) {
                return false;
            }
            return submissionAmount.compareTo(BigDecimal.valueOf(order.getCod())) == 0;
        }

        if (order.getCod() == null || order.getCod() <= 0) {
            return false;
        }

        BigDecimal systemAmount = submission.getSystemAmount();
        BigDecimal actualAmount = submission.getActualAmount();
        BigDecimal codAmount = BigDecimal.valueOf(order.getCod());
        return (systemAmount != null && systemAmount.compareTo(codAmount) == 0)
                || (actualAmount != null && actualAmount.compareTo(codAmount) == 0)
                || (submission.getNotes() != null && submission.getNotes().toUpperCase().contains("COD"));
    }

    private boolean hasExistingCodSubmission(Order order) {
        if (order == null) {
            return false;
        }
        List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
        if (existing == null || existing.isEmpty()) {
            return false;
        }
        for (PaymentSubmission submission : existing) {
            if (isCodPaymentSubmission(order, submission)) {
                return true;
            }
        }
        return false;
    }

    private void createPaymentSubmission(Order order, User shipperUser, int amount, String note) {
        if (amount <= 0) return;

        // Lock recipientaddress to prevent duplicate COD across instances
        try {
            Optional<Order> locked = orderRepository.findByIdForUpdate(order.getId());
            if (locked.isPresent()) order = locked.get();
        } catch (Exception e) {
        }

        // Prevent creating COD when recipientaddress is already submitted/received
        if (order.getCod() != null && order.getCod() > 0
                && (order.getCodStatus() == OrderCodStatus.SUBMITTED || order.getCodStatus() == OrderCodStatus.RECEIVED)) {
            // Attempt to create COD submission but recipientaddress codStatus prevents it
            return;
        }

        boolean isCod = order.getCod() != null && amount == order.getCod();

        // If there's already a pending/in-batch submission, try to reuse it
        List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
        if (existing != null) {
            for (PaymentSubmission ex : existing) {
                if (ex.getStatus() == PaymentSubmissionStatus.PENDING) {
                    if (isCod) {
                        try {
                            List<OrderProduct> products = orderProductRepository.findByOrderIdWithProduct(order.getId());
                            List<PaymentSubmissionItem> items = new ArrayList<>();
                            BigDecimal codSum = BigDecimal.ZERO;
                            if (products != null) {
                                for (OrderProduct p : products) {
                                    int delivered = p.getDeliveredQuantity() == null ? 0 : p.getDeliveredQuantity();
                                    if (delivered <= 0) continue;
                                    BigDecimal unit = BigDecimal.valueOf(p.getPrice() == null ? 0 : p.getPrice());
                                    BigDecimal total = unit.multiply(BigDecimal.valueOf(delivered));
                                    codSum = codSum.add(total);
                                    PaymentSubmissionItem it = new PaymentSubmissionItem();
                                    it.setOrderProduct(p);
                                    it.setQuantity(delivered);
                                    it.setUnitAmount(unit);
                                    it.setTotalAmount(total);
                                    it.setPaymentSubmission(ex);
                                    items.add(it);
                                }
                            }
                            // Fallback: nếu chưa có deliveredQuantity thì vẫn lấy COD theo đơn để không ra 0.
                            if (codSum.compareTo(BigDecimal.ZERO) <= 0 && order.getCod() != null && order.getCod() > 0) {
                                codSum = BigDecimal.valueOf(order.getCod());
                            }

                            if (!items.isEmpty()) {
                                // Idempotency: if existing submission already matches computed items, avoid replacing
                                boolean same = ex.getSystemAmount() != null && ex.getSystemAmount().compareTo(codSum) == 0
                                        && ex.getItems() != null && ex.getItems().size() == items.size();
                                if (same) {
                                    for (PaymentSubmissionItem ei : ex.getItems()) {
                                        boolean match = items.stream().anyMatch(it -> it.getOrderProduct() != null && ei.getOrderProduct() != null
                                                && ei.getOrderProduct().getId() != null && ei.getOrderProduct().getId().equals(it.getOrderProduct().getId())
                                                && ei.getQuantity() != null && ei.getQuantity().equals(it.getQuantity()));
                                        if (!match) { same = false; break; }
                                    }
                                }
                                if (same) {
                                    ex.setShipper(shipperUser);
                                    ex.setNotes(note);
                                    ex.setStatus(PaymentSubmissionStatus.PENDING);
                                    paymentSubmissionRepository.save(ex);
                                    order.setCodStatus(OrderCodStatus.PENDING);
                                    orderRepository.save(order);
                                } else {
                                    // replace items atomically
                                    ex.getItems().clear();
                                    for (PaymentSubmissionItem it : items) {
                                        it.setPaymentSubmission(ex);
                                        ex.getItems().add(it);
                                    }
                                    ex.setSystemAmount(codSum);
                                    ex.setActualAmount(codSum);
                                    ex.setShipper(shipperUser);
                                    ex.setNotes(note);
                                    ex.setStatus(PaymentSubmissionStatus.PENDING);
                                    paymentSubmissionRepository.save(ex);
                                    order.setCodStatus(OrderCodStatus.PENDING);
                                    orderRepository.save(order);
                                }
                            } else if (codSum.compareTo(BigDecimal.ZERO) > 0) {
                                // Không có item chi tiết nhưng vẫn phải cập nhật số tiền COD đúng theo đơn.
                                ex.setSystemAmount(codSum);
                                ex.setActualAmount(codSum);
                                ex.setShipper(shipperUser);
                                ex.setNotes(note);
                                ex.setStatus(PaymentSubmissionStatus.PENDING);
                                paymentSubmissionRepository.save(ex);
                                order.setCodStatus(OrderCodStatus.PENDING);
                                orderRepository.save(order);
                            }
                        } catch (Exception e) {
                        }
                    }
                    return;
                }
            }
        }

        // Create a new submission
        PaymentSubmission submission = new PaymentSubmission();
        submission.setOrder(order);
        submission.setShipper(shipperUser);
        submission.setNotes(note);
        submission.setStatus(PaymentSubmissionStatus.PENDING);

        List<PaymentSubmissionItem> items = new ArrayList<>();

        if (isCod) {
                            List<OrderProduct> products = orderProductRepository.findByOrderIdWithProduct(order.getId());
            BigDecimal codSum = BigDecimal.ZERO;
            if (products != null) {
                for (OrderProduct p : products) {
                    int delivered = p.getDeliveredQuantity() == null ? 0 : p.getDeliveredQuantity();
                    if (delivered <= 0) continue;
                    BigDecimal unit = BigDecimal.valueOf(p.getPrice() == null ? 0 : p.getPrice());
                    BigDecimal total = unit.multiply(BigDecimal.valueOf(delivered));
                    codSum = codSum.add(total);
                    PaymentSubmissionItem it = new PaymentSubmissionItem();
                    it.setOrderProduct(p);
                    it.setQuantity(delivered);
                    it.setUnitAmount(unit);
                    it.setTotalAmount(total);
                    it.setPaymentSubmission(submission);
                    items.add(it);
                }
            }
            // Fallback: nếu chưa có luồng cập nhật deliveredQuantity (giao thành công nhanh từ màn chi tiết)
            // thì vẫn phải ghi nhận COD theo giá trị đơn hàng để tránh phát sinh COD = 0.
            if (codSum.compareTo(BigDecimal.ZERO) <= 0 && order.getCod() != null && order.getCod() > 0) {
                codSum = BigDecimal.valueOf(order.getCod());
            }
            submission.setSystemAmount(codSum);
            submission.setActualAmount(codSum);
        } else {
            submission.setSystemAmount(BigDecimal.valueOf(amount));
            submission.setActualAmount(BigDecimal.valueOf(amount));
        }

        submission.setItems(items);
        submission = paymentSubmissionRepository.save(submission);

        String submissionCode = "SUB_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + submission.getId();
        submission.setCode(submissionCode);
        paymentSubmissionRepository.save(submission);

        if (isCod) {
            try {
                order.setCodStatus(OrderCodStatus.PENDING);
                orderRepository.save(order);
            } catch (Exception e) {
            }
        }
    }

    // Tạo bản ghi ghi nhận hoàn tiền COD khi trả hàng (giữ dấu vết giao dịch)
    private void createReturnPaymentSubmission(Order order, User shipperUser, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        PaymentSubmission submission = new PaymentSubmission();
        submission.setOrder(order);
        // Lưu dưới dạng số âm để biểu thị trả lại
        submission.setSystemAmount(amount.negate());
        submission.setActualAmount(amount.negate());
        submission.setStatus(PaymentSubmissionStatus.ADJUSTED);
        submission.setShipper(shipperUser);
        submission.setNotes(note);

        submission = paymentSubmissionRepository.save(submission);
        String submissionCode = "RET_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + submission.getId();
        submission.setCode(submissionCode);
        paymentSubmissionRepository.save(submission);

        // Sau khi hoàn COD, cập nhật trạng thái COD và trạng thái thanh toán của đơn
        try {
            order.setCodStatus(OrderCodStatus.NONE);
            order.setPaymentStatus(OrderPaymentStatus.REFUNDED);
            orderRepository.save(order);
        } catch (Exception e) {
            throw e;
        }
    }

    private int getMaxDeliveryAttempts() {
        try {
            return configService.getInt("MAX_DELIVERY_ATTEMPTS");
        } catch (Exception e) {
            return 3;
        }
    }

    private long countFailedDeliveryAttempts(Integer orderId) {
        return deliveryAttemptRepository.countByOrderIdAndStatus(orderId, com.logistics.enums.DeliveryAttemptStatus.FAILED);
    }

    private String buildDeliveryAttemptNote(String reason, String note, int attemptNumber, int maxAttempts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Giao thất bại lần ").append(attemptNumber).append("/").append(maxAttempts);
        if (reason != null && !reason.isBlank()) {
            sb.append(": ").append(reason);
        }
        if (note != null && !note.isBlank()) {
            sb.append(" - ").append(note);
        }
        return sb.toString();
    }

    private ApiResponse<String> handleDeliverySuccess(Order order, Employee employee, User shipperUser, UpdateDeliveryStatusRequest request) {
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);
        applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERED);

        int cashCollected = 0;
        if (order.getPayer() == OrderPayerType.CUSTOMER) {
            cashCollected += order.getShippingFee() != null ? order.getShippingFee() : 0;
        }
        if (cashCollected > 0) {
            createPaymentSubmission(order, shipperUser, cashCollected, "Đối soát sau khi giao thành công");
        }
        if (order.getCod() != null && order.getCod() > 0) {
            List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
            boolean hasPositive = existing.stream().anyMatch(ps -> ps.getActualAmount() != null && ps.getActualAmount().compareTo(BigDecimal.ZERO) > 0);
            if (!hasPositive) {
                createPaymentSubmission(order, shipperUser, order.getCod(), "Thu COD sau khi giao");
                order.setCodStatus(OrderCodStatus.PENDING);
                orderRepository.save(order);
            }
        }

        notificationService.create(
                "Đã giao hàng thành công",
                "Đơn hàng " + order.getTrackingNumber() + " - Đã giao hàng thành công",
                "order_status_changed",
                shipperUser.getId(),
                null,
                "recipientaddress",
                order.getTrackingNumber()
        );
        saveHistory(order, OrderHistoryActionType.DELIVERED, "Shipper đã giao hàng thành công");
        return new ApiResponse<>(true, "Cập nhật trạng thái giao hàng thành công", null);
    }

    private ApiResponse<String> handleDeliveryFailure(Order order, Employee employee, User shipperUser, UpdateDeliveryStatusRequest request) {
        if (order.getStatus() != OrderStatus.DELIVERING) {
            return new ApiResponse<>(false, "Chỉ có thể báo giao thất bại khi đơn đang giao", null);
        }
        int maxAttempts = getMaxDeliveryAttempts();
        long failedCountBefore = countFailedDeliveryAttempts(order.getId());
        int attemptNumber = (int) failedCountBefore + 1;
        boolean finalFail = attemptNumber >= maxAttempts;

        String reason = request.getFailReason();
        String note = request.getNotes();
        if (reason == null || reason.isBlank()) {
            return new ApiResponse<>(false, "failReason là bắt buộc", null);
        }

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setOrder(order);
        attempt.setShipper(shipperUser);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStatus(com.logistics.enums.DeliveryAttemptStatus.FAILED);
        attempt.setFailReason(com.logistics.enums.DeliveryFailReason.valueOf(reason.trim().toUpperCase()));
        attempt.setNote(note);
        deliveryAttemptRepository.save(attempt);

        if (finalFail) {
            order.setStatus(OrderStatus.DELIVERY_FAILED_FINAL);
            orderRepository.save(order);
            saveHistory(order, OrderHistoryActionType.DELIVERY_FAILED_FINAL, "Giao thất bại quá số lần cho phép");
            return new ApiResponse<>(true, "Đã ghi nhận giao thất bại cuối cùng", null);
        }

        order.setStatus(OrderStatus.DELIVERY_RETRY);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.DELIVERY_RETRY, buildDeliveryAttemptNote(reason, note, attemptNumber, maxAttempts));
        return new ApiResponse<>(true, "Đã ghi nhận giao thất bại", null);
    }

    @Transactional
    public ApiResponse<String> returnFailedToOffice(Integer id) {
        Employee employee = getCurrentEmployee();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể thao tác", null);
        }
        if (order.getStatus() != OrderStatus.DELIVERY_RETRY) {
            return new ApiResponse<>(false, "Chỉ có thể nộp hàng về bưu cục khi đơn ở trạng thái chờ giao lại", null);
        }

        applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERY_RETRY);
        order.setStatus(OrderStatus.AT_DEST_OFFICE);
        order.setEmployee(null);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.AT_DEST_OFFICE, "Shipper đã nộp hàng giao thất bại về bưu cục đích");

        return new ApiResponse<>(true, "Đã nộp hàng thất bại về bưu cục", null);
    }

    private ApiResponse<String> handleDeliveryFailedFinal(Order order, Employee employee, User shipperUser, UpdateDeliveryStatusRequest request) {
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể thao tác", null);
        }
        if (order.getStatus() != OrderStatus.DELIVERING && order.getStatus() != OrderStatus.DELIVERY_FAILED_FINAL) {
            return new ApiResponse<>(false, "Chỉ có thể báo giao thất bại cuối cùng khi đơn đang giao", null);
        }
        order.setStatus(OrderStatus.DELIVERY_FAILED_FINAL);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.DELIVERY_FAILED_FINAL, "Giao thất bại quá số lần cho phép");
        return new ApiResponse<>(true, "Đã ghi nhận giao thất bại cuối cùng", null);
    }

    private String resolveSenderFullAddress(Order order) {
        if (order == null) return null;
        if (order.getSenderFullAddress() != null && !order.getSenderFullAddress().isBlank()) {
            return order.getSenderFullAddress();
        }
        return resolveAddressFromEntity(order.getSenderAddress());
    }

    private String resolveRecipientFullAddress(Order order) {
        if (order == null) return null;
        if (order.getRecipientFullAddress() != null && !order.getRecipientFullAddress().isBlank()) {
            return order.getRecipientFullAddress();
        }
        return resolveAddressFromEntity(order.getRecipientAddress());
    }

    private String resolveAddressFromEntity(Address address) {
        if (address == null) return null;
        if (address.getFullAddress() != null && !address.getFullAddress().isBlank()) {
            return address.getFullAddress();
        }
        return buildAddressFromParts(address.getDetail(), address.getWardName(), address.getCityName());
    }

    private String buildAddressFromParts(String detail, String wardName, String cityName) {
        List<String> parts = new ArrayList<>();
        if (detail != null && !detail.isBlank()) parts.add(detail);
        if (wardName != null && !wardName.isBlank()) parts.add(wardName);
        if (cityName != null && !cityName.isBlank()) parts.add(cityName);
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    // Lộ trình giao hàng (ưu tiên tuyến AI đã xác nhận)
    public ApiResponse<Map<String, Object>> getDeliveryRoute() {
        Employee employee = getCurrentEmployee();

        List<AiRoutePlanRoute> aiRoutes = aiRoutePlanRouteRepository.findConfirmedRoutesForShipper(
                employee.getId(), AiRoutePlanStatus.CONFIRMED);
        if (!aiRoutes.isEmpty()) {
            return buildAiDeliveryRouteResponse(employee, aiRoutes.get(0));
        }

        Integer officeId = employee.getOffice().getId();

        Specification<Order> routeSpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));
            // Lấy tất cả đơn hàng ở trạng thái cần giao, không giới hạn ngày tạo
            predicates.add(root.get("status").in(
                    OrderStatus.AT_DEST_OFFICE,
                    OrderStatus.PICKED_UP,
                    OrderStatus.DELIVERING
            ));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Order> routeOrders = orderRepository.findAll(routeSpec, Sort.by(Sort.Direction.ASC, "createdAt"));

        int totalCOD = routeOrders.stream().mapToInt(o -> o.getCod() != null ? o.getCod() : 0).sum();

        List<Map<String, Object>> deliveryStops = routeOrders.stream().map(order -> {
            String recipientFullAddress = resolveRecipientFullAddress(order);
            Map<String, Object> stop = new HashMap<>();
            stop.put("id", order.getId());
            stop.put("trackingNumber", order.getTrackingNumber());
            stop.put("recipientName", order.getRecipientName());
            stop.put("recipientPhone", order.getRecipientPhone());
            stop.put("recipientAddress", recipientFullAddress);
            stop.put("recipientFullAddress", recipientFullAddress);
            stop.put("codAmount", order.getCod());
            stop.put("priority", order.getCod() > 1000000 ? "urgent" : "normal");
            stop.put("serviceType", order.getServiceType() != null ? order.getServiceType().getName() : "Tiêu chuẩn");
            stop.put("status", order.getStatus() == OrderStatus.DELIVERED ? "completed" :
                    (order.getStatus() == OrderStatus.DELIVERING || order.getStatus() == OrderStatus.PICKED_UP) ? "in_progress" : "pending");
            return stop;
        }).toList();

        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("id", 1);
        routeInfo.put("name", "Tuyến " + officeId);
        routeInfo.put("startLocation", employee.getOffice().getName());
        routeInfo.put("totalStops", routeOrders.size());
        routeInfo.put("completedStops", (int) routeOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count());
        routeInfo.put("totalDistance", 0); // TODO: Tính toán khoảng cách thực tế
        routeInfo.put("estimatedDuration", 0); // TODO: Tính toán thời gian ước tính
        routeInfo.put("totalCOD", totalCOD);
        routeInfo.put("status", "not_started");

        Map<String, Object> result = new HashMap<>();
        result.put("routeInfo", routeInfo);
        result.put("deliveryStops", deliveryStops);

        return new ApiResponse<>(true, "Lấy lộ trình giao hàng thành công", result);
    }

    private ApiResponse<Map<String, Object>> buildAiDeliveryRouteResponse(Employee employee, AiRoutePlanRoute aiRoute) {
        List<AiRoutePlanStop> stops = aiRoute.getStops();
        if (stops != null) {
            stops.sort(Comparator.comparing(AiRoutePlanStop::getStopSequence));
        } else {
            stops = List.of();
        }

        int totalCOD = stops.stream().mapToInt(s -> s.getCodAmount() != null ? s.getCodAmount() : 0).sum();

        List<Map<String, Object>> deliveryStops = stops.stream().map(stop -> {
            Order order = stop.getOrder();
            String recipientFullAddress = stop.getRecipientAddress() != null
                    ? stop.getRecipientAddress()
                    : (order != null ? resolveRecipientFullAddress(order) : "");
            Map<String, Object> m = new HashMap<>();
            m.put("id", order != null ? order.getId() : stop.getOrder().getId());
            m.put("trackingNumber", stop.getTrackingNumber());
            m.put("recipientName", stop.getRecipientName());
            m.put("recipientPhone", stop.getRecipientPhone());
            m.put("recipientAddress", recipientFullAddress);
            m.put("recipientFullAddress", recipientFullAddress);
            m.put("latitude", stop.getRecipientLatitude());
            m.put("longitude", stop.getRecipientLongitude());
            m.put("codAmount", stop.getCodAmount());
            m.put("priority", "HIGH".equalsIgnoreCase(stop.getPriority()) ? "urgent" : "normal");
            m.put("serviceType", order != null && order.getServiceType() != null
                    ? order.getServiceType().getName() : "Tiêu chuẩn");
            m.put("stopSequence", stop.getStopSequence());
            m.put("etaTime", stop.getEtaTime());
            m.put("etaMinutesFromStart", stop.getEtaMinutesFromStart());
            m.put("status", order != null && order.getStatus() == OrderStatus.DELIVERED ? "completed" : "pending");
            return m;
        }).toList();

        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("id", aiRoute.getId());
        routeInfo.put("planId", aiRoute.getPlan().getId());
        routeInfo.put("planCode", aiRoute.getPlan().getPlanCode());
        routeInfo.put("name", "Tuyến AI - " + aiRoute.getShipperName());
        routeInfo.put("startLocation", employee.getOffice().getName());
        routeInfo.put("totalStops", stops.size());
        routeInfo.put("completedStops", (int) stops.stream()
                .filter(s -> s.getOrder() != null && s.getOrder().getStatus() == OrderStatus.DELIVERED)
                .count());
        routeInfo.put("totalDistance", aiRoute.getEstimatedDistanceKm() != null
                ? aiRoute.getEstimatedDistanceKm().doubleValue() : 0);
        routeInfo.put("estimatedDuration", aiRoute.getEstimatedDurationMinutes() != null
                ? aiRoute.getEstimatedDurationMinutes().doubleValue() : 0);
        routeInfo.put("fuelCost", aiRoute.getFuelCost() != null ? aiRoute.getFuelCost().doubleValue() : 0);
        routeInfo.put("totalCOD", totalCOD);
        routeInfo.put("encodedPolyline", aiRoute.getEncodedPolyline());
        routeInfo.put("startTime", aiRoute.getStartTime());
        routeInfo.put("status", "ai_optimized");
        routeInfo.put("source", "AI");

        Map<String, Object> officeMap = new HashMap<>();
        officeMap.put("name", employee.getOffice().getName());
        officeMap.put("latitude", employee.getOffice().getLatitude());
        officeMap.put("longitude", employee.getOffice().getLongitude());

        Map<String, Object> result = new HashMap<>();
        result.put("routeInfo", routeInfo);
        result.put("deliveryStops", deliveryStops);
        result.put("office", officeMap);

        return new ApiResponse<>(true, "Lấy lộ trình AI đã tối ưu thành công", result);
    }

    public ApiResponse<String> startRoute(Integer routeId) {
        // TODO: Implement logic để đánh dấu route đã bắt đầu
        return new ApiResponse<>(true, "Đã bắt đầu tuyến giao hàng", null);
    }
}


