package com.logistics.service.shipper;

import com.logistics.entity.Address;
import com.logistics.entity.Employee;
import com.logistics.entity.IncidentReport;
import com.logistics.entity.Order;
import com.logistics.entity.PaymentSubmission;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.User;
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
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.IncidentReportRepository;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.repository.OfficeRepository;
import com.logistics.request.shipper.CreateIncidentReportRequest;
import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.request.shipper.UpdateDeliveryStatusRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.utils.SecurityUtils;
import com.logistics.service.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderShipperService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private IncidentReportRepository incidentReportRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private PaymentSubmissionRepository paymentSubmissionRepository;

    @Autowired
    private com.logistics.service.assignment.AutoAssignService autoAssignService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OfficeRepository officeRepository;

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

        // COD shipper đã thu (PENDING / IN_BATCH)
        int codCollected = paymentSubmissionRepository
            .findByShipperIdAndStatusIn(employee.getUser().getId(),
                Arrays.asList(PaymentSubmissionStatus.PENDING, PaymentSubmissionStatus.IN_BATCH))
            .stream()
            .mapToInt(ps -> ps.getActualAmount().intValue())
            .sum();

        List<Map<String, Object>> todayOrderSummaries = todayOrders.stream().map(this::mapOrderSummary).toList();

        // Lấy thông báo gần đây cho shipper (5 thông báo mới nhất)
        List<Map<String, Object>> notificationMaps = Collections.emptyList();
        try {
            NotificationSearchRequest nreq = new NotificationSearchRequest(1, 5, null, null);
            ApiResponse<com.logistics.response.NotificationResponse> nres = notificationService.getNotifications(employee.getUser().getId(), nreq);
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
        } catch (Exception ignored) {}

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
                    OrderStatus.DELIVERING,
                    OrderStatus.DELIVERED,
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
            assignedPreds.add(root.get("status").in(OrderStatus.PICKING_UP, OrderStatus.PICKED_UP));
            assignedPreds.add(cb.equal(root.get("pickupType"), OrderPickupType.PICKUP_BY_COURIER));

            // Thu hẹp theo bưu cục của shipper: chỉ theo fromOffice nếu có
            try {
                Predicate fromOfficeMatch = cb.equal(root.get("fromOffice").get("id"), officeId);
                availablePreds.add(fromOfficeMatch);
                assignedPreds.add(fromOfficeMatch);
            } catch (Exception ignored) {
                // nếu không có fromOffice, bỏ qua điều kiện
            }

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

    @Transactional
    public ApiResponse<String> claimOrder(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Nếu đây là đơn loại PICKUP_BY_COURIER (người gửi yêu cầu shipper tới lấy),
        // kiểm tra dựa trên fromOffice của đơn
        if (order.getPickupType() != null && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            if (order.getFromOffice() == null || !Objects.equals(order.getFromOffice().getId(), officeId)) {
                return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
            }
            if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
                return new ApiResponse<>(false, "Chỉ có thể nhận đơn đang ở trạng thái SẴN SÀNG LẤY", null);
            }
        } else {
            // Trường hợp nhận đơn truyền thống: kiểm tra toOffice
            if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
                return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
            }

            if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.AT_DEST_OFFICE && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
                return new ApiResponse<>(false, "Chỉ có thể nhận đơn đã xác nhận, đã đến bưu cục đích hoặc sẵn sàng lấy", null);
            }
        }

        // Gán employee hiện tại khi shipper nhận đơn và chuyển trạng thái sang PICKING_UP
        order.setEmployee(employee);
        order.setStatus(OrderStatus.PICKING_UP);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.PICKING_UP, "Shipper nhận đơn để lấy hàng");
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
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
        }

        User shipperUser = employee.getUser();
        
        // Luồng cơ bản: PICKED_UP -> DELIVERING -> DELIVERED / FAILED_DELIVERY / RETURNED
        order.setStatus(newStatus);

        // Tiền mặt thu được trong chuyến giao/hoàn
        int cashCollected = 0;

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());

            // Chỉ tạo payment submission cho phí ship khi payer là CUSTOMER.
            if (order.getPayer() == OrderPayerType.CUSTOMER) {
                cashCollected += order.getShippingFee() != null ? order.getShippingFee() : 0;
            }

            if (cashCollected > 0) {
                createPaymentSubmission(order, shipperUser, cashCollected,
                        "Đối soát sau khi giao thành công");
                order.setPaymentStatus(OrderPaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
            }

            // Nếu đơn có COD và chưa có bản ghi thu tiền COD, tạo bản ghi PENDING cho COD
            try {
                if (order.getCod() != null && order.getCod() > 0) {
                    List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
                    boolean hasPositive = existing.stream()
                            .anyMatch(ps -> ps.getActualAmount() != null && ps.getActualAmount().compareTo(BigDecimal.ZERO) > 0);
                    if (!hasPositive) {
                        createPaymentSubmission(order, shipperUser, order.getCod(), "Thu COD sau khi giao");
                        try {
                            order.setCodStatus(OrderCodStatus.PENDING);
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Nếu giao thất bại và có báo cáo Khách từ chối, đảm bảo trạng thái COD được đặt NONE
        if (newStatus == OrderStatus.FAILED_DELIVERY) {
            try {
                boolean recipientRefused = incidentReportRepository.findAll().stream()
                        .anyMatch(ir -> ir.getOrder() != null
                                && Objects.equals(ir.getOrder().getId(), order.getId())
                                && ir.getIncidentType() != null
                                && ir.getIncidentType() == IncidentType.RECIPIENT_REFUSED);
                if (recipientRefused) {
                    order.setCodStatus(OrderCodStatus.NONE);
                }
            } catch (Exception ignored) {
            }
        }

        // Khi hoàn trả: chỉ thu phí dịch vụ (không thu COD)
        if (newStatus == OrderStatus.RETURNED) {
            // Quyết định ai chịu phí hoàn dựa trên incident nếu có
            try {
                boolean recipientNotAvailable = incidentReportRepository.findAll().stream()
                        .anyMatch(ir -> ir.getOrder() != null
                                && Objects.equals(ir.getOrder().getId(), order.getId())
                                && ir.getIncidentType() != null
                                && ir.getIncidentType() == IncidentType.RECIPIENT_NOT_AVAILABLE);

                boolean recipientRefused = incidentReportRepository.findAll().stream()
                        .anyMatch(ir -> ir.getOrder() != null
                                && Objects.equals(ir.getOrder().getId(), order.getId())
                                && ir.getIncidentType() != null
                                && ir.getIncidentType() == IncidentType.RECIPIENT_REFUSED);

                if (recipientNotAvailable) {
                    order.setPayer(OrderPayerType.SHOP);
                }

                // Nếu người nhận từ chối (khách từ chối lấy hàng) thì đảm bảo COD không còn được kỳ vọng
                if (recipientRefused) {
                    order.setCodStatus(OrderCodStatus.NONE);
                }
            } catch (Exception ignored) {
            }

            // Kiểm tra đã có thu COD trước đó hay chưa
            int collectedTotal = 0;
            try {
                List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
                collectedTotal = existing.stream().mapToInt(ps -> ps.getActualAmount() != null ? ps.getActualAmount().intValue() : 0).sum();
            } catch (Exception ignored) {
            }

            // Nếu chưa thu COD mà payer trước đó là CUSTOMER, chuyển sang thu từ SHOP (người gửi)
            if (collectedTotal == 0 && order.getPayer() == OrderPayerType.CUSTOMER) {
                order.setPayer(OrderPayerType.SHOP);
            }

            // Nếu có phí giao/hoàn thì thu theo payer hiện tại
            cashCollected += order.getShippingFee() != null ? order.getShippingFee() : 0;

            if (cashCollected > 0) {
                createPaymentSubmission(order, shipperUser, cashCollected,
                        "Đối soát phí hoàn hàng");
                order.setPaymentStatus(OrderPaymentStatus.UNPAID);
            }

            // Nếu trước đó đã có PaymentSubmission thu COD, tạo bản ghi return để không làm mất dấu tiền
            if (collectedTotal > 0) {
                try {
                    createReturnPaymentSubmission(order, shipperUser, BigDecimal.valueOf(collectedTotal), "COD_RETURN: Hoàn tiền do trả hàng");
                } catch (Exception ignored) {
                }
            }
        }

        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        orderRepository.save(order);

        // Gửi thông báo khi cập nhật trạng thái
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
                "order",
                order.getTrackingNumber()
        );

        // Nhắc nộp COD nếu đã giao thành công và có COD
        if (newStatus == OrderStatus.DELIVERED && order.getCod() > 0) {
            notificationService.create(
                    "Nhắc nhở nộp COD",
                    "Đơn hàng " + order.getTrackingNumber() + " có COD " + order.getCod() + "đ cần nộp",
                    "cod_reminder",
                    shipperUser.getId(),
                    null,
                    "order",
                    order.getTrackingNumber()
            );
        }

        // Ghi OrderHistory cho các trạng thái shipper cập nhật
        switch (newStatus) {
            case DELIVERING -> saveHistory(order, OrderHistoryActionType.DELIVERING,
                    "Shipper bắt đầu giao hàng");
            case DELIVERED -> saveHistory(order, OrderHistoryActionType.DELIVERED,
                    "Shipper đã giao hàng thành công");
            case FAILED_DELIVERY -> {
                String note = "Shipper đã trả đơn về bưu cục";
                if (request.getNotes() != null && !request.getNotes().isBlank()) {
                    note += ": " + request.getNotes();
                }
                saveHistory(order, OrderHistoryActionType.FAILED_DELIVERY, note);
            }
            case RETURNED -> {
                String note = "Shipper đã trả đơn về bưu cục";
                if (request.getNotes() != null && !request.getNotes().isBlank()) {
                    note += ": " + request.getNotes();
                }
                saveHistory(order, OrderHistoryActionType.RETURNED, note);
            }
            default -> {
            }
        }

        return new ApiResponse<>(true, "Cập nhật trạng thái giao hàng thành công", null);
    }

    @Transactional
    public ApiResponse<String> markPickedUp(Integer id, com.logistics.request.shipper.PickedUpRequest request) {
        Employee employee = getCurrentEmployee();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Chỉ shipper đã nhận (employee) mới có thể đánh dấu đã lấy
        if (order.getEmployee() == null || order.getEmployee().getId() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            return new ApiResponse<>(false, "Chỉ shipper được gán mới có thể xác nhận đã lấy hàng", null);
        }

        // Kiểm tra trạng thái phù hợp (đang lấy hàng)
        if (order.getStatus() != OrderStatus.PICKING_UP && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            return new ApiResponse<>(false, "Chỉ có thể xác nhận đã lấy khi đơn ở trạng thái đang lấy hoặc sẵn sàng lấy", null);
        }

        order.setStatus(OrderStatus.PICKED_UP);
        try {
            order.setDeliveredAt(java.time.LocalDateTime.now());
        } catch (Exception ignored) {}

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
        saveHistory(order, OrderHistoryActionType.PICKED_UP, "Shipper xác nhận đã lấy hàng");

        // Gửi notification đơn giản
        try {
            notificationService.create("Đã lấy hàng", "Đơn " + order.getTrackingNumber() + " đã được shipper xác nhận lấy", "order_picked_up", employee.getUser().getId(), null, "order", order.getTrackingNumber());
        } catch (Exception ignored) {}

        return new ApiResponse<>(true, "Xác nhận đã lấy hàng thành công", null);
    }

    @Transactional
    public ApiResponse<String> deliverToOrigin(Integer id, com.logistics.request.shipper.DeliverOriginRequest request) {
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

        // Nếu request chỉ định officeId thì dùng office đó, ngược lại dùng office của shipper
        try {
            if (request != null && request.getOfficeId() != null) {
                officeRepository.findById(request.getOfficeId()).ifPresent(order::setToOffice);
            } else {
                order.setToOffice(employee.getOffice());
            }
        } catch (Exception ignored) {}

        order.setStatus(OrderStatus.AT_ORIGIN_OFFICE);
        orderRepository.save(order);

        saveHistory(order, OrderHistoryActionType.IMPORTED, "Shipper nộp hàng tại bưu cục nguồn");

        try { autoAssignService.autoAssignOnArrival(order.getId()); } catch (Exception ignored) {}

        try {
            notificationService.create("Đã đến bưu cục", "Đơn " + order.getTrackingNumber() + " đã được nộp tại bưu cục", "order_at_origin_office", employee.getUser().getId(), null, "order", order.getTrackingNumber());
        } catch (Exception ignored) {}

        return new ApiResponse<>(true, "Đã nộp hàng tại bưu cục", null);
    }

    public ApiResponse<Map<String, Object>> getDeliveryHistory(int page, int limit, String status) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "deliveredAt"));

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));
            predicates.add(root.get("status").in(
                    OrderStatus.DELIVERED,
                    OrderStatus.FAILED_DELIVERY,
                    OrderStatus.RETURNED
            ));

            // Chỉ bao gồm các đơn đã được gán cho nhân viên này
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

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssigned", orderPage.getTotalElements());
        stats.put("inProgress", 0);
        stats.put("delivered", orderPage.getContent().stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count());
        stats.put("failed", orderPage.getContent().stream()
                .filter(o -> o.getStatus() == OrderStatus.FAILED_DELIVERY || o.getStatus() == OrderStatus.RETURNED)
                .count());
        // COD đã thu: tổng các submission PENDING / IN_BATCH của shipper
        int codCollectedHistory = paymentSubmissionRepository
            .findByShipperIdAndStatusIn(employee.getUser().getId(),
                Arrays.asList(PaymentSubmissionStatus.PENDING, PaymentSubmissionStatus.IN_BATCH))
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
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("trackingNumber", order.getTrackingNumber());
        map.put("senderName", order.getSenderName());
        map.put("senderPhone", order.getSenderPhone());
        map.put("senderAddress", buildAddress(order.getSenderAddress()));
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
        map.put("recipientAddress", buildAddress(order.getRecipientAddress()));
        map.put("cod", order.getCod());
        map.put("codAmount", order.getCod()); 
        map.put("shippingFee", order.getShippingFee());
        map.put("status", order.getStatus().name()); 
        map.put("priority", "normal");
        map.put("serviceType", order.getServiceType() != null ? order.getServiceType().getName() : null);
        return map;
    }

    private Map<String, Object> mapOrderDetail(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("trackingNumber", order.getTrackingNumber());
        map.put("senderName", order.getSenderName());
        map.put("senderPhone", order.getSenderPhone());
        map.put("senderAddress", buildAddress(order.getSenderAddress()));
        map.put("recipientName", order.getRecipientName());
        map.put("recipientPhone", order.getRecipientPhone());
        map.put("recipientAddress", buildAddress(order.getRecipientAddress()));
        map.put("weight", order.getWeight());
        map.put("cod", order.getCod());
        map.put("codStatus", order.getCodStatus() != null ? order.getCodStatus().name() : null);
        // Bao gồm các bản ghi PaymentSubmission liên quan đến đơn hàng này
        try {
            List<PaymentSubmission> submissions = paymentSubmissionRepository.findByOrderId(order.getId());
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
        } catch (Exception ignored) {
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
        return map;
    }

    private Map<String, Object> mapIncident(IncidentReport incident) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", incident.getId());
        map.put("orderId", incident.getOrder() != null ? incident.getOrder().getId() : null);
        map.put("trackingNumber", incident.getOrder() != null ? incident.getOrder().getTrackingNumber() : null);
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
    private void createPaymentSubmission(Order order, User shipperUser, int amount, String note) {
        if (amount <= 0) {
            return;
        }

        try {
            // Nếu đã có submission cho order (vì mapping 1-1), cập nhật bản ghi hiện có thay vì tạo mới
            List<PaymentSubmission> existing = paymentSubmissionRepository.findByOrderId(order.getId());
            if (existing != null && !existing.isEmpty()) {
                PaymentSubmission ex = existing.get(0);
                boolean changed = false;

                if (ex.getSystemAmount() == null || ex.getSystemAmount().intValue() == 0) {
                    ex.setSystemAmount(BigDecimal.valueOf(amount));
                    changed = true;
                }
                if (ex.getActualAmount() == null || ex.getActualAmount().intValue() == 0) {
                    ex.setActualAmount(BigDecimal.valueOf(amount));
                    changed = true;
                }
                if (ex.getStatus() == null || ex.getStatus() != PaymentSubmissionStatus.PENDING) {
                    ex.setStatus(PaymentSubmissionStatus.PENDING);
                    changed = true;
                }

                ex.setShipper(shipperUser);
                ex.setNotes(note);
                ex.setPaidAt(LocalDateTime.now());

                if (changed || ex.getCode() == null) {
                    ex = paymentSubmissionRepository.save(ex);
                }

                if (ex.getCode() == null) {
                    String submissionCode = "SUB_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + ex.getId();
                    ex.setCode(submissionCode);
                    paymentSubmissionRepository.save(ex);
                }

                return;
            }
        } catch (Exception ignored) {
        }

        PaymentSubmission submission = new PaymentSubmission();
        submission.setOrder(order);
        submission.setSystemAmount(BigDecimal.valueOf(amount));
        submission.setActualAmount(BigDecimal.valueOf(amount));
        submission.setStatus(PaymentSubmissionStatus.PENDING);
        submission.setShipper(shipperUser);
        submission.setNotes(note);
        submission.setPaidAt(LocalDateTime.now());

        submission = paymentSubmissionRepository.save(submission);

        String submissionCode = "SUB_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + submission.getId();
        submission.setCode(submissionCode);
        paymentSubmissionRepository.save(submission);
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
        submission.setPaidAt(LocalDateTime.now());

        submission = paymentSubmissionRepository.save(submission);
        String submissionCode = "RET_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + submission.getId();
        submission.setCode(submissionCode);
        paymentSubmissionRepository.save(submission);

        // Sau khi hoàn COD, cập nhật trạng thái COD và trạng thái thanh toán của đơn
        try {
            order.setCodStatus(OrderCodStatus.NONE);
            order.setPaymentStatus(OrderPaymentStatus.REFUNDED);
        } catch (Exception ignored) {
        }
    }

    private String buildAddress(Address address) {
        if (address == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (address.getDetail() != null && !address.getDetail().isBlank()) {
            builder.append(address.getDetail());
        }
        if (address.getWardCode() != null) {
            try {
                String wardName = com.logistics.utils.LocationUtils.getWardNameByCode(address.getCityCode(), address.getWardCode());
                if (wardName != null && !wardName.isBlank()) {
                    if (builder.length() > 0) builder.append(", ");
                    builder.append(wardName);
                } else {
                    if (builder.length() > 0) builder.append(", ");
                    builder.append("Phường ").append(address.getWardCode());
                }
            } catch (Exception ignored) {
                if (builder.length() > 0) builder.append(", ");
                builder.append("Phường ").append(address.getWardCode());
            }
        }
        if (address.getCityCode() != null) {
            try {
                String cityName = com.logistics.utils.LocationUtils.getCityNameByCode(address.getCityCode());
                if (cityName != null && !cityName.isBlank()) {
                    if (builder.length() > 0) builder.append(", ");
                    builder.append(cityName);
                } else {
                    if (builder.length() > 0) builder.append(", ");
                    builder.append("TP ").append(address.getCityCode());
                }
            } catch (Exception ignored) {
                if (builder.length() > 0) builder.append(", ");
                builder.append("TP ").append(address.getCityCode());
            }
        }
        return builder.toString();
    }

    // Lộ trình giao hàng
    public ApiResponse<Map<String, Object>> getDeliveryRoute() {
        Employee employee = getCurrentEmployee();
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

        int totalCOD = routeOrders.stream().mapToInt(Order::getCod).sum();

        List<Map<String, Object>> deliveryStops = routeOrders.stream().map(order -> {
            Map<String, Object> stop = new HashMap<>();
            stop.put("id", order.getId());
            stop.put("trackingNumber", order.getTrackingNumber());
            stop.put("recipientName", order.getRecipientName());
            stop.put("recipientPhone", order.getRecipientPhone());
            stop.put("recipientAddress", buildAddress(order.getRecipientAddress()));
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

    public ApiResponse<String> startRoute(Integer routeId) {
        // TODO: Implement logic để đánh dấu route đã bắt đầu
        return new ApiResponse<>(true, "Đã bắt đầu tuyến giao hàng", null);
    }
}


