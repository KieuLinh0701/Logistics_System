package com.logistics.service.shipper;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.*;
import com.logistics.repository.*;
import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.request.shipper.CreateIncidentReportRequest;
import com.logistics.request.shipper.DeliverOriginRequest;
import com.logistics.request.shipper.PickedUpRequest;
import com.logistics.request.shipper.PickupInsertionRequest;
import com.logistics.request.shipper.ShipperReOptimizeRequest;
import com.logistics.request.shipper.UpdateDeliveryStatusRequest;
import com.logistics.dto.ai.AiLocationDto;
import com.logistics.dto.ai.AiOfficeLocationDto;
import com.logistics.dto.ai.AiOrderInputDto;
import com.logistics.dto.ai.AiRouteOptimizationRequestDto;
import com.logistics.dto.ai.AiRouteOptimizationResponseDto;
import com.logistics.dto.ai.AiRouteStopInputDto;
import com.logistics.dto.ai.AiRouteStopOutputDto;
import com.logistics.dto.ai.AiShipperInputDto;
import com.logistics.dto.ai.AiShipperRouteOutputDto;
import com.logistics.service.ai.AiServiceClient;
import com.logistics.response.NotificationResponse;
import com.logistics.response.Pagination;
import com.logistics.service.assignment.AutoAssignService;
import com.logistics.service.common.ConfigService;
import com.logistics.service.common.NotificationService;
import com.logistics.utils.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
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
                    OrderStatus.PICKUP_RETRY,
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
                OrderStatus.PICKUP_RETRY
            ));
            assignedPreds.add(cb.equal(root.get("pickupType"), OrderPickupType.PICKUP_BY_COURIER));

            // Thu hẹp theo bưu cục của shipper: chỉ theo fromOffice nếu có
            try {
                Predicate fromOfficeMatch = cb.equal(root.get("fromOffice").get("id"), officeId);
                availablePreds.add(fromOfficeMatch);
                assignedPreds.add(fromOfficeMatch);
            } catch (Exception e) {
                throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR, e);
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
    public void claimOrderRequest(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() != null) {
            throw new AppException(OrderErrorCode.ORDER_ALREADY_CLAIMED);
        }

        if (order.getPickupType() != null && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            if (order.getFromOffice() == null || !Objects.equals(order.getFromOffice().getId(), officeId)) {
                throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
            }
        } else {
            if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
                throw new AppException(OrderErrorCode.ORDER_OFFICE_MISMATCH);
            }
        }

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.AT_DEST_OFFICE && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_CLAIM_STATUS);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.PICKING_UP);
        order.setEmployee(employee);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.PICKING_UP, "Shipper nhận yêu cầu lấy hàng (bắt đầu lấy)");

        if (order.getUser() != null) {
            notificationService.create(
                    "Shipper đang đến lấy hàng",
                    String.format("Shipper đang trên đường đến lấy đơn %s.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }

        try {
            assignOrderToActiveAiRoute(employee, order);
        } catch (Exception e) {
            log.warn("claimOrderRequest: failed to assign order {} to AI route: {}", id, e.getMessage());
        }
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

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.AT_DEST_OFFICE && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_CLAIM_STATUS);
        }

        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order.setEmployee(employee);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.READY_FOR_PICKUP, "Shipper nhận đơn (sẵn sàng lấy)");

        // Thêm đơn vào AI route hiện tại của shipper
        try {
            assignOrderToActiveAiRoute(employee, order);
        } catch (Exception e) {
            log.warn("claimOrder: failed to assign order {} to AI route: {}", id, e.getMessage());
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
            String rawStatus = request.getStatus().trim().toUpperCase();
            if ("DELIVERY_FAILED".equals(rawStatus) || "FAILED".equals(rawStatus)) {
                rawStatus = "FAILED_DELIVERY";
            }
            newStatus = OrderStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_DELIVERY_STATUS);
        }

        User shipperUser = employee.getUser();

        if (request.getStatus() != null && request.getStatus().equalsIgnoreCase("DELIVERY_FAILED_FINAL")) {
            handleDeliveryFailedFinal(order, employee, shipperUser, request);
            return;
        }

        if (newStatus == OrderStatus.DELIVERED) {
            handleDeliverySuccess(order, employee, shipperUser, request);
            return;
        }

        if (newStatus == OrderStatus.DELIVERY_RETRY) {
            handleDeliveryFailure(order, employee, shipperUser, request);
            return;
        }

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
    }

    @Transactional
    public void markPickedUp(Integer id, PickedUpRequest request) {
        Employee employee = getCurrentEmployee();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        // Chỉ shipper đã nhận (employee) mới có thể đánh dấu đã lấy
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        // Kiểm tra trạng thái phù hợp (đang lấy hàng)
        if (order.getStatus() != OrderStatus.PICKING_UP
                && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
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

        if (order.getUser() != null) {
            notificationService.create(
                    "Lấy hàng chưa thành công",
                    String.format("Lấy hàng đơn %s chưa thành công. Hệ thống sẽ sắp xếp lấy lại.", order.getTrackingNumber()),
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

        // Chỉ hợp lệ nếu đã pick up
        if (order.getStatus() != OrderStatus.PICKED_UP && order.getStatus() != OrderStatus.PICKING_UP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
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
        order.setStatus(OrderStatus.AT_DEST_OFFICE);
        order.setEmployee(null);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.AT_DEST_OFFICE, "Shipper đã nộp hàng giao thất bại về bưu cục đích");
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
    @Transactional(readOnly = true)
    public Map<String, Object> getDeliveryRoute() {
        Employee employee = getCurrentEmployee();
        log.info("[getDeliveryRoute] shipperEmployeeId={} officeId={}",
                employee.getId(), employee.getOffice() != null ? employee.getOffice().getId() : null);

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

    public void startRoute(Integer routeId) {
    }

    // ===================== SHIPPER RE-OPTIMIZE =====================
    @Transactional
    public Map<String, Object> reOptimizeRoute(ShipperReOptimizeRequest request) {
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
            newRoute.setEstimatedDurationMinutes(BigDecimal.valueOf(
                    newAiRoute.getEstimatedDurationMinutes() != null ? newAiRoute.getEstimatedDurationMinutes() : 0.0));
            newRoute.setFuelCost(BigDecimal.valueOf(
                    newAiRoute.getFuelCost() != null ? newAiRoute.getFuelCost() : 0.0));
            newRoute.setTotalCod(newAiRoute.getTotalCod() != null ? newAiRoute.getTotalCod() : 0L);
            newRoute.setEncodedPolyline(newAiRoute.getEncodedPolyline());
            newRoute.setStartTime(newAiRoute.getStartTime());
            newRoute.setCurrentLatitude(request.getCurrentLatitude());
            newRoute.setCurrentLongitude(request.getCurrentLongitude());
            newRoute.setRouteMode(RouteMode.CLOSED_LOOP);
            newRoute.setReturnToOffice(true);
            newRoute.setRouteVersion((currentRoute.getRouteVersion() != null ? currentRoute.getRouteVersion() : 1) + 1);
            newRoute.setParentRouteId(currentRoute.getId());
            newRoute.setIsActive(true);
            newRoute.setReoptimizeReason(request.getReason() != null ? request.getReason() : "MANUAL");

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

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP && order.getStatus() != OrderStatus.CONFIRMED) {
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

    private boolean hasValidLatLng(Double lat, Double lng) {
        return lat != null && lng != null
                && !lat.equals(0.0) && !lng.equals(0.0)
                && !Double.isNaN(lat) && !Double.isNaN(lng)
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180;
    }
}


