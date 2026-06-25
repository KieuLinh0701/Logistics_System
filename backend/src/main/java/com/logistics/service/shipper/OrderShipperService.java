package com.logistics.service.shipper;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.dto.ai.*;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.*;
import com.logistics.repository.*;
import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.request.shipper.*;
import com.logistics.response.NotificationResponse;
import com.logistics.response.Pagination;
import com.logistics.service.ai.AiServiceClient;
import com.logistics.service.assignment.AutoAssignService;
import com.logistics.service.common.ConfigService;
import com.logistics.service.common.NotificationService;
import com.logistics.service.common.OrderDestinationService;
import com.logistics.utils.OrderUtils;
import com.logistics.utils.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
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
    private AiRoutePlanStopRepository aiRoutePlanStopRepository;

    @Autowired
    private ShipperVehicleRepository shipperVehicleRepository;

    @Autowired
    private AiServiceClient aiServiceClient;

    @Autowired
    private OrderDestinationService orderDestinationService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentOrderRepository shipmentOrderRepository;

    @Autowired
    private ShipmentDeliveryService shipmentDeliveryService;

    private void saveHistory(Order order, OrderHistoryActionType action, String note) {
        // Backward-compat: tìm shipment active của order (nếu có) để set shipmentId
        com.logistics.entity.Shipment activeShipment = null;
        try {
            List<com.logistics.entity.Shipment> active = shipmentOrderRepository.findActiveShipmentsForOrder(order.getId());
            if (active != null && !active.isEmpty()) {
                activeShipment = active.get(0);
            }
        } catch (Exception ignored) {}
        saveHistory(order, activeShipment, action, note);
    }

    private void saveHistory(Order order, com.logistics.entity.Shipment shipment, OrderHistoryActionType action, String note) {
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setFromOffice(order.getToOffice());
        history.setToOffice(order.getToOffice());
        history.setShipment(shipment);
        history.setAction(action);
        history.setNote(note);
        orderHistoryRepository.save(history);
    }

    private Employee getCurrentEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.get(0);
    }

    /**
     * ==================================================================
     * Quick-claim cho accept-pickup flow.
     *
     * Mục tiêu: KHÔNG giữ pessimistic lock quá lâu.
     *   - Đọc order bằng findById thường (không lock).
     *   - Validate + update + save.
     *   - Trả về order đã update.
     *
     * Mọi logic nặng khác (findActiveShipments, insertPickupIntoShipment, saveHistory,
     * notification, AI route mirror) do caller xử lý SAU KHI method này commit.
     *
     * Race condition: 2 shipper cùng nhận 1 đơn → 1 thắng, 1 nhận
     * ObjectOptimisticLockingFailureException (do @Version trên entity).
     * Caller sẽ map exception → success=false với message "Đơn đã được nhận bởi shipper khác".
     *
     * timeout = 5s: tránh treo DB transaction khi có contention.
     * ==================================================================
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public Order quickClaimOrderForPickup(Integer orderId, Employee employee) {
        // KHÔNG dùng findByIdForUpdate (pessimistic lock).
        // Dùng findById thường, validate, update, save.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getPickupType() != com.logistics.enums.OrderPickupType.PICKUP_BY_COURIER) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }

        if (order.getEmployee() != null && !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_ALREADY_CLAIMED);
        }

        Set<OrderStatus> allowed = EnumSet.of(
                OrderStatus.CONFIRMED,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.URGENT_PICKUP,
                OrderStatus.PICKUP_RETRY
        );
        if (!allowed.contains(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_CLAIM_STATUS);
        }

        // Gán employee + fromOffice
        order.setEmployee(employee);
        if (order.getFromOffice() == null && employee.getOffice() != null) {
            order.setFromOffice(employee.getOffice());
        }
        // Chuyển status -> PICKING_UP
        order.setStatus(OrderStatus.PICKING_UP);
        return orderRepository.save(order);
    }

    private ShipperVehicle getOrCreateVehicle(Employee employee) {
        return shipperVehicleRepository.findByShipperId(employee.getId())
                .map(v -> v)
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

    public void applyVehicleWorkloadByStatus(Order order, Employee employee, OrderStatus newStatus) {
        applyVehicleWorkloadByStatus(order, employee, null, newStatus);
    }

    /**
     * Overload idempotent: chỉ cộng/trừ vehicle load khi status THỰC SỰ chuyển.
     * - Gọi với oldStatus != null → check transition (non-PICKED_UP → PICKED_UP mới cộng).
     * - Gọi với oldStatus == null → giữ tương thích code cũ (cộng/trừ mỗi lần gọi).
     */
    public void applyVehicleWorkloadByStatus(Order order, Employee employee, OrderStatus oldStatus, OrderStatus newStatus) {
        if (order == null || employee == null || newStatus == null) {
            log.warn("[VEHICLE_WORKLOAD_SKIP_NULL] order={} employee={} newStatus={}",
                    order == null ? "null" : order.getId(),
                    employee == null ? "null" : employee.getId(),
                    newStatus);
            return;
        }

        // Idempotent guard: chỉ áp dụng khi status thực sự thay đổi
        if (oldStatus != null && oldStatus == newStatus) {
            log.info("[VEHICLE_WORKLOAD_SKIP_UNCHANGED] orderId={} oldStatus={} newStatus={} employeeId={}",
                    order.getId(), oldStatus, newStatus, employee.getId());
            return;
        }

        ShipperVehicle vehicle = getOrCreateVehicle(employee);
        int currentOrdersBefore = vehicle.getCurrentOrders() != null ? vehicle.getCurrentOrders() : 0;
        BigDecimal currentWeightBefore = normalizeWeight(vehicle.getCurrentWeightKg());
        BigDecimal orderWeightKg = normalizeWeight(order.getWeight());

        log.info(
                "[VEHICLE_WORKLOAD_BEFORE] employeeId={} vehicleId={} orderId={} oldStatus={} newStatus={} "
                        + "currentOrdersBefore={} currentWeightBefore={} orderWeightKg={}",
                employee.getId(),
                vehicle.getId(),
                order.getId(),
                oldStatus,
                newStatus,
                currentOrdersBefore,
                currentWeightBefore,
                orderWeightKg);

        int currentOrders = currentOrdersBefore;
        BigDecimal currentWeightKg = currentWeightBefore;

        switch (newStatus) {
            // Pickup success: shipper vừa lấy hàng xong, đơn đang trên xe → tăng load.
            // Ngưỡng cộng đúng: PICKED_UP (sau khi bấm "Xác nhận đã lấy"), không phải PICKING_UP (chỉ claim).
            case PICKED_UP -> {
                currentOrders += 1;
                currentWeightKg = currentWeightKg.add(orderWeightKg);
            }
            // Delivery success: giao xong → giảm load
            case DELIVERED -> {
                currentOrders = Math.max(0, currentOrders - 1);
                currentWeightKg = currentWeightKg.subtract(orderWeightKg);
            }
            // Đơn kết thúc ở các trạng thái cuối (không còn trên xe / đã trả về office gốc):
            //   - RETURNED: đã trả cho sender
            //   - DELIVERY_FAILED_FINAL / PICKUP_FAILED_FINAL / RETURN_FAILED_FINAL: fail cuối, không retry
            //   - CANCELLED: hủy
            case RETURNED, DELIVERY_FAILED_FINAL, PICKUP_FAILED_FINAL,
                 RETURN_FAILED_FINAL, CANCELLED -> {
                currentOrders = Math.max(0, currentOrders - 1);
                currentWeightKg = currentWeightKg.subtract(orderWeightKg);
            }
            case PARTIAL_DELIVERY, PARTIAL_RETURN -> {
                currentOrders = Math.max(0, currentOrders);
                currentWeightKg = calculateReturnedWeightKg(order);
            }
            default -> {
                // DELIVERY_RETRY / RETURN_RETRY / PICKUP_RETRY / DELIVERING / PICKING_UP / RETURNING /
                // AT_ORIGIN_OFFICE / AT_DEST_OFFICE / IN_TRANSIT / RETURN_AT_ORIGIN_OFFICE:
                // KHÔNG đổi load trên xe (đơn vẫn đang được shipper giữ hoặc chỉ mới claim chưa lấy).
                log.info(
                        "[VEHICLE_WORKLOAD_NOOP] employeeId={} vehicleId={} orderId={} newStatus={}",
                        employee.getId(), vehicle.getId(), order.getId(), newStatus);
                return;
            }
        }

        int deltaOrders = currentOrders - currentOrdersBefore;
        BigDecimal deltaWeight = currentWeightKg.subtract(currentWeightBefore);

        log.info(
                "[VEHICLE_WORKLOAD_DELTA] employeeId={} vehicleId={} orderId={} oldStatus={} newStatus={} "
                        + "deltaOrders={} deltaWeight={}",
                employee.getId(),
                vehicle.getId(),
                order.getId(),
                oldStatus,
                newStatus,
                deltaOrders,
                deltaWeight);

        vehicle.setCurrentOrders(currentOrders);
        vehicle.setCurrentWeightKg(currentWeightKg);
        ShipperVehicle saved = shipperVehicleRepository.save(vehicle);
        shipperVehicleRepository.flush();

        log.info(
                "[VEHICLE_WORKLOAD_AFTER] employeeId={} vehicleId={} orderId={} "
                        + "currentOrdersAfter={} currentWeightAfter={} savedId={}",
                employee.getId(),
                vehicle.getId(),
                order.getId(),
                saved.getCurrentOrders(),
                saved.getCurrentWeightKg(),
                saved.getId());

        // Verify: đọc lại từ DB ngay sau save+flush để chắc chắn transaction ghi xuống
        shipperVehicleRepository.findByShipperId(employee.getId()).ifPresent(refreshed -> {
            log.info(
                    "[VEHICLE_WORKLOAD_VERIFY] employeeId={} vehicleId={} orderId={} "
                            + "currentOrdersDb={} currentWeightDb={}",
                    employee.getId(),
                    refreshed.getId(),
                    order.getId(),
                    refreshed.getCurrentOrders(),
                    refreshed.getCurrentWeightKg());
        });
    }

    public Map<String, Object> getDashboard() {
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
                || o.getStatus() == OrderStatus.RETURNED)
            .count();

        int inProgress = (int) assignedOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERING)
            .count();

        int delivered = (int) assignedOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .count();

        int failed = (int) assignedOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.RETURNED)
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
            NotificationResponse nres = notificationService.getNotifications(employee.getUser().getId(), nreq);
            if (nres != null) {
                notificationMaps = nres.getNotifications().stream().map(dto -> {
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
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR, e);
        }

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

        return data;
    }

    public Map<String, Object> listOrders(int page, int limit, String status, String search) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Lấy danh sách orderId thuộc các shipment DELIVERY active (PENDING/IN_TRANSIT) của shipper.
        // Đơn trong shipment có thể chưa được claim cá nhân (Order.employee = null) nhưng vẫn
        // phải hiển thị ở /shipper/orders vì shipper chịu trách nhiệm giao.
        List<Integer> shipmentOrderIds = shipmentOrderRepository
                .findOrderIdsByActiveDeliveryShipmentOfEmployee(employee.getId());

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));

            // Hiển thị: (a) đơn đã gán cho shipper này HOẶC (b) đơn thuộc shipment active của shipper.
            if (shipmentOrderIds != null && !shipmentOrderIds.isEmpty()) {
                Predicate byEmployee = cb.equal(root.get("employee").get("id"), employee.getId());
                Predicate byShipment = root.get("id").in(shipmentOrderIds);
                predicates.add(cb.or(byEmployee, byShipment));
            } else {
                predicates.add(cb.equal(root.get("employee").get("id"), employee.getId()));
            }

                // Đơn đã được shipper nhận / sẵn sàng để lấy / đang giao / đã giao / chờ lấy tại bưu cục đích
                predicates.add(root.get("status").in(
                    OrderStatus.AT_DEST_OFFICE,        // Hàng đã về bưu cục đích - chờ shipper đến lấy
                    OrderStatus.PICKED_UP,
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.PICKUP_RETRY,
                    OrderStatus.DELIVERING,
                    OrderStatus.DELIVERED,
                    OrderStatus.DELIVERY_RETRY,
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

        return result;
    }

    public Map<String, Object> listUnassignedOrders(int page, int limit) {
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

        return result;
    }

    // Mới: lấy danh sách các đơn mà người dùng chọn Yêu cầu lấy hàng (PICKUP_BY_COURIER)
    // và đã đánh dấu SẴN SÀNG LẤY (READY_FOR_PICKUP), chưa có shipper gán.
    public Map<String, Object> listPickupByCourierRequests(int page, int limit) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Order> spec = (root, query, cb) -> {
            // Chỉ các đơn do USER tạo
            Predicate createdByUser = cb.equal(root.get("createdByType"), OrderCreatorType.USER);

            // Điều kiện: (READY_FOR_PICKUP | URGENT_PICKUP && pickupType = PICKUP_BY_COURIER && employee IS NULL)
            List<Predicate> availablePreds = new ArrayList<>();
            availablePreds.add(root.get("status").in(OrderStatus.READY_FOR_PICKUP, OrderStatus.URGENT_PICKUP));
            availablePreds.add(cb.equal(root.get("pickupType"), OrderPickupType.PICKUP_BY_COURIER));
            availablePreds.add(cb.isNull(root.get("employee")));

            // Điều kiện: (employee = currentEmployee && status IN (READY_FOR_PICKUP, URGENT_PICKUP, PICKING_UP, PICKED_UP) && pickupType = PICKUP_BY_COURIER)
            List<Predicate> assignedPreds = new ArrayList<>();
            assignedPreds.add(cb.equal(root.get("employee").get("id"), employee.getId()));
            assignedPreds.add(root.get("status").in(
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.URGENT_PICKUP,
                OrderStatus.PICKING_UP,
                OrderStatus.PICKED_UP,
                OrderStatus.PICKUP_RETRY
            ));
            assignedPreds.add(cb.equal(root.get("pickupType"), OrderPickupType.PICKUP_BY_COURIER));

            // Thu hẹp theo bưu cục của shipper: nếu đơn đã có fromOffice thì lọc theo officeId,
            // nếu fromOffice = null thì vẫn cho hiện (đơn chưa được gán bưu cục)
            Predicate fromOfficeMatch = cb.or(
                    cb.isNull(root.get("fromOffice")),
                    cb.equal(root.get("fromOffice").get("id"), officeId)
            );
            availablePreds.add(fromOfficeMatch);
            assignedPreds.add(fromOfficeMatch);

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

        return result;
    }

    public Map<String, Object> getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice() != null ? employee.getOffice().getId() : null;

        boolean allowed = false;
        if (order.getEmployee() != null && order.getEmployee().getId() != null) {
            allowed = Objects.equals(order.getEmployee().getId(), employee.getId());
        } else if (order.getToOffice() != null && officeId != null) {
            allowed = Objects.equals(order.getToOffice().getId(), officeId);
        }

        if (!allowed) {
            throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
        }

        return mapOrderDetail(order);
    }

    public Map<String, Object> getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice() != null ? employee.getOffice().getId() : null;

        boolean allowed = false;
        if (order.getEmployee() != null && order.getEmployee().getId() != null) {
            allowed = Objects.equals(order.getEmployee().getId(), employee.getId());
        } else if (order.getToOffice() != null && officeId != null) {
            allowed = Objects.equals(order.getToOffice().getId(), officeId);
        }

        if (!allowed) {
            throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        data.put("trackingNumber", order.getTrackingNumber());
        return data;
    }

    public Map<String, Object> buildOrderDetail(Order order) {
        return mapOrderDetail(order);
    }
    
    //Bắt đầu luồng giao 1 phần: trả về danh sách sản phẩm và số lượng còn lại/đã giao/đã trả
    @Transactional
    public Map<String, Object> startPartialDelivery(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        Employee employee = getCurrentEmployee();
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
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
        return data;
    }

    @Transactional
    public void markProductDelivered(Integer orderProductId, Integer deliveredQuantity) {
        throw new UnsupportedOperationException("markProductDelivered is not supported. Use markProductDeliveredAtomic(...) which is atomic and concurrency-safe.");
    }

    @Transactional
    public void markProductDeliveredAtomic(Integer orderProductId, Integer deliveredQuantity) {
        if (deliveredQuantity == null || deliveredQuantity <= 0) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_QUANTITY);
        }

        OrderProduct op = orderProductRepository.findById(orderProductId)
            .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Order order = op.getOrder();
        Employee employee = getCurrentEmployee();
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        int affected = orderProductRepository.incrementDelivered(orderProductId, deliveredQuantity);
        if (affected == 0) {
            throw new AppException(OrderErrorCode.ORDER_QUANTITY_EXCEEDED);
        }

        OrderProduct updated = orderProductRepository.findById(orderProductId).orElse(op);
        saveHistory(order, OrderHistoryActionType.PARTIAL_DELIVERY,
                "Giao " + deliveredQuantity + " x " + (updated.getProduct() != null ? updated.getProduct().getName() : "sản phẩm"));
    }

    @Transactional
    public void markProductReturned(Integer orderProductId, Integer returnedQuantity, String reason) {
        throw new UnsupportedOperationException("markProductReturned is not supported. Use markProductReturnedAtomic(...) which is atomic and concurrency-safe.");
    }

    @Transactional
    public void markProductReturnedAtomic(Integer orderProductId, Integer returnedQuantity, String reason) {
        if (returnedQuantity == null || returnedQuantity <= 0) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_QUANTITY);
        }
        OrderProduct op = orderProductRepository.findById(orderProductId)
            .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Order order = op.getOrder();
        Employee employee = getCurrentEmployee();
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        int affected = orderProductRepository.incrementReturned(orderProductId, returnedQuantity);
        if (affected == 0) {
            throw new AppException(OrderErrorCode.ORDER_QUANTITY_EXCEEDED);
        }

        OrderProduct updated = orderProductRepository.findById(orderProductId).orElse(op);

        String note = "Trả lại " + returnedQuantity + " x " + (updated.getProduct() != null ? updated.getProduct().getName() : "sản phẩm");
        if (reason != null && !reason.isBlank()) note += ": " + reason;
        saveHistory(order, OrderHistoryActionType.PARTIAL_RETURN, note);
    }

    @Transactional
    public void finishPartialDelivery(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        Employee employee = getCurrentEmployee();
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        List<OrderProduct> products = orderProductRepository.findByOrderIdWithProduct(orderId);
        int totalQty = products.stream().mapToInt(p -> p.getQuantity() == null ? 0 : p.getQuantity()).sum();
        int totalDelivered = products.stream().mapToInt(p -> p.getDeliveredQuantity() == null ? 0 : p.getDeliveredQuantity()).sum();
        int totalReturned = products.stream().mapToInt(p -> p.getReturnedQuantity() == null ? 0 : p.getReturnedQuantity()).sum();

        if (totalDelivered >= totalQty && totalReturned == 0) {
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
            applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERED);
            saveHistory(order, OrderHistoryActionType.DELIVERED, "Đã giao toàn bộ đơn hàng");
            if (order.getUser() != null) {
                notificationService.create(
                        "Giao hàng thành công",
                        String.format("Đơn %s đã được giao thành công.", order.getTrackingNumber()),
                        "order_status",
                        order.getUser().getId(),
                        null,
                        "orders/tracking",
                        order.getTrackingNumber());
            }
            try {
                User shipperUser = employee.getUser();
                if (order.getCod() != null && order.getCod() > 0 && !hasExistingCodSubmission(order)) {
                    createPaymentSubmission(order, shipperUser, order.getCod(), "Thu COD sau khi giao");
                }
            } catch (Exception e) {
            throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR, e);
        }
            return;
        }

        OrderStatus oldStatus = order.getStatus();
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
            saveHistory(order, OrderHistoryActionType.PARTIAL_RETURN, "Trả 1 phần: trả " + totalReturned + " / " + totalQty);
        }

        if (order.getUser() != null) {
            String title;
            if (totalDelivered > 0) {
                title = "Giao hàng một phần";
            } else {
                title = "Hoàn hàng một phần";
            }
            notificationService.create(
                    title,
                    String.format("Đơn %s %s.", order.getTrackingNumber(), totalDelivered > 0 ? "đã được giao một phần" : "có sản phẩm được hoàn một phần"),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }

        if (totalDelivered > 0) {
            try {
                User shipperUser = employee.getUser();
                if (order.getCod() != null && order.getCod() > 0 && !hasExistingCodSubmission(order)) {
                    createPaymentSubmission(order, shipperUser, order.getCod(), "Thu COD sau khi giao");
                    try { order.setCodStatus(OrderCodStatus.PENDING); } catch (Exception e) {}
                    orderRepository.save(order);
                }
            } catch (Exception e) {
                throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    @Transactional
    public void acceptPickupRequest(Integer id) {
        shipmentDeliveryService.acceptPickupRequest(id);
    }

    @Transactional
    public void startPickup(Integer id) {
        shipmentDeliveryService.startPickup(id);
    }

    @Transactional
    public void claimOrder(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() != null) {
            throw new AppException(OrderErrorCode.ORDER_ALREADY_CLAIMED);
        }

        if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
            throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
        }

        // SHIPMENT-CENTERED: đơn giao phải thuộc shipment IN_TRANSIT trước khi shipper nhận
        shipmentRepository.findActiveDeliveryShipmentForOrder(employee.getId(), id)
                .orElseThrow(() -> new AppException(com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER));

        if (order.getStatus() != OrderStatus.AT_DEST_OFFICE && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_CLAIM_STATUS);
        }

        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order.setEmployee(employee);
        orderRepository.save(order);

        // Ghi history có shipmentId
        com.logistics.entity.Shipment activeShipment = shipmentRepository
                .findActiveDeliveryShipmentForOrder(employee.getId(), id).orElse(null);
        saveHistory(order, activeShipment, OrderHistoryActionType.READY_FOR_PICKUP,
                "Shipper nhận đơn giao (sẵn sàng lấy tại bưu cục)");

        // Thêm đơn vào AI route hiện tại của shipper
        try {
            assignOrderToActiveAiRoute(employee, order);
        } catch (Exception e) {
            // ignore AI route assign failure
        }
    }

    @Transactional
    public void unclaimOrder(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getPickupType() != null && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            if (order.getFromOffice() == null || !Objects.equals(order.getFromOffice().getId(), officeId)) {
                throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
            }
        } else {
            if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
                throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
            }
        }

        if (order.getStatus() != OrderStatus.PICKED_UP && order.getStatus() != OrderStatus.READY_FOR_PICKUP && order.getStatus() != OrderStatus.PICKING_UP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

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
    }

    @Transactional
    public void recordDeliveryAttempt(Integer id, UpdateDeliveryStatusRequest request) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
            throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
        }
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() == OrderStatus.PARTIAL_DELIVERY || order.getStatus() == OrderStatus.PARTIAL_RETURN) {
            throw new AppException(OrderErrorCode.ORDER_PARTIAL_DELIVERY_INVALID);
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new AppException(OrderErrorCode.ORDER_NOT_DELIVERING);
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.RETURNING || order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.DELIVERY_FAILED_FINAL) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        String status = request != null && request.getStatus() != null ? request.getStatus().trim().toUpperCase() : null;
        if (status == null || status.isBlank()) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_DELIVERY_STATUS);
        }

        if ("SUCCESS".equals(status)) {
            handleDeliverySuccess(order, employee, employee.getUser(), request);
            return;
        }
        if (!"FAILED".equals(status)) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_DELIVERY_STATUS);
        }
        if (request.getFailReason() == null || request.getFailReason().isBlank()) {
            throw new AppException(OrderErrorCode.ORDER_MISSING_FAIL_REASON);
        }

        handleDeliveryFailure(order, employee, employee.getUser(), request);
    }

    @Transactional
    public void updateDeliveryStatus(Integer id, UpdateDeliveryStatusRequest request) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
            throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_DELIVERY_STATUS);
        }

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_DELIVERY_STATUS);
        }

        User shipperUser = employee.getUser();

        // SHIPMENT-CENTERED: ủy quyền cho ShipmentDeliveryService khi có thể
        if (newStatus == OrderStatus.DELIVERED) {
            shipmentDeliveryService.markDelivered(id, request);
            applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERED);
            if (order.getUser() != null) {
                notificationService.create("Giao hàng thành công",
                        String.format("Đơn %s đã được giao thành công.", order.getTrackingNumber()),
                        "order_status", order.getUser().getId(), null, "orders/tracking", order.getTrackingNumber());
            }
            return;
        }
        if (newStatus == OrderStatus.DELIVERING) {
            // Theo rule mới: PICKED_UP -> DELIVERING, validate trong service
            shipmentDeliveryService.startDelivery(id);
            applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERING);
            return;
        }
        if (newStatus == OrderStatus.DELIVERY_RETRY) {
            shipmentDeliveryService.markDeliveryFailed(id, request);
            applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERY_RETRY);
            return;
        }
        if (request.getStatus() != null && request.getStatus().equalsIgnoreCase("DELIVERY_FAILED_FINAL")) {
            shipmentDeliveryService.markDeliveryFailedFinal(id, request);
            // vehicle workload: chuyển từ DELIVERING -> DELIVERY_FAILED_FINAL
            applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERY_FAILED_FINAL);
            if (order.getUser() != null) {
                notificationService.create("Giao hàng không thành công",
                        String.format("Đơn %s giao không thành công sau nhiều lần thử.", order.getTrackingNumber()),
                        "order_status", order.getUser().getId(), null, "orders/tracking", order.getTrackingNumber());
            }
            return;
        }

        // Các status khác (DELIVERING, RETURNED, ...) - giữ logic cũ
        order.setStatus(newStatus);
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        orderRepository.save(order);
        applyVehicleWorkloadByStatus(order, employee, newStatus);

        String statusMessage = switch (newStatus) {
            case DELIVERING -> "Đã bắt đầu giao hàng";
            case DELIVERED -> "Đã giao hàng thành công";
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
            case RETURNED -> saveHistory(order, OrderHistoryActionType.RETURNED, "Shipper đã trả đơn về bưu cục");
            default -> {
            }
        }
    }

    /**
     * Returns true nếu order đã ở PICKED_UP trước khi gọi (idempotent replay),
     * false nếu vừa chuyển sang PICKED_UP.
     */
    @Transactional
    public boolean markPickedUp(Integer id, PickedUpRequest request) {
        Employee employee = getCurrentEmployee();

        // 1. Lock order trước để tránh race
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        // Chỉ shipper đã nhận (employee) mới có thể đánh dấu đã lấy
        if (order.getEmployee() == null || order.getEmployee().getId() == null
                || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        // 2. Kiểm tra order có thuộc shipment DELIVERY IN_TRANSIT của shipper hiện tại không
        List<com.logistics.entity.Shipment> activeShipments =
                shipmentOrderRepository.findActiveShipmentsForOrder(order.getId());
        com.logistics.entity.Shipment inTransitShipment = null;
        if (activeShipments != null) {
            for (com.logistics.entity.Shipment s : activeShipments) {
                if (s.getStatus() == com.logistics.enums.ShipmentStatus.IN_TRANSIT
                        && s.getType() == com.logistics.enums.ShipmentType.DELIVERY
                        && s.getEmployee() != null
                        && Objects.equals(s.getEmployee().getId(), employee.getId())) {
                    inTransitShipment = s;
                    break;
                }
            }
        }

        OrderStatus orderStatusBefore = order.getStatus();
        String branch;
        if (inTransitShipment != null) {
            branch = "SHIPMENT";
        } else if (activeShipments != null && !activeShipments.isEmpty()) {
            branch = "PENDING_SHIPMENT";
        } else {
            branch = "STANDALONE";
        }

        // [DEBUG] Log đầy đủ context trước khi decide branch
        log.info(
                "[MARK_PICKED_UP_BRANCH] orderId={} orderStatus={} activeShipments={} "
                        + "chosenShipmentId={} branch={}",
                id,
                orderStatusBefore,
                activeShipments == null ? 0 : activeShipments.size(),
                inTransitShipment != null ? inTransitShipment.getId() : null,
                branch);

        // Idempotent: nếu order đã ở PICKED_UP → trả về ngay, KHÔNG cộng vehicle nữa.
        // Workload đã được áp dụng ở lần gọi đầu tiên.
        if (orderStatusBefore == OrderStatus.PICKED_UP) {
            log.info("[MARK_PICKED_UP_IDEMPOTENT] orderId={} alreadyPickedUp=true branch={}",
                    id, branch);
            return true;
        }

        // 3. Nếu thuộc shipment IN_TRANSIT → ủy quyền cho ShipmentDeliveryService và return
        if (inTransitShipment != null) {
            shipmentDeliveryService.markPickedUp(id, request);
            // refresh + apply workload (idempotent nhờ oldStatus guard)
            order = orderRepository.findById(id)
                    .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
            applyVehicleWorkloadByStatus(order, employee, orderStatusBefore, OrderStatus.PICKED_UP);
            if (order.getUser() != null) {
                notificationService.create(
                        "Đã lấy hàng thành công",
                        String.format("Đơn %s đã được lấy hàng thành công.", order.getTrackingNumber()),
                        "order_status",
                        order.getUser().getId(),
                        null,
                        "orders/tracking",
                        order.getTrackingNumber());
            }
            return false;
        }

        // 3b. Nếu có shipment khác (PENDING, COMPLETED, CANCELLED...) nhưng KHÔNG phải IN_TRANSIT
        // → trả 400 rõ ràng thay vì 500.
        boolean belongsToAnyShipment = activeShipments != null && !activeShipments.isEmpty();
        if (belongsToAnyShipment) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        // 4. Else: standalone pickup (order không thuộc shipment nào)
        OrderStatus cur = order.getStatus();
        Set<OrderStatus> standaloneAllowed = EnumSet.of(
                OrderStatus.PICKING_UP,
                OrderStatus.PICKUP_RETRY,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.CONFIRMED,
                OrderStatus.URGENT_PICKUP
        );
        if (!standaloneAllowed.contains(cur)) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        // Đảm bảo order có employee = current shipper
        if (order.getEmployee() == null) {
            order.setEmployee(employee);
        }

        // Ghi notes / ảnh / vị trí nếu có (lưu tạm vào notes để không thay đổi schema)
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

        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);
        // Lưu history không có shipment (standalone)
        saveHistory(order, OrderHistoryActionType.PICKED_UP, "Shipper đã lấy hàng thành công");

        // Cộng vehicle load (idempotent nhờ oldStatus guard)
        applyVehicleWorkloadByStatus(order, employee, orderStatusBefore, OrderStatus.PICKED_UP);

        if (order.getUser() != null) {
            notificationService.create(
                    "Đã lấy hàng thành công",
                    String.format("Đơn %s đã được lấy hàng thành công.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
        return false;
    }

    @Transactional
    public void retryPickup(Integer id) {
        Employee employee = getCurrentEmployee();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        // Kiểm tra trạng thái phải là PICKUP_RETRY
        if (order.getStatus() != OrderStatus.PICKUP_RETRY) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        // Kiểm tra shipper hiện tại là người đã nhận đơn
        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        order.setStatus(OrderStatus.PICKING_UP);
        orderRepository.save(order);
        orderRepository.flush();
        saveHistory(order, OrderHistoryActionType.PICKING_UP, "Shipper tiến hành đến lấy lại (retry pickup)");
        // Retry pickup: chỉ chuyển trạng thái về PICKING_UP — vehicle load KHÔNG cộng ở đây,
        // chỉ cộng khi shipper thực sự lấy lại hàng (PICKED_UP).

        if (order.getUser() != null) {
            notificationService.create(
                    "Shipper đang đến lấy lại",
                    String.format("Shipper sẽ đến lấy lại đơn %s trong ít phút.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    @Transactional
    public void deliverToOrigin(Integer id, DeliverOriginRequest request) {
        Employee employee = getCurrentEmployee();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        // Kiểm tra shipper được gán
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        // SHIPMENT-CENTERED: ủy quyền cho ShipmentDeliveryService (validate IN_TRANSIT, set AT_ORIGIN_OFFICE)
        // Service cũng sẽ set currentOffice = shipment.fromOffice. Nếu request chỉ định officeId, set vào order trước.
        try {
            if (request != null && request.getOfficeId() != null) {
                officeRepository.findById(request.getOfficeId()).ifPresent(order::setToOffice);
            }
        } catch (Exception e) { throw e; }
        orderRepository.save(order);

        shipmentDeliveryService.deliverToOrigin(id);
        order = orderRepository.findById(id).orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        // Nếu request chỉ định officeId thì dùng office đó
        // (đã làm phía trên)

        // Gán fromOffice = bưu cục hiện tại của shipper (nếu chưa có)
        Office currentOffice = employee.getOffice();
        if (order.getFromOffice() == null && currentOffice != null) {
            order.setFromOffice(currentOffice);
        }
        // Set currentOffice = bưu cục hiện tại của shipper (giữ giá trị shipment delivery set)
        if (currentOffice != null) {
            order.setCurrentOffice(currentOffice);
        }

        // Stage 1 - Kiểm tra nếu office hiện tại là bưu cục đích thì set pendingDestinationConfirm = true
        if (currentOffice != null && orderDestinationService.isDestinationOffice(order, currentOffice)) {
            order.setPendingDestinationConfirm(true);
        }

        orderRepository.save(order);

        try { autoAssignService.autoAssignOnArrival(order.getId()); } catch (Exception e) { throw e; }

        if (order.getUser() != null) {
            notificationService.create(
                    "Hàng đã về bưu cục gốc",
                    String.format("Đơn %s đã về bưu cục gốc.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    /**
     * Stage 2 - Xác nhận thủ công đơn hàng đã đến bưu cục đích (dành cho Shipper).
     * Chỉ áp dụng cho đơn có pendingDestinationConfirm = true.
     */
    @Transactional
    public void confirmDestinationOffice(Integer orderId) {
        Employee employee = getCurrentEmployee();

        // Dùng findByIdForUpdate để tránh race condition
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        // Validate pendingDestinationConfirm
        if (order.getPendingDestinationConfirm() == null || !order.getPendingDestinationConfirm()) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION,
                    "Trạng thái hiện tại không yêu cầu xác nhận bưu cục đích",
                    "AT_DEST_OFFICE");
        }

        // Validate order status hợp lệ: IN_TRANSIT hoặc AT_ORIGIN_OFFICE
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.IN_TRANSIT && currentStatus != OrderStatus.AT_ORIGIN_OFFICE) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION,
                    OrderUtils.translateOrderStatus(currentStatus),
                    OrderUtils.translateOrderStatus(OrderStatus.AT_DEST_OFFICE));
        }

        // Validate shipper thuộc bưu cục hiện tại của đơn (dùng isDestinationOffice để handle toOffice null)
        Office shipperOffice = employee.getOffice();
        if (shipperOffice == null || !orderDestinationService.isDestinationOffice(order, shipperOffice)) {
            throw new AppException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        // Set status
        order.setStatus(OrderStatus.AT_DEST_OFFICE);
        order.setPendingDestinationConfirm(false);
        // Nếu toOffice null, set bằng office hiện tại để autoAssignOnArrival hoạt động đúng
        if (order.getToOffice() == null) {
            order.setToOffice(shipperOffice);
        }
        // Set currentOffice = bưu cục hiện tại của shipper
        order.setCurrentOffice(shipperOffice);
        orderRepository.save(order);

        // Ghi OrderHistory AT_DEST_OFFICE
        saveHistory(order, OrderHistoryActionType.AT_DEST_OFFICE, "Shipper xác nhận đơn đã đến bưu cục đích");

        // Gửi notification cho user
        if (order.getUser() != null) {
            notificationService.create(
                    "Hàng đã đến bưu cục đích",
                    String.format("Đơn %s đã đến bưu cục đích và sẵn sàng giao đến bạn.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    /**
     * Lấy danh sách đơn hàng có pendingDestinationConfirm = true tại bưu cục của shipper.
     */
    public Map<String, Object> getPendingDestinationConfirmOrders() {
        Employee employee = getCurrentEmployee();
        Office shipperOffice = employee.getOffice();
        if (shipperOffice == null) {
            return Map.of("orders", List.of());
        }

        List<OrderStatus> statuses = List.of(OrderStatus.IN_TRANSIT, OrderStatus.AT_ORIGIN_OFFICE);
        List<Order> orders = orderRepository.findPendingDestinationConfirmByOfficeId(shipperOffice.getId(), shipperOffice.getCityCode(), statuses);

        List<Map<String, Object>> orderMaps = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("trackingNumber", order.getTrackingNumber());
            map.put("status", order.getStatus().name());
            map.put("recipientName", order.getRecipientName());
            map.put("recipientPhone", order.getRecipientPhone());
            map.put("recipientFullAddress", order.getRecipientFullAddress());
            map.put("pendingDestinationConfirm", order.getPendingDestinationConfirm());
            map.put("cod", order.getCod());
            map.put("totalFee", order.getTotalFee());
            map.put("createdAt", order.getCreatedAt());
            map.put("updatedAt", order.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return Map.of(
                "orders", orderMaps,
                "pagination", Map.of(
                        "page", 1,
                        "limit", 50,
                        "total", orderMaps.size()
                )
        );
    }

    public Map<String, Object> getDeliveryHistory(int page, int limit, String status) {
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
                        return os == OrderStatus.DELIVERY_FAILED_FINAL;
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

        return result;
    }

    @Transactional
    public Map<String, Object> createIncidentReport(CreateIncidentReportRequest request) {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

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

        return data;
    }

    @Transactional
    public Map<String, Object> createIncidentReport(Integer orderId, String incidentType, String title, String description, String priority, org.springframework.web.multipart.MultipartFile[] images) {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

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

        return data;
    }

    public List<Map<String, Object>> listIncidentReports() {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        List<IncidentReport> incidents = incidentReportRepository.findAll()
                .stream()
                .filter(ir -> ir.getShipper() != null && Objects.equals(ir.getShipper().getId(), shipperUser.getId()))
                .toList();

        List<Map<String, Object>> data = incidents.stream()
                .map(this::mapIncident)
                .toList();

        return data;
    }

    public Map<String, Object> getIncidentDetail(Integer id) {
        Employee employee = getCurrentEmployee();
        User shipperUser = employee.getUser();

        IncidentReport incident = incidentReportRepository.findById(id)
                .orElseThrow(() -> new AppException(IncidentReportErrorCode.INCIDENT_REPORT_NOT_FOUND));

        if (incident.getShipper() == null || !Objects.equals(incident.getShipper().getId(), shipperUser.getId())) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return mapIncident(incident);
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
            f.put("code", order.getFromOffice().getCode());
            f.put("phoneNumber", order.getFromOffice().getPhoneNumber());
            f.put("detail", order.getFromOffice().getDetail());
            f.put("cityCode", order.getFromOffice().getCityCode());
            f.put("wardCode", order.getFromOffice().getWardCode());
            f.put("latitude", order.getFromOffice().getLatitude());
            f.put("longitude", order.getFromOffice().getLongitude());
            map.put("fromOffice", f);
        } else {
            map.put("fromOffice", null);
        }
        if (order.getCurrentOffice() != null) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", order.getCurrentOffice().getId());
            c.put("name", order.getCurrentOffice().getName());
            c.put("code", order.getCurrentOffice().getCode());
            c.put("phoneNumber", order.getCurrentOffice().getPhoneNumber());
            c.put("detail", order.getCurrentOffice().getDetail());
            c.put("cityCode", order.getCurrentOffice().getCityCode());
            c.put("wardCode", order.getCurrentOffice().getWardCode());
            c.put("latitude", order.getCurrentOffice().getLatitude());
            c.put("longitude", order.getCurrentOffice().getLongitude());
            map.put("currentOffice", c);
        } else {
            map.put("currentOffice", null);
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

        // ============== Populate shipment fields for UI ==============
        // Backend từng bỏ qua, khiến frontend OrderDetail.tsx hiện "Chưa gắn chuyến"
        // mặc dù DB đã có shipment_orders từ AI confirmPlan.
        // Dùng cùng pattern với saveHistory(...) ở line 108-118.
        try {
            List<com.logistics.entity.Shipment> activeShipments =
                    shipmentOrderRepository.findActiveShipmentsForOrder(order.getId());
            if (activeShipments != null && !activeShipments.isEmpty()) {
                com.logistics.entity.Shipment s = activeShipments.get(0);
                map.put("shipmentId", s.getId());
                map.put("shipmentCode", s.getCode());
                map.put("shipmentStatus", s.getStatus() != null ? s.getStatus().name() : null);
                map.put("shipmentType", s.getType() != null ? s.getType().name() : null);
            } else {
                map.put("shipmentId", null);
                map.put("shipmentCode", null);
                map.put("shipmentStatus", null);
                map.put("shipmentType", null);
            }
        } catch (Exception e) {
            log.warn("[SHIPPER_ORDER_SHIPMENT_MAP_LOAD_FAIL] orderId={} error={}",
                    order.getId(), e.getMessage());
            map.put("shipmentId", null);
            map.put("shipmentCode", null);
            map.put("shipmentStatus", null);
            map.put("shipmentType", null);
        }

        // Debug log theo yêu cầu - in ngay trước khi return response
        log.info("[SHIPPER_ORDER_SHIPMENT_MAP] orderId={} shipmentId={} shipmentCode={} shipmentStatus={}",
                order.getId(), map.get("shipmentId"), map.get("shipmentCode"), map.get("shipmentStatus"));

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

    private void handleDeliverySuccess(Order order, Employee employee, User shipperUser, UpdateDeliveryStatusRequest request) {
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

        saveHistory(order, OrderHistoryActionType.DELIVERED, "Shipper đã giao hàng thành công");

        if (order.getUser() != null) {
            notificationService.create(
                    "Giao hàng thành công",
                    String.format("Đơn %s đã được giao thành công.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    private void handleDeliveryFailure(Order order, Employee employee, User shipperUser, UpdateDeliveryStatusRequest request) {
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new AppException(OrderErrorCode.ORDER_NOT_DELIVERING);
        }
        int maxAttempts = getMaxDeliveryAttempts();
        long failedCountBefore = countFailedDeliveryAttempts(order.getId());
        int attemptNumber = (int) failedCountBefore + 1;
        boolean finalFail = attemptNumber >= maxAttempts;

        String reason = request.getFailReason();
        String note = request.getNotes();
        if (reason == null || reason.isBlank()) {
            throw new AppException(OrderErrorCode.ORDER_MISSING_FAIL_REASON);
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
            if (order.getUser() != null) {
                notificationService.create(
                        "Giao hàng không thành công",
                        String.format("Đơn %s giao không thành công sau nhiều lần thử.", order.getTrackingNumber()),
                        "order_status",
                        order.getUser().getId(),
                        null,
                        "orders/tracking",
                        order.getTrackingNumber());
            }

            // Chuyển tiếp sang RETURNING sau khi ghi DELIVERY_FAILED_FINAL
            order.setStatus(OrderStatus.RETURNING);
            order.setEmployee(null); // unassign shipper
            orderRepository.save(order);
            saveHistory(order, OrderHistoryActionType.RETURNING, "Đơn hàng giao thất bại tối đa, chuyển sang trạng thái hoàn hàng về bưu cục/người gửi");
            if (order.getUser() != null) {
                notificationService.create(
                        "Đơn hàng đang được hoàn về",
                        String.format("Đơn %s giao không thành công sau nhiều lần thử và đang được hoàn về bưu cục gốc/người gửi.", order.getTrackingNumber()),
                        "order_status",
                        order.getUser().getId(),
                        null,
                        "orders/tracking",
                        order.getTrackingNumber());
            }
            return;
        }

        order.setStatus(OrderStatus.DELIVERY_RETRY);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.DELIVERY_RETRY, buildDeliveryAttemptNote(reason, note, attemptNumber, maxAttempts));
        if (order.getUser() != null) {
            notificationService.create(
                    "Giao hàng chưa thành công",
                    String.format("Giao đơn %s chưa thành công. Hệ thống sẽ sắp xếp giao lại.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    @Transactional
    public void returnFailedToOffice(Integer id) {
        Employee employee = getCurrentEmployee();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.DELIVERY_RETRY) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        applyVehicleWorkloadByStatus(order, employee, OrderStatus.DELIVERY_RETRY);
        // SHIPMENT-CENTERED: ủy quyền cho ShipmentDeliveryService (validate IN_TRANSIT, set AT_DEST_OFFICE,
        // currentOffice = shipment.fromOffice). Theo rule mới, KHÔNG set employee=null (giữ trong shipment).
        shipmentDeliveryService.returnFailedToDestOffice(id);
    }

    private void handleDeliveryFailedFinal(Order order, Employee employee, User shipperUser, UpdateDeliveryStatusRequest request) {
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.DELIVERING && order.getStatus() != OrderStatus.DELIVERY_FAILED_FINAL) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }
        order.setStatus(OrderStatus.DELIVERY_FAILED_FINAL);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.DELIVERY_FAILED_FINAL, "Giao thất bại quá số lần cho phép");
        if (order.getUser() != null) {
            notificationService.create(
                    "Giao hàng không thành công",
                    String.format("Đơn %s giao không thành công sau nhiều lần thử.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }

        // Chuyển tiếp sang RETURNING sau khi ghi DELIVERY_FAILED_FINAL
        order.setStatus(OrderStatus.RETURNING);
        order.setEmployee(null);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.RETURNING, "Đơn hàng giao thất bại tối đa, chuyển sang trạng thái hoàn hàng về bưu cục/người gửi");
        if (order.getUser() != null) {
            notificationService.create(
                    "Đơn hàng đang được hoàn về",
                    String.format("Đơn %s giao không thành công sau nhiều lần thử và đang được hoàn về bưu cục gốc/người gửi.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
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

    /**
     * Tính ETA cộng dồn theo từng LEG thật giữa các điểm:
     *   currentPos -> stop1 -> stop2 -> ... -> office (nếu returnToOffice)
     *
     * - legDistanceKm = Haversine giữa 2 tọa độ
     * - legDurationMinutes = legDistanceKm / speedKmh * 60
     * - serviceTimeMinutes: lấy từ AI nếu có, fallback theo stopType/COD
     * - ETA stop = baseTime + sum(leg) trước stop
     *
     * @return tổng estimatedDuration = sum(legDurationMinutes + serviceTimeMinutes)
     */
    private double computeLegBasedEtas(List<AiRoutePlanStop> stops,
                                        LocalDateTime baseTime,
                                        Double startLat, Double startLng,
                                        Double officeLat, Double officeLng,
                                        Double speedKmh) {
        if (stops == null || stops.isEmpty()) return 0.0;

        double speed = (speedKmh != null && speedKmh > 0) ? speedKmh : 25.0;

        // Sắp xếp theo stopSequence để tính tuần tự
        List<AiRoutePlanStop> sorted = new ArrayList<>(stops);
        sorted.sort(Comparator.comparing(AiRoutePlanStop::getStopSequence,
                Comparator.nullsLast(Comparator.naturalOrder())));

        LocalDateTime currentEta = baseTime;
        Double fromLat = startLat;
        Double fromLng = startLng;
        double totalDuration = 0.0;
        int stopIdx = 0;

        for (AiRoutePlanStop s : sorted) {
            boolean isReturnStop = s.getStopType() == RouteStopType.RETURN_TO_OFFICE;

            Double toLat = s.getRecipientLatitude();
            Double toLng = s.getRecipientLongitude();

            // Nếu là stop RETURN_TO_OFFICE, dùng tọa độ bưu cục nếu thiếu
            if (isReturnStop && (!hasValidLatLng(toLat, toLng))) {
                toLat = officeLat;
                toLng = officeLng;
            }

            int legMin;
            double distanceKm = 0.0;

            if (hasValidLatLng(fromLat, fromLng) && hasValidLatLng(toLat, toLng)) {
                distanceKm = haversineKm(fromLat, fromLng, toLat, toLng);
                legMin = (int) Math.round(distanceKm / speed * 60.0);
                if (legMin < 1) legMin = 1;
            } else {
                // Không có tọa độ: dùng legDurationMinutes từ AI nếu có & hợp lệ
                Integer aiLeg = s.getLegDurationMinutes();
                if (aiLeg != null && aiLeg > 0) {
                    legMin = aiLeg;
                    distanceKm = legMin / 60.0 * speed;
                } else {
                    // Fallback cuối: chia đều 5 phút
                    legMin = 5;
                }
            }

            currentEta = currentEta.plusMinutes(legMin);
            s.setEtaTime(currentEta.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            s.setLegDistanceKm(BigDecimal.valueOf(Math.round(distanceKm * 100.0) / 100.0));
            s.setLegDurationMinutes(legMin);

            int minsFromStart = (int) java.time.Duration.between(baseTime, currentEta).toMinutes();
            s.setEtaMinutesFromStart(minsFromStart);

            // Service time: ưu tiên AI, fallback theo rule
            int svcMin = resolveServiceTimeMinutes(s);
            s.setServiceTimeMinutes(svcMin);

            currentEta = currentEta.plusMinutes(svcMin);

            totalDuration += legMin + svcMin;

            fromLat = toLat;
            fromLng = toLng;
            stopIdx++;
        }

        return totalDuration;
    }

    /**
     * Xác định tốc độ trung bình (km/h) để tính leg duration.
     * Ưu tiên: lấy từ shipper.speedKmh nếu có, fallback 25 km/h.
     */
    private Double resolveAverageSpeedKmh(AiRoutePlanRoute route) {
        try {
            if (route != null && route.getShipperEmployeeId() != null) {
                // Shipper entity không có speedKmh cố định; dùng config mặc định theo khu vực
                // Hiện tại hard-code 25 km/h nội thành; có thể mở rộng sau
            }
        } catch (Exception ignored) {
        }
        return 25.0;
    }

    /**
     * Service time tại 1 stop theo rule nghiệp vụ:
     * - DELIVERY/PICKUP thường: 5/7 phút
     * - COD > 0: cộng thêm 2 phút
     * - Ưu tiên lấy từ AI serviceTimeMinutes nếu có và > 0
     */
    private int resolveServiceTimeMinutes(AiRoutePlanStop s) {
        Integer aiSvc = s.getServiceTimeMinutes();
        if (aiSvc != null && aiSvc > 0) {
            return aiSvc;
        }
        int base;
        if (s.getStopType() == RouteStopType.PICKUP) {
            base = 7;
        } else {
            base = 5;
        }
        if (s.getCodAmount() != null && s.getCodAmount() > 0) {
            base += 2;
        }
        return base;
    }

    /**
     * Khoảng cách Haversine giữa 2 tọa độ (km).
     */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Tính tổng duration = sum(legDurationMinutes + serviceTimeMinutes) cho tất cả stops.
     */
    private double computeTotalDurationMinutes(List<AiRoutePlanStop> stops) {
        if (stops == null || stops.isEmpty()) return 0;
        long total = 0;
        for (AiRoutePlanStop s : stops) {
            Integer leg = s.getLegDurationMinutes();
            Integer svc = s.getServiceTimeMinutes();
            if (leg != null) total += leg;
            if (svc != null) total += svc;
        }
        return total;
    }

    // Lộ trình giao hàng (ưu tiên tuyến AI đã xác nhận)
    @Transactional(readOnly = true)
    public Map<String, Object> getDeliveryRoute() {
        Employee employee = getCurrentEmployee();
        log.info("[getDeliveryRoute] shipperEmployeeId={} officeId={}",
                employee.getId(), employee.getOffice() != null ? employee.getOffice().getId() : null);

        // Priority 1: Shipment DELIVERY của shipper (shipment-centric, source of truth sau AI confirm Phase 1)
        List<Shipment> activeShipments = shipmentRepository.findActiveDeliveryShipmentsByEmployee(employee.getId());
        log.info("[getDeliveryRoute] activeShipments (PENDING+IN_TRANSIT) found={}", activeShipments.size());

        // Ưu tiên IN_TRANSIT trước (shipper đang chạy)
        Shipment inTransitShipment = activeShipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT)
                .findFirst()
                .orElse(null);
        if (inTransitShipment != null) {
            log.info("[getDeliveryRoute] returning IN_TRANSIT shipment: id={} code={}",
                    inTransitShipment.getId(), inTransitShipment.getCode());
            return buildShipmentRouteResponseData(employee, inTransitShipment);
        }

        // Sau đó PENDING (chưa start)
        Shipment pendingShipment = activeShipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.PENDING)
                .findFirst()
                .orElse(null);
        if (pendingShipment != null) {
            log.info("[getDeliveryRoute] returning PENDING shipment: id={} code={}",
                    pendingShipment.getId(), pendingShipment.getCode());
            return buildShipmentRouteResponseData(employee, pendingShipment);
        }

        // Fallback: AiRoutePlanRoute CONFIRMED (giữ nguyên logic cũ cho backward compat)
        List<AiRoutePlanRoute> aiRoutes = aiRoutePlanRouteRepository.findActiveConfirmedRoutesForShipper(
                employee.getId(), AiRoutePlanStatus.CONFIRMED);
        log.info("[getDeliveryRoute] aiRoutes found={}", aiRoutes.size());

        AiRoutePlanRoute aiRoute = null;
        for (AiRoutePlanRoute r : aiRoutes) {
            long deliveryPickupStops = r.getStops().stream()
                    .filter(s -> s.getStopType() != RouteStopType.RETURN_TO_OFFICE)
                    .count();
                    if (deliveryPickupStops > 0) {
                aiRoute = r;
                break;
            }
        }

        if (aiRoute != null) {
            return buildAiDeliveryRouteResponseData(employee, aiRoute);
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
                    OrderStatus.DELIVERING,
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.PICKING_UP
            ));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Order> routeOrders = orderRepository.findAll(routeSpec, Sort.by(Sort.Direction.ASC, "createdAt"));

        int totalCOD = routeOrders.stream().mapToInt(o -> o.getCod() != null ? o.getCod() : 0).sum();

        List<Map<String, Object>> deliveryStops = routeOrders.stream().map(order -> {
            String recipientFullAddress = resolveRecipientFullAddress(order);
            Map<String, Object> stop = new HashMap<>();
            stop.put("id", order.getId());
            stop.put("orderId", order.getId());
            stop.put("trackingNumber", order.getTrackingNumber());
            // Fallback chỉ build delivery stops (không có shipment thì route là delivery thuần).
            // Thiếu stopType sẽ khiến FE isDeliveryStop() loại mất -> "Không có lộ trình vận chuyển hôm nay".
            stop.put("stopType", "DELIVERY");
            stop.put("recipientName", order.getRecipientName());
            stop.put("recipientPhone", order.getRecipientPhone());
            stop.put("recipientAddress", recipientFullAddress);
            stop.put("recipientFullAddress", recipientFullAddress);
            stop.put("recipientLatitude", order.getRecipientLatitude());
            stop.put("recipientLongitude", order.getRecipientLongitude());
            stop.put("latitude", order.getRecipientLatitude());
            stop.put("longitude", order.getRecipientLongitude());
            stop.put("codAmount", order.getCod());
            stop.put("priority", order.getCod() != null && order.getCod() > 1000000 ? "urgent" : "normal");
            stop.put("serviceType", order.getServiceType() != null ? order.getServiceType().getName() : "Tiêu chuẩn");
            OrderStatus os = order.getStatus();
            stop.put("status", os == OrderStatus.DELIVERED ? "completed" :
                    (os == OrderStatus.DELIVERING || os == OrderStatus.PICKED_UP) ? "in_progress" : "pending");
            stop.put("orderStatus", os != null ? os.name() : null);
            return stop;
        }).toList();

        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("id", 1);
        routeInfo.put("name", "Tuyến " + officeId);
        routeInfo.put("startLocation", employee.getOffice().getName());
        routeInfo.put("totalStops", routeOrders.size());
        routeInfo.put("completedStops", (int) routeOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count());
        // Ước tính: ~20 phút/điểm dừng (di chuyển + giao)
        int estimatedMinutes = routeOrders.size() * 20;
        routeInfo.put("totalDistance", 0); // TODO: Tính toán khoảng cách thực tế
        routeInfo.put("estimatedDuration", estimatedMinutes);
        routeInfo.put("totalCOD", totalCOD);
        routeInfo.put("status", "not_started");

        Map<String, Object> result = new HashMap<>();
        result.put("routeInfo", routeInfo);
        result.put("deliveryStops", deliveryStops);

        log.info("[getDeliveryRoute] returning fallback route: totalStops={}", routeOrders.size());
        return result;
    }

    private Map<String, Object> buildAiDeliveryRouteResponseData(Employee employee, AiRoutePlanRoute aiRoute) {
        // Query directly from DB to get correct ordering
        List<AiRoutePlanStop> allStops = aiRoutePlanStopRepository.findByRouteIdOrderByStopSequenceAsc(aiRoute.getId());
        if (allStops != null) {
            allStops.sort(Comparator.comparing(AiRoutePlanStop::getStopSequence));
        } else {
            allStops = List.<AiRoutePlanStop>of();
        }

        // Visible stops = DELIVERY + PICKUP (không tính RETURN_TO_OFFICE)
        List<AiRoutePlanStop> visibleStops = allStops.stream()
                .filter(s -> s.getStopType() != RouteStopType.RETURN_TO_OFFICE)
                .toList();

        // totalCOD: chỉ DELIVERY stops
        int totalCOD = visibleStops.stream()
                .filter(s -> s.getStopType() == RouteStopType.DELIVERY)
                .mapToInt(s -> {
                    Integer cod = s.getCodAmount();
                    return cod != null ? cod : 0;
                })
                .sum();

        List<Map<String, Object>> deliveryStops = visibleStops.stream().map(stop -> {
            Order order = stop.getOrder();
            String trackingNumber = stop.getTrackingNumber();
            String recipientName = stop.getRecipientName();
            String recipientPhone = stop.getRecipientPhone();
            String stopAddress = stop.getRecipientAddress();
            String recipientFullAddress = stopAddress != null
                    ? stopAddress
                    : (order != null ? resolveRecipientFullAddress(order) : "");

            // COD: chỉ DELIVERY mới có COD
            int codAmount = 0;
            if (stop.getStopType() == RouteStopType.DELIVERY) {
                Integer cod = stop.getCodAmount();
                codAmount = cod != null ? cod : 0;
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", stop.getId());
            m.put("orderId", order != null ? order.getId() : null);
            m.put("trackingNumber", trackingNumber != null ? trackingNumber : "");
            m.put("stopType", stop.getStopType() != null ? stop.getStopType().name() : "DELIVERY");
            m.put("recipientName", recipientName != null ? recipientName : "");
            m.put("recipientPhone", recipientPhone != null ? recipientPhone : "");
            m.put("recipientAddress", recipientFullAddress);
            m.put("recipientFullAddress", recipientFullAddress);
            m.put("latitude", stop.getRecipientLatitude());
            m.put("longitude", stop.getRecipientLongitude());
            m.put("codAmount", codAmount);
            m.put("priority", "HIGH".equalsIgnoreCase(stop.getPriority()) ? "urgent" : "normal");
            m.put("serviceType", order != null && order.getServiceType() != null
                    ? order.getServiceType().getName() : "Tiêu chuẩn");
            m.put("stopSequence", stop.getStopSequence());
            m.put("etaTime", stop.getEtaTime());
            m.put("etaMinutesFromStart", stop.getEtaMinutesFromStart());
            m.put("stopStatus", stop.getStopStatus() != null ? stop.getStopStatus().name() : "PENDING");
            m.put("isInserted", stop.getIsInserted() != null ? stop.getIsInserted() : false);
            m.put("insertedReason", stop.getInsertedReason());
            m.put("legDistanceKm", stop.getLegDistanceKm() != null ? stop.getLegDistanceKm().doubleValue() : null);
            m.put("legDurationMinutes", stop.getLegDurationMinutes());
            m.put("actualArrivedAt", stop.getActualArrivedAt());
            m.put("actualCompletedAt", stop.getActualCompletedAt());
            if (order != null) {
                m.put("status", order.getStatus() == OrderStatus.DELIVERED ? "completed" : "pending");
            } else {
                RouteStopStatus ss = stop.getStopStatus();
                m.put("status", ss != null ? ss.name().toLowerCase() : "pending");
            }
            return m;
        }).toList();

        AiRoutePlan plan = aiRoute.getPlan();
        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("id", aiRoute.getId());
        routeInfo.put("planId", plan != null ? plan.getId() : null);
        routeInfo.put("planCode", plan != null ? plan.getPlanCode() : null);
        routeInfo.put("name", aiRoute.getShipperName() != null ? "Tuyến AI - " + aiRoute.getShipperName() : "Tuyến AI");
        routeInfo.put("startLocation", employee.getOffice() != null ? employee.getOffice().getName() : "");
        // totalStops = DELIVERY + PICKUP (không tính RETURN_TO_OFFICE)
        routeInfo.put("totalStops", visibleStops.size());
        routeInfo.put("completedStops", (int) visibleStops.stream()
                .filter(s -> s.getStopStatus() == RouteStopStatus.COMPLETED)
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
        routeInfo.put("routeMode", aiRoute.getRouteMode() != null ? aiRoute.getRouteMode().name() : "CLOSED_LOOP");
        routeInfo.put("returnToOffice", aiRoute.getReturnToOffice() != null ? aiRoute.getReturnToOffice() : true);
        routeInfo.put("routeVersion", aiRoute.getRouteVersion() != null ? aiRoute.getRouteVersion() : 1);
        routeInfo.put("isActive", aiRoute.getIsActive() != null ? aiRoute.getIsActive() : true);
        routeInfo.put("parentRouteId", aiRoute.getParentRouteId());
        routeInfo.put("currentLatitude", aiRoute.getCurrentLatitude());
        routeInfo.put("currentLongitude", aiRoute.getCurrentLongitude());
        routeInfo.put("actualStartedAt", aiRoute.getActualStartedAt());
        routeInfo.put("actualCompletedAt", aiRoute.getActualCompletedAt());
        routeInfo.put("reoptimizedAt", aiRoute.getReoptimizedAt());
        routeInfo.put("reoptimizeReason", aiRoute.getReoptimizeReason());

        Office office = employee.getOffice();
        Map<String, Object> officeMap = new HashMap<>();
        officeMap.put("id", office != null ? office.getId() : null);
        officeMap.put("name", office != null ? office.getName() : "");
        officeMap.put("latitude", office != null ? office.getLatitude() : null);
        officeMap.put("longitude", office != null ? office.getLongitude() : null);

        Map<String, Object> result = new HashMap<>();
        result.put("routeInfo", routeInfo);
        result.put("deliveryStops", deliveryStops);
        result.put("office", officeMap);
        return result;
    }

    private Map<String, Object> buildShipmentRouteResponseData(Employee employee, Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        // Lấy ShipmentOrder theo shipmentId
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        if (shipmentOrders == null) {
            shipmentOrders = List.of();
        }

        // Phase 3A: sort theo stopSequence (snapshot từ AI) trước, fallback createdAt.
        // Comparator: NULL stopSequence xuống cuối; trong cùng nhóm sort theo createdAt.
        List<ShipmentOrder> sortedSos = new ArrayList<>(shipmentOrders);
        sortedSos.sort((a, b) -> {
            Integer sa = a.getStopSequence();
            Integer sb = b.getStopSequence();

            boolean aNull = (sa == null);
            boolean bNull = (sb == null);
            if (aNull && !bNull) return 1;
            if (!aNull && bNull) return -1;
            if (!aNull && !bNull) {
                int cmp = Integer.compare(sa, sb);
                if (cmp != 0) return cmp;
            }

            // Tie-breaker: order.createdAt ASC (ổn định cho legacy data chưa có stopSequence)
            Order oa = a.getOrder();
            Order ob = b.getOrder();
            if (oa == null && ob == null) return 0;
            if (oa == null) return 1;
            if (ob == null) return -1;
            if (oa.getCreatedAt() == null && ob.getCreatedAt() == null) return 0;
            if (oa.getCreatedAt() == null) return 1;
            if (ob.getCreatedAt() == null) return -1;
            return oa.getCreatedAt().compareTo(ob.getCreatedAt());
        });

        List<Map<String, Object>> deliveryStops = new ArrayList<>();
        int totalCOD = 0;
        int completedStops = 0;

        for (ShipmentOrder so : sortedSos) {
            Order order = so.getOrder();
            if (order == null) {
                continue;
            }

            // Phase 3A: stopType snapshot từ ShipmentOrder.stopType (mặc định DELIVERY nếu null)
            String stopType = so.getStopType() != null ? so.getStopType().name() : "DELIVERY";
            boolean isPickup = "PICKUP".equalsIgnoreCase(stopType);

            String senderFullAddress = resolveSenderFullAddress(order);
            String recipientFullAddress = resolveRecipientFullAddress(order);
            int codAmount = order.getCod() != null ? order.getCod() : 0;

            OrderStatus os = order.getStatus();

            // Phase pickup-status mapping:
            //   DELIVERED => completed (đã giao - tính vào hoàn thành route)
            //   PICKED_UP / AT_ORIGIN_OFFICE => "completed" mapped nhưng KHÔNG tính completedStops
            //     (PICKED_UP = đã lấy hàng, còn phải giao tiếp)
            //   PICKUP_FAILED_FINAL / DELIVERY_FAILED_FINAL / RETURNED => final (filter ra khỏi route)
            //   PICKING_UP / DELIVERING / RETURNING => in_progress
            String stopStatus = "pending";
            if (os == OrderStatus.DELIVERED) {
                stopStatus = "completed";
                completedStops++;
            } else if (os == OrderStatus.PICKED_UP
                    || os == OrderStatus.AT_ORIGIN_OFFICE) {
                // PICKED_UP = đã lấy hàng, vẫn đang trên đường giao -> KHÔNG tính completed
                stopStatus = "completed";
            } else if (os == OrderStatus.DELIVERING
                    || os == OrderStatus.PICKING_UP
                    || os == OrderStatus.RETURNING) {
                stopStatus = "in_progress";
            } else if (os == OrderStatus.PICKUP_FAILED_FINAL
                    || os == OrderStatus.DELIVERY_FAILED_FINAL
                    || os == OrderStatus.RETURN_FAILED_FINAL
                    || os == OrderStatus.CANCELLED
                    || os == OrderStatus.RETURNED) {
                stopStatus = "final";
            }

            // Phase PICKUP-correctness: PICKUP stop phải dùng sender address/contact,
            // DELIVERY stop dùng recipient. Luôn populate cả 2 để FE flexible.
            Map<String, Object> stop = new HashMap<>();
            stop.put("id", order.getId());
            stop.put("orderId", order.getId());
            stop.put("shipmentOrderId", so.getId() != null ? so.getId().getOrderId() : null);
            stop.put("trackingNumber", order.getTrackingNumber());
            stop.put("stopType", stopType);

            // Recipient (cho DELIVERY stop / fallback)
            stop.put("recipientName", order.getRecipientName());
            stop.put("recipientPhone", order.getRecipientPhone());
            stop.put("recipientAddress", recipientFullAddress);
            stop.put("recipientFullAddress", recipientFullAddress);
            stop.put("recipientLatitude", order.getRecipientLatitude());
            stop.put("recipientLongitude", order.getRecipientLongitude());

            // Sender (cho PICKUP stop)
            stop.put("senderName", order.getSenderName());
            stop.put("senderPhone", order.getSenderPhone());
            stop.put("senderAddress", senderFullAddress);
            stop.put("senderFullAddress", senderFullAddress);
            stop.put("senderLatitude", order.getSenderLatitude());
            stop.put("senderLongitude", order.getSenderLongitude());

            // Contact/address/lat-lng chính của stop: PICKUP -> sender, DELIVERY -> recipient
            stop.put("contactName", isPickup ? order.getSenderName() : order.getRecipientName());
            stop.put("contactPhone", isPickup ? order.getSenderPhone() : order.getRecipientPhone());
            stop.put("contactAddress", isPickup ? senderFullAddress : recipientFullAddress);
            stop.put("latitude", isPickup ? order.getSenderLatitude() : order.getRecipientLatitude());
            stop.put("longitude", isPickup ? order.getSenderLongitude() : order.getRecipientLongitude());

            stop.put("codAmount", codAmount);
            stop.put("priority", codAmount > 1000000 ? "urgent" : "normal");
            stop.put("serviceType", order.getServiceType() != null
                    ? order.getServiceType().getName() : "Tiêu chuẩn");
            stop.put("status", stopStatus);
            stop.put("orderStatus", os != null ? os.name() : null);
            // Phase 3A: snapshot từ ShipmentOrder (Phase 1 confirmPlan copy từ AiRoutePlanStop)
            stop.put("stopSequence", so.getStopSequence());
            stop.put("etaTime", so.getEtaTime());
            stop.put("etaMinutesFromStart", so.getEtaMinutesFromStart());
            stop.put("legDistanceKm", so.getLegDistanceKm());
            stop.put("legDurationMinutes", so.getLegDurationMinutes());

            deliveryStops.add(stop);
            totalCOD += codAmount;
        }

        // Compute estimatedDuration + totalDistance từ snapshot AI đã ghi trên ShipmentOrder
        // (etaMinutesFromStart, legDistanceKm) — không dùng heuristic cũ "stops × 20" vì sai lệch lớn.
        // Fallback: shipment cũ chưa có snapshot (maxEtaMin == 0) → dùng heuristic để tránh hiển thị 0.
        int maxEtaMin = 0;
        double totalLegKm = 0.0;
        for (Map<String, Object> s : deliveryStops) {
            Object e = s.get("etaMinutesFromStart");
            if (e instanceof Number n && n.intValue() > maxEtaMin) {
                maxEtaMin = n.intValue();
            }
            Object k = s.get("legDistanceKm");
            if (k instanceof Number n) {
                totalLegKm += n.doubleValue();
            }
        }
        double totalDistanceKm = Math.round(totalLegKm * 100.0) / 100.0;
        double estimatedMinutes = maxEtaMin > 0 ? maxEtaMin : deliveryStops.size() * 20.0;

        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("id", shipment.getId());
        routeInfo.put("shipmentId", shipment.getId());
        routeInfo.put("shipmentCode", shipment.getCode());
        routeInfo.put("shipmentStatus", shipment.getStatus() != null ? shipment.getStatus().name() : null);
        routeInfo.put("source", "SHIPMENT");
        routeInfo.put("status", shipment.getStatus() == ShipmentStatus.IN_TRANSIT ? "in_progress" : "pending");
        routeInfo.put("name", "Chuyến " + (shipment.getCode() != null ? shipment.getCode() : shipment.getId()));
        routeInfo.put("totalStops", deliveryStops.size());
        routeInfo.put("completedStops", completedStops);
        routeInfo.put("estimatedDuration", estimatedMinutes);
        routeInfo.put("totalDistance", totalDistanceKm);
        routeInfo.put("totalCOD", totalCOD);
        routeInfo.put("startLocation",
                employee.getOffice() != null ? employee.getOffice().getName() : "");

        Office office = employee.getOffice();
        Map<String, Object> officeMap = new HashMap<>();
        officeMap.put("id", office != null ? office.getId() : null);
        officeMap.put("name", office != null ? office.getName() : "");
        officeMap.put("latitude", office != null ? office.getLatitude() : null);
        officeMap.put("longitude", office != null ? office.getLongitude() : null);

        Map<String, Object> result = new HashMap<>();
        result.put("routeInfo", routeInfo);
        result.put("deliveryStops", deliveryStops);
        result.put("office", officeMap);
        result.put("source", "SHIPMENT");
        return result;
    }

    public void startRoute(Integer routeId) {
        if (routeId == null) {
            throw new AppException(com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_FOUND);
        }
        // routeId thực tế là AiRoutePlanRoute.id (Long), KHÔNG phải Shipment.id (Integer).
        // Nếu frontend gọi /route/start thì phải gửi shipmentId riêng qua /shipments/{id}/start.
        // Method này giữ để tương thích ngược cũ nhưng KHÔNG tự ý map routeId -> shipmentId nữa.
        throw new AppException(com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_FOUND,
                "routeId không phải shipmentId. Vui lòng gọi endpoint /shipments/{id}/start với shipmentId.");
    }

    /**
     * Re-optimize cho shipment-based route (Phase 3B cho Shipment-centric flow).
     *
     * Khác với reOptimizeRoute() (AI-source):
     *  - Không tạo AiRoutePlanRoute mới.
     *  - Cập nhật trực tiếp ShipmentOrder: stopSequence, etaTime, etaMinutesFromStart,
     *    legDistanceKm, legDurationMinutes.
     *  - Không thay đổi Order.status (giữ nguyên).
     *
     * Pre-conditions:
     *  - Current user là shipper được gán shipment (shipment.employee.id == employee.id).
     *  - shipment.type == DELIVERY.
     *  - shipment.status IN (PENDING, IN_TRANSIT).
     *  - Còn ít nhất 1 ShipmentOrder chưa ở trạng thái terminal.
     */
    @Transactional
    public Map<String, Object> reOptimizeShipmentRoute(ShipperReOptimizeRequest request) {
        Long startMs = System.currentTimeMillis();
        Employee employee = getCurrentEmployee();

        try {
            Office office = employee.getOffice();
            if (office == null) {
                throw new AppException(CommonErrorCode.BAD_REQUEST, "Shipper chưa được gán bưu cục");
            }

            Integer shipmentId = request.getShipmentId();
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new AppException(
                            com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_FOUND,
                            "Không tìm thấy shipment id=" + shipmentId));

            // Validate shipper is assigned to this shipment
            if (shipment.getEmployee() == null
                    || !Objects.equals(shipment.getEmployee().getId(), employee.getId())) {
                throw new AppException(CommonErrorCode.FORBIDDEN,
                        "Bạn không được gán cho shipment này");
            }

            // Validate shipment type
            if (shipment.getType() != ShipmentType.DELIVERY) {
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "Chỉ hỗ trợ re-optimize cho shipment DELIVERY (shipper)");
            }

            // Validate shipment status
            ShipmentStatus shipStatus = shipment.getStatus();
            if (shipStatus != ShipmentStatus.PENDING && shipStatus != ShipmentStatus.IN_TRANSIT) {
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "Shipment phải ở trạng thái PENDING hoặc IN_TRANSIT để tái tối ưu (hiện tại: "
                                + shipStatus + ")");
            }

            log.info("reOptimizeShipmentRoute: employeeId={} shipmentId={} status={}",
                    employee.getId(), shipmentId, shipStatus);

            // Load ShipmentOrder theo shipmentId, sort theo stopSequence hiện tại (fallback orderId).
            List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentIdOrderByStopSequenceAsc(shipmentId);
            if (shipmentOrders == null || shipmentOrders.isEmpty()) {
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "Shipment không có đơn hàng nào để tái tối ưu");
            }

            // Exclude terminal orders (không gửi cho AI)
            Set<OrderStatus> terminal = Set.of(
                    OrderStatus.DELIVERED,
                    OrderStatus.CANCELLED,
                    OrderStatus.RETURNED,
                    OrderStatus.AT_ORIGIN_OFFICE,
                    OrderStatus.RETURN_AT_ORIGIN_OFFICE
            );

            List<ShipmentOrder> eligible = new ArrayList<>();
            for (ShipmentOrder so : shipmentOrders) {
                Order o = so.getOrder();
                if (o == null) continue;
                OrderStatus st = o.getStatus();
                if (st == null) continue;
                if (terminal.contains(st)) continue;
                eligible.add(so);
            }

            if (eligible.isEmpty()) {
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "Không còn đơn nào (loại trừ DELIVERED/CANCELLED/RETURNED/AT_ORIGIN_OFFICE/RETURN_AT_ORIGIN_OFFICE) để tái tối ưu");
            }

            // Build AI request
            boolean hasValidGps = request.getCurrentLatitude() != null
                    && request.getCurrentLongitude() != null
                    && hasValidLatLng(request.getCurrentLatitude(), request.getCurrentLongitude());

            AiLocationDto startLocation;
            if (hasValidGps) {
                startLocation = AiLocationDto.builder()
                        .type("CURRENT_POSITION")
                        .latitude(request.getCurrentLatitude())
                        .longitude(request.getCurrentLongitude())
                        .name(request.getCurrentAddress())
                        .build();
            } else {
                startLocation = AiLocationDto.builder()
                        .type("OFFICE")
                        .id(office.getId())
                        .name(office.getName())
                        .latitude(office.getLatitude().doubleValue())
                        .longitude(office.getLongitude().doubleValue())
                        .build();
            }

            AiLocationDto endLocation = AiLocationDto.builder()
                    .type("OFFICE")
                    .id(office.getId())
                    .name(office.getName())
                    .latitude(office.getLatitude().doubleValue())
                    .longitude(office.getLongitude().doubleValue())
                    .build();

            // Build stopInputs từ eligible ShipmentOrder
            // DELIVERY → recipient coords; PICKUP → sender coords.
            List<AiRouteStopInputDto> stopInputs = new ArrayList<>();
            Map<Integer, ShipmentOrder> oldSoMap = new HashMap<>();

            for (ShipmentOrder so : eligible) {
                Order order = so.getOrder();
                if (order == null) continue;

                RouteStopType stopType = so.getStopType() != null ? so.getStopType() : RouteStopType.DELIVERY;
                Double lat;
                Double lng;
                String name;
                String phone;
                String address;

                if (stopType == RouteStopType.PICKUP) {
                    lat = order.getSenderLatitude();
                    lng = order.getSenderLongitude();
                    name = order.getSenderName();
                    phone = order.getSenderPhone();
                    address = order.getSenderFullAddress();
                } else {
                    lat = order.getRecipientLatitude();
                    lng = order.getRecipientLongitude();
                    name = order.getRecipientName();
                    phone = order.getRecipientPhone();
                    address = order.getRecipientFullAddress();
                }

                if (!hasValidLatLng(lat, lng)) {
                    log.warn("reOptimizeShipmentRoute: skip orderId={} due to invalid GPS (lat={}, lng={})",
                            order.getId(), lat, lng);
                    continue;
                }

                Integer codAmount = order.getCod() != null ? order.getCod() : 0;

                AiRouteStopInputDto input = AiRouteStopInputDto.builder()
                        .stopId(so.getId() != null ? so.getId().getOrderId().longValue() : null)
                        .orderId(order.getId())
                        .trackingNumber(order.getTrackingNumber())
                        .stopType(stopType.name())
                        .recipientName(name)
                        .recipientPhone(phone)
                        .address(address)
                        .latitude(lat)
                        .longitude(lng)
                        .codAmount(codAmount)
                        .priority(so.getStopType() != null ? "normal" : "normal")
                        .serviceTimeMinutes(5)
                        .weightKg(order.getWeight() != null ? order.getWeight().doubleValue() : 1.0)
                        .build();
                stopInputs.add(input);
                oldSoMap.put(order.getId(), so);
            }

            if (stopInputs.isEmpty()) {
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "Tất cả đơn hợp lệ đều thiếu tọa độ GPS, không thể gửi AI");
            }

            AiShipperInputDto shipper = AiShipperInputDto.builder()
                    .id(employee.getUser().getId())
                    .employeeId(employee.getId())
                    .name((employee.getUser().getFirstName() + " " + employee.getUser().getLastName()).trim())
                    .capacity(20)
                    .speedKmh(25.0)
                    .fuelCostPerKm(3000.0)
                    .startTime("08:00")
                    .assignments(List.of())
                    .build();

            AiOfficeLocationDto officeDto = AiOfficeLocationDto.builder()
                    .id(office.getId())
                    .name(office.getName())
                    .address(office.getDetail())
                    .latitude(office.getLatitude().doubleValue())
                    .longitude(office.getLongitude().doubleValue())
                    .build();

            AiRouteOptimizationRequestDto aiRequest = AiRouteOptimizationRequestDto.builder()
                    .office(officeDto)
                    .startLocation(startLocation)
                    .endLocation(endLocation)
                    .returnToOffice(request.getReturnToOffice() != null ? request.getReturnToOffice() : true)
                    .routeMode("CLOSED_LOOP")
                    .optimizationScope("SHIPPER_LOCAL")
                    .shippers(List.of(shipper))
                    .stops(stopInputs)
                    .options(Map.of("ortools_time_limit_seconds", 8))
                    .build();

            log.info("reOptimizeShipmentRoute: AI request → stops={} shipmentId={}",
                    stopInputs.size(), shipmentId);

            AiRouteOptimizationResponseDto aiResponse;
            try {
                aiResponse = aiServiceClient.optimizeRoutes(aiRequest);
            } catch (Exception e) {
                log.error("AI re-optimize (shipment) failed: {}", e.getMessage(), e);
                throw new AppException(AiRouteErrorCode.AI_SERVICE_UNAVAILABLE,
                        "AI không phản hồi: " + e.getMessage());
            }

            if (aiResponse.getRoutes() == null || aiResponse.getRoutes().isEmpty()) {
                throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR, "AI không tìm được tuyến nào");
            }

            AiShipperRouteOutputDto newAiRoute = aiResponse.getRoutes().get(0);

            // Apply AI output: cập nhật ShipmentOrder in-place
            // QUAN TRỌNG: KHÔNG tin tưởng AI trả về etaTime / etaMinutesFromStart vì
            // AI tính theo shipper.startTime="08:00" mặc định → sai hoàn toàn khi
            // shipper đang chạy thực tế (vd 14:30). Phải recompute từ LocalDateTime.now()
            // + cumulative leg duration Haversine (giống computeLegBasedEtas của AI-source flow).
            int seq = 1;
            int applied = 0;

            // Build list các ShipmentOrder (đã theo thứ tự AI trả về) để recompute ETA.
            List<ShipmentOrder> orderedForEta = new ArrayList<>();
            for (AiRouteStopOutputDto stopDto : newAiRoute.getStops()) {
                String stopTypeStr = stopDto.getStopType();
                boolean isReturnStop = "RETURN_TO_OFFICE".equalsIgnoreCase(stopTypeStr)
                        || "OFFICE".equalsIgnoreCase(stopTypeStr)
                        || "DEPOT".equalsIgnoreCase(stopTypeStr);
                if (stopDto.getOrderId() == null || isReturnStop) {
                    continue;
                }
                ShipmentOrder so = oldSoMap.get(stopDto.getOrderId());
                if (so == null) {
                    log.warn("reOptimizeShipmentRoute: AI returned unknown orderId={}, skip",
                            stopDto.getOrderId());
                    continue;
                }

                // Ưu tiên dùng legDurationMinutes từ AI nếu hợp lệ (>0).
                // Fallback Haversine/speed nếu AI trả null hoặc 0.
                Integer aiLegMin = stopDto.getLegDurationMinutes();
                Double aiLegKm = stopDto.getLegDistanceKm();

                Order ord = so.getOrder();
                Double stopLat = null, stopLng = null;
                RouteStopType stopType = so.getStopType() != null ? so.getStopType() : RouteStopType.DELIVERY;
                if (ord != null) {
                    if (stopType == RouteStopType.PICKUP) {
                        stopLat = ord.getSenderLatitude();
                        stopLng = ord.getSenderLongitude();
                    } else {
                        stopLat = ord.getRecipientLatitude();
                        stopLng = ord.getRecipientLongitude();
                    }
                }

                Double distanceKm = (aiLegKm != null && aiLegKm > 0) ? aiLegKm : null;
                Integer legMin = (aiLegMin != null && aiLegMin > 0) ? aiLegMin : null;

                if ((distanceKm == null || legMin == null) && hasValidLatLng(stopLat, stopLng)) {
                    // Recompute bằng Haversine
                    double dKm = (distanceKm != null) ? distanceKm : haversineKm(
                            request.getCurrentLatitude() != null ? request.getCurrentLatitude() : office.getLatitude().doubleValue(),
                            request.getCurrentLongitude() != null ? request.getCurrentLongitude() : office.getLongitude().doubleValue(),
                            stopLat, stopLng);
                    double speed = 25.0;
                    int computedMin = (int) Math.round(dKm / speed * 60.0);
                    if (computedMin < 1) computedMin = 1;
                    distanceKm = dKm;
                    legMin = computedMin;
                }

                if (legMin == null) legMin = 5; // fallback cuối
                if (distanceKm == null) distanceKm = legMin / 60.0 * 25.0;

                so.setStopSequence(seq++);
                so.setLegDistanceKm(BigDecimal.valueOf(Math.round(distanceKm * 100.0) / 100.0));
                so.setLegDurationMinutes(legMin);
                // etaTime / etaMinutesFromStart sẽ set sau khi cumulative xong (xem loop dưới)
                orderedForEta.add(so);
                applied++;
            }

            // Recompute cumulative ETA từ now() theo thứ tự orderedForEta
            LocalDateTime baseTime = LocalDateTime.now();
            Double startLat = hasValidLatLng(request.getCurrentLatitude(), request.getCurrentLongitude())
                    ? request.getCurrentLatitude()
                    : (office.getLatitude() != null ? office.getLatitude().doubleValue() : null);
            Double startLng = hasValidLatLng(request.getCurrentLatitude(), request.getCurrentLongitude())
                    ? request.getCurrentLongitude()
                    : (office.getLongitude() != null ? office.getLongitude().doubleValue() : null);
            Double fromLat = startLat;
            Double fromLng = startLng;
            LocalDateTime currentEta = baseTime;
            DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");

            for (ShipmentOrder so : orderedForEta) {
                Order ord = so.getOrder();
                Double stopLat = null, stopLng = null;
                if (ord != null) {
                    RouteStopType stt = so.getStopType() != null ? so.getStopType() : RouteStopType.DELIVERY;
                    if (stt == RouteStopType.PICKUP) {
                        stopLat = ord.getSenderLatitude();
                        stopLng = ord.getSenderLongitude();
                    } else {
                        stopLat = ord.getRecipientLatitude();
                        stopLng = ord.getRecipientLongitude();
                    }
                }

                int legMin = so.getLegDurationMinutes() != null ? so.getLegDurationMinutes() : 5;
                if (hasValidLatLng(fromLat, fromLng) && hasValidLatLng(stopLat, stopLng)) {
                    double dKm = haversineKm(fromLat, fromLng, stopLat, stopLng);
                    int computedMin = (int) Math.round(dKm / 25.0 * 60.0);
                    if (computedMin < 1) computedMin = 1;
                    legMin = computedMin;
                    so.setLegDistanceKm(BigDecimal.valueOf(Math.round(dKm * 100.0) / 100.0));
                }
                so.setLegDurationMinutes(legMin);

                currentEta = currentEta.plusMinutes(legMin);
                so.setEtaTime(currentEta.format(HHmm));
                int minsFromStart = (int) java.time.Duration.between(baseTime, currentEta).toMinutes();
                so.setEtaMinutesFromStart(minsFromStart);

                // Service time 5 phút (giống stopInput bên trên)
                currentEta = currentEta.plusMinutes(5);

                shipmentOrderRepository.save(so);

                fromLat = stopLat;
                fromLng = stopLng;
            }

            if (applied == 0) {
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "AI không trả về điểm dừng nào khớp với shipment. Không có gì thay đổi.");
            }

            log.info("reOptimizeShipmentRoute: shipmentId={} updated {} ShipmentOrder rows in {}ms",
                    shipmentId, applied, System.currentTimeMillis() - startMs);

            // Build response cùng shape với reOptimizeRoute để frontend vẫn dùng chung
            Map<String, Object> result = buildShipmentRouteResponseData(employee, shipment);

            long elapsed = System.currentTimeMillis() - startMs;
            log.info("reOptimizeShipmentRoute: completed shipmentId={} totalMs={} applied={}",
                    shipmentId, elapsed, applied);

            return result;

        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("reOptimizeShipmentRoute failed: {}", ex.getMessage(), ex);
            throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "Lỗi khi tái tối ưu shipment route: " + ex.getClass().getSimpleName()
                            + " - " + ex.getMessage());
        }
    }

    // ===================== SHIPPER RE-OPTIMIZE =====================
    @Transactional
    public Map<String, Object> reOptimizeRoute(ShipperReOptimizeRequest request) {
        // Shipment-based route: DeliveryRoute.tsx gửi shipmentId khi routeInfo.source === "SHIPMENT"
        if (request.getShipmentId() != null) {
            return reOptimizeShipmentRoute(request);
        }
        Long startMs = System.currentTimeMillis();
        Employee employee = getCurrentEmployee();

        try {
            Office office = employee.getOffice();
            if (office == null) {
                throw new AppException(CommonErrorCode.BAD_REQUEST, "Shipper chưa được gán bưu cục");
            }

            AiRoutePlanRoute currentRoute;
            if (request.getRouteId() != null) {
                currentRoute = aiRoutePlanRouteRepository.findByIdWithDetails(request.getRouteId().longValue())
                        .orElseThrow(() -> new AppException(AiRouteErrorCode.AI_PLAN_NOT_FOUND));
                if (!Objects.equals(currentRoute.getShipperEmployeeId(), employee.getId())) {
                    throw new AppException(CommonErrorCode.FORBIDDEN, "Bạn không có quyền tái tối ưu tuyến này");
                }
            } else {
                List<AiRoutePlanRoute> routes = aiRoutePlanRouteRepository.findActiveConfirmedRoutesForShipper(
                        employee.getId(), AiRoutePlanStatus.CONFIRMED);
                if (routes.isEmpty()) {
                    throw new AppException(AiRouteErrorCode.AI_PLAN_NOT_FOUND, "Không có tuyến nào để tái tối ưu");
                }
                if (routes.size() > 1) {
                    throw new AppException(CommonErrorCode.BAD_REQUEST,
                            "Có " + routes.size() + " tuyến đang active. Vui lòng chọn tuyến cụ thể (routeId).");
                }
                currentRoute = routes.get(0);
            }

            if (currentRoute.getIsActive() == null || !currentRoute.getIsActive()) {
                long completedStops = currentRoute.getStops().stream()
                        .filter(s -> s.getStopStatus() == RouteStopStatus.COMPLETED)
                        .count();
                long totalStops = currentRoute.getStops().size();
                boolean allCompleted = totalStops > 0 && completedStops == totalStops;

                log.warn("[BE_ROUTE_BLOCKED] routeId={} isActive={} completedAt={} completedStops={}/{} allCompleted={} reoptimizedAt={} totalStops={}",
                        currentRoute.getId(),
                        currentRoute.getIsActive(),
                        currentRoute.getActualCompletedAt(),
                        completedStops, totalStops, allCompleted,
                        currentRoute.getReoptimizedAt(),
                        currentRoute.getStops().size());

                if (currentRoute.getActualCompletedAt() != null) {
                    throw new AppException(AiRouteErrorCode.AI_INVALID_PLAN_STATUS,
                            "Tuyến đã hoàn tất (completedAt=" + currentRoute.getActualCompletedAt() + ")");
                }
                if (allCompleted) {
                    throw new AppException(AiRouteErrorCode.AI_INVALID_PLAN_STATUS,
                            "Tất cả điểm dừng đã hoàn thành, không thể tái tối ưu");
                }
                log.info("[BE_ROUTE_REACTIVATING] routeId={} isActive=false but can be re-optimized",
                        currentRoute.getId());
            }

            log.info("reOptimizeRoute: employeeId={} requestRouteId={} currentRouteId={} planId={} shipperEmployeeId={}",
                    employee.getId(), request.getRouteId(), currentRoute.getId(),
                    currentRoute.getPlan(), currentRoute.getShipperEmployeeId());

            List<AiRoutePlanStop> allStops = currentRoute.getStops();
            log.debug("reOptimizeRoute: totalStops={}", allStops.size());

            List<AiRoutePlanStop> remainingStops = allStops.stream()
                    .filter(s -> s.getStopType() != RouteStopType.RETURN_TO_OFFICE)
                    .filter(s -> hasValidLatLng(s.getRecipientLatitude(), s.getRecipientLongitude()))
                    .filter(s -> s.getStopStatus() == RouteStopStatus.PENDING
                            || s.getStopStatus() == RouteStopStatus.ARRIVED)
                    .sorted(Comparator.comparing(AiRoutePlanStop::getStopSequence))
                    .toList();

            if (remainingStops.isEmpty()) {
                log.warn("reOptimizeRoute: remainingStops is empty! totalStops={}", allStops.size());
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "Không còn điểm dừng nào để tái tối ưu. totalStops=" + allStops.size()
                                + " remainingAfterFilter=0. Xem log chi tiết.");
            }

            AiLocationDto startLocation;
            boolean hasValidGps = request.getCurrentLatitude() != null
                    && request.getCurrentLongitude() != null
                    && hasValidLatLng(request.getCurrentLatitude(), request.getCurrentLongitude());

            if (hasValidGps) {
                startLocation = AiLocationDto.builder()
                        .type("CURRENT_POSITION")
                        .latitude(request.getCurrentLatitude())
                        .longitude(request.getCurrentLongitude())
                        .name(request.getCurrentAddress())
                        .build();
            } else {
                startLocation = AiLocationDto.builder()
                        .type("OFFICE")
                        .id(office.getId())
                        .name(office.getName())
                        .latitude(office.getLatitude().doubleValue())
                        .longitude(office.getLongitude().doubleValue())
                        .build();
                log.warn("reOptimizeRoute: GPS không hợp lệ, dùng office làm startLocation. lat={} lng={}",
                        request.getCurrentLatitude(), request.getCurrentLongitude());
            }

            AiLocationDto endLocation = AiLocationDto.builder()
                    .type("OFFICE")
                    .id(office.getId())
                    .name(office.getName())
                    .latitude(office.getLatitude().doubleValue())
                    .longitude(office.getLongitude().doubleValue())
                    .build();

            List<AiRouteStopInputDto> stopInputs = remainingStops.stream()
                .map(s -> {
                    RouteStopType stopType = s.getStopType();
                    Order order = s.getOrder();
                    if (stopType == RouteStopType.PICKUP && order != null) {
                        Double senderLat = order.getSenderLatitude();
                        Double senderLng = order.getSenderLongitude();
                        return AiRouteStopInputDto.builder()
                                .stopId(s.getId())
                                .orderId(order.getId())
                                .trackingNumber(order.getTrackingNumber())
                                .stopType("PICKUP")
                                .recipientName(order.getSenderName())
                                .recipientPhone(order.getSenderPhone())
                                .address(order.getSenderFullAddress())
                                .latitude(senderLat)
                                .longitude(senderLng)
                                .codAmount(0)
                                .priority(s.getPriority())
                                .serviceTimeMinutes(5)
                                .weightKg(order.getWeight() != null ? order.getWeight().doubleValue() : 1.0)
                                .build();
                    } else {
                        Integer cod = s.getCodAmount();
                        Integer orderId = (order != null && order.getId() != null) ? order.getId().intValue() : null;
                        return AiRouteStopInputDto.builder()
                                .stopId(s.getId())
                                .orderId(orderId)
                                .trackingNumber(s.getTrackingNumber())
                                .stopType("DELIVERY")
                                .recipientName(s.getRecipientName())
                                .recipientPhone(s.getRecipientPhone())
                                .address(s.getRecipientAddress())
                                .latitude(s.getRecipientLatitude())
                                .longitude(s.getRecipientLongitude())
                                .codAmount(cod != null ? cod : 0)
                                .priority(s.getPriority())
                                .serviceTimeMinutes(5)
                                .weightKg(1.0)
                                .build();
                    }
                })
                .toList();

            AiShipperInputDto shipper = AiShipperInputDto.builder()
                    .id(employee.getUser().getId())
                    .employeeId(employee.getId())
                    .name((employee.getUser().getFirstName() + " " + employee.getUser().getLastName()).trim())
                    .capacity(20)
                    .speedKmh(25.0)
                    .fuelCostPerKm(3000.0)
                    .startTime("08:00")
                    .assignments(List.of())
                    .build();

            AiOfficeLocationDto officeDto = AiOfficeLocationDto.builder()
                    .id(office.getId())
                    .name(office.getName())
                    .address(office.getDetail())
                    .latitude(office.getLatitude().doubleValue())
                    .longitude(office.getLongitude().doubleValue())
                    .build();

            AiRouteOptimizationRequestDto aiRequest = AiRouteOptimizationRequestDto.builder()
                    .office(officeDto)
                    .startLocation(startLocation)
                    .endLocation(endLocation)
                    .returnToOffice(request.getReturnToOffice() != null ? request.getReturnToOffice() : true)
                    .routeMode("CLOSED_LOOP")
                    .optimizationScope("SHIPPER_LOCAL")
                    .shippers(List.of(shipper))
                    .stops(stopInputs)
                    .options(Map.of("ortools_time_limit_seconds", 8))
                    .build();

            log.info("reOptimizeRoute: AI request → scope={} mode={} returnToOffice={} startType={} stops={} shippers=1 officeId={}",
                    aiRequest.getOptimizationScope(), aiRequest.getRouteMode(),
                    aiRequest.getReturnToOffice(),
                    startLocation.getType(), stopInputs.size(), office.getId());

            AiRouteOptimizationResponseDto aiResponse;
            Long aiCallStartMs = System.currentTimeMillis();
            try {
                aiResponse = aiServiceClient.optimizeRoutes(aiRequest);
            } catch (Exception e) {
                log.error("AI re-optimize failed: {}", e.getMessage(), e);
                throw new AppException(AiRouteErrorCode.AI_SERVICE_UNAVAILABLE, "AI không phản hồi: " + e.getMessage());
            }
            Long aiCallEndMs = System.currentTimeMillis();
            log.info("reOptimizeRoute: AI call completed in {}ms", aiCallEndMs - aiCallStartMs);

            if (aiResponse.getRoutes() == null || aiResponse.getRoutes().isEmpty()) {
                throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR, "AI không tìm được tuyến nào");
            }

            AiShipperRouteOutputDto newAiRoute = aiResponse.getRoutes().get(0);

            // Validate AI output before any DB writes
            long aiDeliveryPickupCount = newAiRoute.getStops().stream()
                    .filter(s -> {
                        String t = s.getStopType();
                        return t == null || (!t.equalsIgnoreCase("RETURN_TO_OFFICE")
                                && !t.equalsIgnoreCase("OFFICE")
                                && !t.equalsIgnoreCase("DEPOT"));
                    })
                    .count();

            log.debug("[BE_AI_VALIDATION] aiStops={} deliveryPickupCount={}",
                    newAiRoute.getStops().size(), aiDeliveryPickupCount);

            if (aiDeliveryPickupCount == 0) {
                log.warn("[BE_NO_DELIVERY_STOPS] AI returned 0 DELIVERY/PICKUP stops. remainingStops sent to AI={}. Not deactivating old route.",
                        remainingStops.size());
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "AI không trả về điểm giao hàng nào. Còn " + remainingStops.size()
                                + " điểm dừng chưa hoàn thành trên tuyến cũ. Không thể tái tối ưu.");
            }

            // Build oldStopMap BEFORE deactivating old route
            Map<Integer, AiRoutePlanStop> oldStopMap = remainingStops.stream()
                    .collect(Collectors.toMap(
                            s -> s.getOrder() != null ? s.getOrder().getId() : s.getId().intValue(),
                            s -> s,
                            (a, b) -> a
                    ));

            log.debug("reOptimizeRoute: oldStopMap size={}", oldStopMap.size());

            List<AiRoutePlanStop> newStops = new ArrayList<>();
            int seq = 1;

            for (AiRouteStopOutputDto stopDto : newAiRoute.getStops()) {
                String stopTypeStr = stopDto.getStopType();
                boolean isReturnStop = "RETURN_TO_OFFICE".equalsIgnoreCase(stopTypeStr)
                        || "OFFICE".equalsIgnoreCase(stopTypeStr)
                        || "DEPOT".equalsIgnoreCase(stopTypeStr);

                if (stopDto.getOrderId() == null || isReturnStop) {
                    continue;
                }

                AiRoutePlanStop oldStop = oldStopMap.get(stopDto.getOrderId());
                if (oldStop == null) {
                    throw new AppException(CommonErrorCode.BAD_REQUEST,
                            "AI trả về đơn không có trong danh sách: orderId=" + stopDto.getOrderId()
                                    + " tracking=" + stopDto.getTrackingNumber()
                                    + ". Các đơn hàng hợp lệ: " + oldStopMap.keySet());
                }

                AiRoutePlanStop newStop = new AiRoutePlanStop();
                newStop.setRoute(null);
                newStop.setOrder(oldStop.getOrder());
                newStop.setStopType(stopTypeStr != null ? RouteStopType.valueOf(stopTypeStr) : RouteStopType.DELIVERY);
                newStop.setStopSequence(seq++);
                newStop.setTrackingNumber(stopDto.getTrackingNumber());
                newStop.setRecipientName(stopDto.getRecipientName());
                newStop.setRecipientPhone(stopDto.getRecipientPhone());
                newStop.setRecipientAddress(stopDto.getRecipientAddress());
                newStop.setRecipientLatitude(stopDto.getLatitude());
                newStop.setRecipientLongitude(stopDto.getLongitude());
                newStop.setCodAmount(stopDto.getCodAmount());
                newStop.setPriority(stopDto.getPriority());
                newStop.setEtaTime(stopDto.getEtaTime());
                newStop.setEtaMinutesFromStart(stopDto.getEtaMinutesFromStart());
                Double legDist = stopDto.getLegDistanceKm() != null ? stopDto.getLegDistanceKm() : 0.0;
                newStop.setLegDistanceKm(BigDecimal.valueOf(legDist));
                newStop.setLegDurationMinutes(stopDto.getLegDurationMinutes());
                newStop.setServiceTimeMinutes(stopDto.getServiceTimeMinutes());
                newStop.setStopStatus(RouteStopStatus.PENDING);
                newStop.setIsInserted(oldStop.getIsInserted());
                newStop.setInsertedReason(oldStop.getInsertedReason());
                newStop.setOriginalSequence(oldStop.getOriginalSequence());
                newStops.add(newStop);
            }

            int savedDeliveryPickup = newStops.size();

            // RETURN_TO_OFFICE stop
            int savedReturnStops = 0;
            AiRouteStopOutputDto returnDto = newAiRoute.getReturnToOfficeStop();
            if (returnDto != null) {
                AiRoutePlanStop returnStop = new AiRoutePlanStop();
                returnStop.setRoute(null);
                returnStop.setOrder(null);
                returnStop.setStopType(RouteStopType.RETURN_TO_OFFICE);
                returnStop.setStopSequence(seq++);
                returnStop.setRecipientName(office.getName());
                returnStop.setRecipientAddress(office.getName());
                returnStop.setRecipientLatitude(office.getLatitude().doubleValue());
                returnStop.setRecipientLongitude(office.getLongitude().doubleValue());
                returnStop.setCodAmount(0);
                returnStop.setEtaTime(returnDto.getEtaTime());
                returnStop.setEtaMinutesFromStart(returnDto.getEtaMinutesFromStart());
                returnStop.setStopStatus(RouteStopStatus.PENDING);
                returnStop.setIsInserted(false);
                newStops.add(returnStop);
                savedReturnStops = 1;
            }

            log.debug("reOptimizeRoute: newStops total={} deliveryPickup={} returnOffice={}",
                    newStops.size(), savedDeliveryPickup, savedReturnStops);

            // Validate saved delivery/pickup stops before deactivating old route
            if (savedDeliveryPickup == 0) {
                log.warn("[BE_ROLLBACK] No DELIVERY/PICKUP stops saved. AI returned {} stops. Keeping old route active.",
                        newAiRoute.getStops().size());
                throw new AppException(CommonErrorCode.BAD_REQUEST,
                        "Không có điểm giao hàng nào được lưu. AI trả về " + newAiRoute.getStops().size()
                                + " điểm dừng không hợp lệ. Giữ nguyên tuyến cũ.");
            }

            // Create new route and deactivate old route
            AiRoutePlanRoute newRoute = new AiRoutePlanRoute();
            newRoute.setPlan(currentRoute.getPlan());
            newRoute.setShipperUserId(currentRoute.getShipperUserId());
            newRoute.setShipperEmployeeId(currentRoute.getShipperEmployeeId());
            newRoute.setShipperName(currentRoute.getShipperName());
            newRoute.setRouteSequence(currentRoute.getRouteSequence());
            newRoute.setEstimatedDistanceKm(BigDecimal.valueOf(
                    newAiRoute.getEstimatedDistanceKm() != null ? newAiRoute.getEstimatedDistanceKm() : 0.0));
            newRoute.setFuelCost(BigDecimal.valueOf(
                    newAiRoute.getFuelCost() != null ? newAiRoute.getFuelCost() : 0.0));
            newRoute.setTotalCod(newAiRoute.getTotalCod() != null ? newAiRoute.getTotalCod() : 0L);
            newRoute.setEncodedPolyline(newAiRoute.getEncodedPolyline());
            // route.startTime chỉ lưu HH:mm (cột VARCHAR(10)), KHÔNG lưu LocalDateTime đầy đủ
            String persistedStartTime = currentRoute.getStartTime();
            if (persistedStartTime == null || persistedStartTime.isBlank()) {
                persistedStartTime = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            }
            newRoute.setStartTime(persistedStartTime);
            newRoute.setReoptimizedAt(LocalDateTime.now());
            newRoute.setCurrentLatitude(request.getCurrentLatitude());
            newRoute.setCurrentLongitude(request.getCurrentLongitude());
            newRoute.setRouteMode(RouteMode.CLOSED_LOOP);
            newRoute.setReturnToOffice(true);
            newRoute.setRouteVersion((currentRoute.getRouteVersion() != null ? currentRoute.getRouteVersion() : 1) + 1);
            newRoute.setParentRouteId(currentRoute.getId());
            newRoute.setIsActive(true);
            newRoute.setReoptimizeReason(request.getReason() != null ? request.getReason() : "MANUAL");

            // Tính ETA theo leg thật: currentPos -> stop1 -> stop2 -> ... -> office
            // Không chia đều - dùng khoảng cách Haversine / tốc độ thực tế
            LocalDateTime etaBaseTime = LocalDateTime.now();
            Double startLat = request.getCurrentLatitude();
            Double startLng = request.getCurrentLongitude();
            Double officeLat = office.getLatitude() != null ? office.getLatitude().doubleValue() : null;
            Double officeLng = office.getLongitude() != null ? office.getLongitude().doubleValue() : null;
            Double speedKmh = resolveAverageSpeedKmh(currentRoute);
            double computedDuration = computeLegBasedEtas(
                    newStops, etaBaseTime, startLat, startLng,
                    officeLat, officeLng, speedKmh);

            // Tổng estimatedDuration của route = sum(leg + service) từ computeLegBasedEtas
            // Re-optimize: ETA đã tự tính theo leg thật → duration phải đồng bộ cùng nguồn.
            // KHÔNG ưu tiên AI estimatedDuration vì có thể lệch so với leg thật.
            newRoute.setEstimatedDurationMinutes(BigDecimal.valueOf(computedDuration));

            // Attach stops to route
            for (AiRoutePlanStop s : newStops) {
                s.setRoute(newRoute);
            }
            newRoute.setStops(newStops);
            newRoute.setStopCount(savedDeliveryPickup);

            // Deactivate old route AFTER new route is fully validated
            currentRoute.setIsActive(false);
            currentRoute.setReoptimizedAt(LocalDateTime.now());
            currentRoute.setReoptimizeReason(request.getReason() != null ? request.getReason() : "MANUAL");

            // Save new route first (old route stays active until new route is persisted)
            newRoute = aiRoutePlanRouteRepository.save(newRoute);

            // Then save old route as inactive
            aiRoutePlanRouteRepository.save(currentRoute);

            Long saveEndMs = System.currentTimeMillis();
            log.info("reOptimizeRoute: Route optimized - oldRouteId={} newRouteId={} version={} deliveryStops={} totalMs={}",
                    currentRoute.getId(), newRoute.getId(), newRoute.getRouteVersion(),
                    savedDeliveryPickup, saveEndMs - startMs);

            Map<String, Object> result = buildAiDeliveryRouteResponseData(employee, newRoute);

            long elapsed = System.currentTimeMillis() - startMs;
            log.info("reOptimizeRoute: completed - newRouteId={} totalMs={} resultKeys={}",
                    newRoute.getId(), elapsed, result.keySet());
            log.info("[REOPTIMIZE_RESPONSE] routeId={} estimatedDuration={} totalStops={} totalDistance={}",
                    newRoute.getId(), newRoute.getEstimatedDurationMinutes(),
                    result.get("routeInfo") != null ? ((Map<?, ?>) result.get("routeInfo")).get("totalStops") : null,
                    newRoute.getEstimatedDistanceKm());

            return result;

        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "Lỗi khi tái tối ưu tuyến: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }
    }

    /**
     * Chuẩn hóa stopSequence cho route để:
     * - DELIVERY / PICKUP đứng trước (giữ thứ tự theo stopSequence hiện tại)
     * - RETURN_TO_OFFICE luôn ở cuối
     * - Đánh lại sequence từ 1
     * - SaveAll tất cả stops
     * - Cập nhật route.stopCount
     *
     * QUAN TRỌNG: luôn query trực tiếp từ DB bằng repository,
     * KHÔNG phụ thuộc vào route.getStops() để tránh EntityManager cache.
     */
    private void normalizeRouteStopSequences(Long routeId) {
        List<AiRoutePlanStop> allStops = aiRoutePlanStopRepository.findByRouteIdOrderByStopSequenceAsc(routeId);
        if (allStops == null || allStops.isEmpty()) {
            return;
        }

        // Tách rõ operational (DELIVERY + PICKUP) vs RETURN_TO_OFFICE
        // Sort operational theo stopSequence hiện tại để giữ thứ tự DELIVERY
        List<AiRoutePlanStop> operationalStops = allStops.stream()
                .filter(s -> s.getStopType() != RouteStopType.RETURN_TO_OFFICE)
                .sorted(Comparator
                        .comparing((AiRoutePlanStop s) -> s.getStopSequence() != null ? s.getStopSequence() : 0)
                        .thenComparing(s -> s.getId() != null ? s.getId() : 0L))
                .toList();

        List<AiRoutePlanStop> returnStops = allStops.stream()
                .filter(s -> s.getStopType() == RouteStopType.RETURN_TO_OFFICE)
                .toList();

        // Ghép: operational (đã sort) trước, RETURN_TO_OFFICE cuối
        List<AiRoutePlanStop> ordered = new java.util.ArrayList<>(operationalStops);
        ordered.addAll(returnStops);

        // Đánh lại sequence: operational 1..n, RETURN_TO_OFFICE n+1..
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setStopSequence(i + 1);
        }

        aiRoutePlanStopRepository.saveAll(ordered);

        // Cập nhật route.stopCount và reoptimizedAt
        AiRoutePlanRoute route = aiRoutePlanRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("Route not found: " + routeId));
        route.setStopCount(operationalStops.size());
        route.setReoptimizedAt(java.time.LocalDateTime.now());
        aiRoutePlanRouteRepository.save(route);
    }

    /**
     * Thêm đơn hàng vào AI route đang hoạt động của shipper.
     * Nếu chưa có AI route thì bỏ qua (đơn vẫn được gán vào shipper).
     */
    private void assignOrderToActiveAiRoute(Employee employee, Order order) {
        List<AiRoutePlanRoute> routes = aiRoutePlanRouteRepository.findConfirmedRoutesForShipper(
                employee.getId(), AiRoutePlanStatus.CONFIRMED);

        if (routes.isEmpty()) {
            log.info("assignOrderToActiveAiRoute: no active AI route for shipperId={}, orderId={} skipped",
                    employee.getId(), order.getId());
            return;
        }

        AiRoutePlanRoute targetRoute = routes.get(0);
        Long routeId = targetRoute.getId();

        if (Boolean.TRUE.equals(aiRoutePlanStopRepository.existsByRouteIdAndOrderId(routeId, order.getId()))) {
            log.info("assignOrderToActiveAiRoute: orderId={} already exists in routeId={}, skip",
                    order.getId(), routeId);
            return;
        }

        boolean isPickup = order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER
                || order.getStatus() == OrderStatus.READY_FOR_PICKUP
                || order.getStatus() == OrderStatus.PICKING_UP
                || order.getStatus() == OrderStatus.PICKED_UP;
        RouteStopType stopType = isPickup ? RouteStopType.PICKUP : RouteStopType.DELIVERY;
        String insertedReason = isPickup ? "PICKUP_REQUEST" : "SHIPPER_CLAIM";

        int tempSeq = aiRoutePlanStopRepository.findMaxStopSequenceByRouteId(routeId).orElse(0) + 1;

        AiRoutePlanStop newStop = new AiRoutePlanStop();
        newStop.setRoute(targetRoute);
        newStop.setOrder(order);
        newStop.setStopType(stopType);
        newStop.setStopSequence(tempSeq);
        newStop.setOriginalSequence(tempSeq);
        newStop.setTrackingNumber(order.getTrackingNumber());
        newStop.setRecipientName(order.getSenderName());
        newStop.setRecipientPhone(order.getSenderPhone());
        newStop.setRecipientAddress(order.getSenderFullAddress());
        newStop.setRecipientLatitude(order.getSenderLatitude() != null ? order.getSenderLatitude() : 0.0);
        newStop.setRecipientLongitude(order.getSenderLongitude() != null ? order.getSenderLongitude() : 0.0);
        newStop.setCodAmount(order.getCod() != null ? order.getCod() : 0);
        newStop.setPriority("NORMAL");
        newStop.setStopStatus(RouteStopStatus.PENDING);
        newStop.setIsInserted(true);
        newStop.setInsertedReason(insertedReason);
        newStop.setServiceTimeMinutes(5);
        aiRoutePlanStopRepository.saveAndFlush(newStop);

        // Query thẳng DB để verify stop đã được persist
        List<AiRoutePlanStop> verifyStops = aiRoutePlanStopRepository.findByRouteIdOrderByStopSequenceAsc(routeId);

        // Gọi normalize với routeId — query TRỰC TIẾP từ DB, KHÔNG dùng refreshedRoute entity
        normalizeRouteStopSequences(routeId);

        log.info("assignOrderToActiveAiRoute: orderId={} shipperId={} routeId={} stopType={} "
                        + "stopId={} insertedReason={}",
                order.getId(), employee.getId(), routeId, stopType, newStop.getId(), insertedReason);
    }

    // ===================== PICKUP INSERTION =====================
    @Transactional
    public Map<String, Object> assignPickupToShipperRoute(PickupInsertionRequest request) {
        Employee employee = getCurrentEmployee();

        Order order = orderRepository.findById(request.getPickupOrderId())
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() != null && !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(AiRouteErrorCode.AI_ORDER_ASSIGNED_TO_OTHER, "Đơn đã được gán cho shipper khác");
        }

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP && order.getStatus() != OrderStatus.URGENT_PICKUP && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS, "Trạng thái đơn không phù hợp để gán pickup");
        }

        // Assign order to shipper
        order.setEmployee(employee);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.READY_FOR_PICKUP);
        }
        orderRepository.save(order);

        AiRoutePlanRoute targetRoute;
        if (request.getTargetRouteId() != null) {
            targetRoute = aiRoutePlanRouteRepository.findByIdWithDetails(request.getTargetRouteId().longValue())
                    .orElseThrow(() -> new AppException(AiRouteErrorCode.AI_PLAN_NOT_FOUND));
            if (!Objects.equals(targetRoute.getShipperEmployeeId(), employee.getId())) {
                throw new AppException(CommonErrorCode.FORBIDDEN, "Tuyến không thuộc về bạn");
            }
        } else {
            List<AiRoutePlanRoute> routes = aiRoutePlanRouteRepository.findConfirmedRoutesForShipper(
                    employee.getId(), AiRoutePlanStatus.CONFIRMED);
            if (routes.isEmpty()) {
                // No route exists - just return success, pickup will be standalone
                log.info("assignPickupToShipperRoute: no active route, pickup orderId={} assigned to shipperId={}",
                        request.getPickupOrderId(), employee.getId());
                return Map.of("message", "Đơn pickup đã được gán cho shipper", "orderId", order.getId());
            }
            targetRoute = routes.get(0);
        }

        // Add PICKUP stop to existing route
        AiRoutePlanStop pickupStop = new AiRoutePlanStop();
        pickupStop.setRoute(targetRoute);
        pickupStop.setOrder(order);
        pickupStop.setStopType(RouteStopType.PICKUP);
        pickupStop.setStopSequence(targetRoute.getStops().size() + 1);
        pickupStop.setOriginalSequence(targetRoute.getStops().size() + 1);
        pickupStop.setTrackingNumber(order.getTrackingNumber());
        pickupStop.setRecipientName(order.getSenderName());
        pickupStop.setRecipientPhone(order.getSenderPhone());
        pickupStop.setRecipientAddress(order.getSenderFullAddress());
        pickupStop.setRecipientLatitude(order.getSenderLatitude() != null ? order.getSenderLatitude() : 0.0);
        pickupStop.setRecipientLongitude(order.getSenderLongitude() != null ? order.getSenderLongitude() : 0.0);
        pickupStop.setCodAmount(0);
        pickupStop.setPriority("NORMAL");
        pickupStop.setStopStatus(RouteStopStatus.PENDING);
        pickupStop.setIsInserted(true);
        pickupStop.setInsertedReason("PICKUP_REQUEST");
        pickupStop.setServiceTimeMinutes(5);

        // Save & flush pickup stop TRƯỚC normalize
        Long routeId = targetRoute.getId();
        aiRoutePlanStopRepository.saveAndFlush(pickupStop);

        // Gọi normalize với routeId — query TRỰC TIẾP từ DB, KHÔNG dùng refreshedRoute entity
        normalizeRouteStopSequences(routeId);

        // Notify shipper
        try {
            notificationService.create(
                    "Đơn lấy hàng mới",
                    "Có đơn pickup mới: " + order.getTrackingNumber() + ". Bấm 'Tối ưu lại tuyến' để cập nhật.",
                    "pickup_inserted",
                    employee.getUser().getId(),
                    null,
                    "order",
                    order.getTrackingNumber()
            );
        } catch (Exception e) {
            log.warn("Failed to send pickup notification: {}", e.getMessage());
        }

        log.info("assignPickupToShipperRoute: orderId={} shipperId={} routeId={}",
                request.getPickupOrderId(), employee.getId(), targetRoute.getId());

        return Map.of(
                "message", "Đơn pickup đã được thêm vào tuyến",
                "orderId", order.getId(),
                "routeId", targetRoute.getId(),
                "stopId", pickupStop.getId(),
                "requiresReoptimize", true
        );
    }

    // ===================== PHASE 3C: PICKUP INSERT INTO SHIPMENT =====================

    /**
     * Insert pickup order trực tiếp vào Shipment (ShipmentOrder là source of truth).
     *
     * Flow cũ {@link #assignPickupToShipperRoute} chỉ thêm vào AiRoutePlanStop - gây divergence
     * giữa Shipment (execution) và AI route (advisor). Method này đảm bảo pickup xuất hiện
     * trong Shipment execution path ngay lập tức.
     *
     * Old method giữ lại cho AI-source fallback (routeInfo.source === "AI").
     *
     * Propagation: REQUIRES_NEW — chạy trong transaction riêng để:
     *   1. Nếu throw AppException, chỉ rollback inner transaction.
     *   2. Outer transaction (vd: ShipmentDeliveryService.acceptPickupRequest) vẫn commit được,
     *      giữ order.status = PICKING_UP và trả 200 cho shipper thay vì 500.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> insertPickupIntoShipment(Integer shipmentId,
                                                         com.logistics.request.shipper.InsertPickupShipmentRequest request) {
        Employee employee = getCurrentEmployee();

        // 1. Validate shipment
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AppException(
                        com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_FOUND));

        // Auth: phải là shipper được gán
        if (shipment.getEmployee() == null
                || !Objects.equals(shipment.getEmployee().getId(), employee.getId())) {
            throw new AppException(
                    com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_ASSIGNED,
                    "Bạn không phải shipper được gán cho chuyến này");
        }

        // Loại chuyến phải là DELIVERY
        if (shipment.getType() != com.logistics.enums.ShipmentType.DELIVERY) {
            throw new AppException(
                    com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_DELIVERY,
                    "Chỉ chấp nhận chuyến DELIVERY cho thao tác này");
        }

        // Status phải PENDING hoặc IN_TRANSIT
        ShipmentStatus status = shipment.getStatus();
        if (status == ShipmentStatus.COMPLETED || status == ShipmentStatus.CANCELLED) {
            throw new AppException(
                    com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_INVALID_STATUS,
                    "Chuyến đã " + status.name() + ", không thể thêm đơn pickup");
        }

        // 2. Validate pickup order
        Integer pickupOrderId = request.getPickupOrderId();
        if (pickupOrderId == null) {
            throw new AppException(CommonErrorCode.BAD_REQUEST, "pickupOrderId is required");
        }

        Order order = orderRepository.findById(pickupOrderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        // Phải là PICKUP_BY_COURIER
        if (order.getPickupType() != com.logistics.enums.OrderPickupType.PICKUP_BY_COURIER) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS,
                    "Đơn này không phải đơn pickup (PICKUP_BY_COURIER)");
        }

        // Status order phải hợp lệ
        OrderStatus os = order.getStatus();
        boolean validForInsert = (os == OrderStatus.CONFIRMED
                || os == OrderStatus.READY_FOR_PICKUP
                || os == OrderStatus.PICKUP_RETRY
                || os == OrderStatus.URGENT_PICKUP
                || os == OrderStatus.PICKING_UP);
        if (!validForInsert) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS,
                    "Trạng thái đơn không hợp lệ để pickup: " + (os != null ? os.name() : "null")
                            + ". Yêu cầu CONFIRMED / READY_FOR_PICKUP / PICKUP_RETRY / URGENT_PICKUP / PICKING_UP");
        }

        // Không thuộc shipment PENDING/IN_TRANSIT khác
        List<Shipment> activeShipments = shipmentOrderRepository.findActiveShipmentsForOrder(pickupOrderId);
        if (activeShipments != null && !activeShipments.isEmpty()) {
            boolean inOtherActiveShipment = activeShipments.stream()
                    .anyMatch(s -> !Objects.equals(s.getId(), shipmentId));
            if (inOtherActiveShipment) {
                throw new AppException(
                        com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_CANNOT_ADD_ORDERS,
                        "Đơn " + order.getTrackingNumber() + " đã thuộc chuyến DELIVERY khác đang hoạt động");
            }
            // Đã thuộc shipment này rồi → idempotent skip
            if (activeShipments.stream().anyMatch(s -> Objects.equals(s.getId(), shipmentId))) {
                log.info("insertPickupIntoShipment: orderId={} already in shipmentId={}, skip (idempotent)",
                        pickupOrderId, shipmentId);
                Integer existingSeq = shipmentOrderRepository.findMaxStopSequenceByShipmentId(shipmentId);
                return Map.of(
                        "message", "Đơn pickup đã thuộc chuyến này",
                        "shipmentId", shipmentId,
                        "orderId", pickupOrderId,
                        "stopSequence", existingSeq != null ? existingSeq : 0,
                        "requiresReoptimize", false
                );
            }
        }

        // 3. Set order.employee + update order status theo shipment status
        order.setEmployee(employee);
        if (status == ShipmentStatus.IN_TRANSIT) {
            // Shipment đã chạy → đơn pickup vào luôn trạng thái "đang lấy"
            if (os == OrderStatus.CONFIRMED
                    || os == OrderStatus.PICKUP_RETRY
                    || os == OrderStatus.READY_FOR_PICKUP
                    || os == OrderStatus.URGENT_PICKUP
                    || os == OrderStatus.PICKING_UP) {
                // Nếu đã PICKING_UP thì giữ nguyên PICKING_UP (idempotent),
                // các status khác chuyển sang PICKING_UP vì shipper đã cam kết đi lấy ngay.
                order.setStatus(OrderStatus.PICKING_UP);
                // KHÔNG cộng vehicle load ở đây — chỉ cộng khi shipper lấy hàng thật (PICKED_UP).
            }
        }
        // Nếu shipment PENDING: giữ nguyên status (CONFIRMED / READY_FOR_PICKUP / PICKUP_RETRY / URGENT_PICKUP / PICKING_UP)
        orderRepository.save(order);

        // 4. Tính stopSequence = max + 1
        Integer maxSeq = shipmentOrderRepository.findMaxStopSequenceByShipmentId(shipmentId);
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;

        // 5. Tạo ShipmentOrder (source of truth)
        com.logistics.entity.id.ShipmentOrderId soId = new com.logistics.entity.id.ShipmentOrderId(shipmentId, pickupOrderId);
        ShipmentOrder so = new ShipmentOrder();
        so.setId(soId);
        so.setShipment(shipment);
        so.setOrder(order);
        so.setStopType(com.logistics.enums.RouteStopType.PICKUP);
        so.setStopSequence(nextSeq);
        // eta fields để null (Phase 3B re-optimize sẽ fill)
        so.setEtaTime(null);
        so.setEtaMinutesFromStart(null);
        so.setLegDistanceKm(null);
        so.setLegDurationMinutes(null);
        shipmentOrderRepository.save(so);

        log.info("[PICKUP_INSERT_SHIPMENT] shipmentId={} orderId={} stopSequence={} orderStatus={} shipmentStatus={}",
                shipmentId, pickupOrderId, nextSeq, order.getStatus(), status);

        // 6. Advisory mirror sang AiRoutePlanStop (nếu có AI route active cho shipment này)
        try {
            List<AiRoutePlanRoute> aiRoutes = aiRoutePlanRouteRepository.findActiveByShipmentId(shipmentId);
            if (!aiRoutes.isEmpty()) {
                AiRoutePlanRoute aiRoute = aiRoutes.get(0);
                AiRoutePlanStop pickupStop = new AiRoutePlanStop();
                pickupStop.setRoute(aiRoute);
                pickupStop.setOrder(order);
                pickupStop.setStopType(com.logistics.enums.RouteStopType.PICKUP);
                pickupStop.setStopSequence(nextSeq);
                pickupStop.setTrackingNumber(order.getTrackingNumber());
                pickupStop.setRecipientName(order.getSenderName());
                pickupStop.setRecipientPhone(order.getSenderPhone());
                pickupStop.setRecipientAddress(order.getSenderFullAddress());
                pickupStop.setRecipientLatitude(
                        order.getSenderLatitude() != null ? order.getSenderLatitude() : 0.0);
                pickupStop.setRecipientLongitude(
                        order.getSenderLongitude() != null ? order.getSenderLongitude() : 0.0);
                pickupStop.setCodAmount(0);
                pickupStop.setPriority("NORMAL");
                pickupStop.setStopStatus(RouteStopStatus.PENDING);
                pickupStop.setIsInserted(true);
                pickupStop.setInsertedReason("PICKUP_REQUEST");
                pickupStop.setServiceTimeMinutes(5);
                aiRoutePlanStopRepository.saveAndFlush(pickupStop);

                log.info("[PICKUP_INSERT_AI_MIRROR] aiRouteId={} stopId={}",
                        aiRoute.getId(), pickupStop.getId());
            }
        } catch (Exception mirrorEx) {
            // Mirror failure KHÔNG block pickup insert - ShipmentOrder đã là source of truth
            log.warn("[PICKUP_INSERT_AI_MIRROR_FAILED] shipmentId={} orderId={} reason={}",
                    shipmentId, pickupOrderId, mirrorEx.getMessage());
        }

        // 7. Lưu OrderHistory (audit)
        try {
            saveHistory(order, shipment,
                    com.logistics.enums.OrderHistoryActionType.READY_FOR_PICKUP,
                    "Pickup order được thêm vào chuyến DELIVERY shipmentId=" + shipmentId
                            + " stopSequence=" + nextSeq);
        } catch (Exception e) {
            log.warn("Failed to save OrderHistory for pickup insert: {}", e.getMessage());
        }

        // 8. Notify shipper (suggest re-optimize)
        try {
            notificationService.create(
                    "Đơn pickup mới",
                    "Đã thêm đơn pickup " + order.getTrackingNumber()
                            + " vào chuyến " + shipment.getCode()
                            + ". Bấm 'Tối ưu lại tuyến' để cập nhật ETA.",
                    "pickup_inserted",
                    employee.getUser().getId(),
                    null,
                    "shipment",
                    shipment.getCode()
            );
        } catch (Exception e) {
            log.warn("Failed to send pickup notification: {}", e.getMessage());
        }

        return Map.of(
                "message", "Đơn pickup đã được thêm vào chuyến DELIVERY",
                "shipmentId", shipmentId,
                "orderId", pickupOrderId,
                "stopSequence", nextSeq,
                "requiresReoptimize", true
        );
    }

    private boolean hasValidLatLng(Double lat, Double lng) {
        return lat != null && lng != null
                && !lat.equals(0.0) && !lng.equals(0.0)
                && !Double.isNaN(lat) && !Double.isNaN(lng)
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180;
    }
}


