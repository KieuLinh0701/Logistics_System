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
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.IncidentReportRepository;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionRepository;
import com.logistics.request.shipper.CreateIncidentReportRequest;
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
    private NotificationService notificationService;

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

        // Lấy danh sách đơn trong ngày tại bưu cục của shipper
        Specification<Order> todaySpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("createdByType"), OrderCreatorType.USER));
            predicates.add(cb.between(root.get("createdAt"), startOfDay, endOfDay));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Order> todayOrders = orderRepository.findAll(todaySpec, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Thống kê cơ bản
        int totalAssigned = (int) todayOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERING
                        || o.getStatus() == OrderStatus.DELIVERED
                        || o.getStatus() == OrderStatus.FAILED_DELIVERY
                        || o.getStatus() == OrderStatus.RETURNED)
                .count();

        int inProgress = (int) todayOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERING)
                .count();

        int delivered = (int) todayOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();

        int failed = (int) todayOrders.stream()
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

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssigned", totalAssigned);
        stats.put("inProgress", inProgress);
        stats.put("delivered", delivered);
        stats.put("failed", failed);
        stats.put("codCollected", codCollected);

        data.put("stats", stats);
        data.put("todayOrders", todayOrderSummaries);
        data.put("notifications", Collections.emptyList());

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

            // Đơn đã được shipper nhận / đang giao / đã giao
            predicates.add(root.get("status").in(
                    OrderStatus.PICKED_UP,
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

            // Đơn đã đến bưu cục nhưng chưa có shipper nhận
            predicates.add(root.get("status").in(
                    OrderStatus.CONFIRMED,
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

    public ApiResponse<Map<String, Object>> getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        return new ApiResponse<>(true, "Lấy thông tin đơn hàng thành công", mapOrderDetail(order));
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

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.AT_DEST_OFFICE) {
            return new ApiResponse<>(false, "Chỉ có thể nhận đơn đã xác nhận hoặc đã đến bưu cục đích", null);
        }

        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);
        saveHistory(order, OrderHistoryActionType.PICKED_UP, "Shipper nhận đơn để giao");
        return new ApiResponse<>(true, "Nhận đơn thành công", null);
    }

    @Transactional
    public ApiResponse<String> unclaimOrder(Integer id) {
        Employee employee = getCurrentEmployee();
        Integer officeId = employee.getOffice().getId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getToOffice() == null || !Objects.equals(order.getToOffice().getId(), officeId)) {
            return new ApiResponse<>(false, "Đơn hàng không thuộc bưu cục của bạn", null);
        }

        if (order.getStatus() != OrderStatus.PICKED_UP) {
            return new ApiResponse<>(false, "Chỉ có thể hủy nhận đơn ở trạng thái đã lấy hàng", null);
        }

        // Trả về trạng thái trước đó
        if (order.getFromOffice() != null && order.getToOffice() != null
                && Objects.equals(order.getFromOffice().getId(), order.getToOffice().getId())) {
            order.setStatus(OrderStatus.CONFIRMED);
        } else {
            order.setStatus(OrderStatus.AT_DEST_OFFICE);
        }

        orderRepository.save(order);
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
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Phường ").append(address.getWardCode());
        }
        if (address.getCityCode() != null) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("TP ").append(address.getCityCode());
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


