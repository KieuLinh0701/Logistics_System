package com.logistics.service.shipper.impl;

import com.logistics.dto.shipper.ShipperActiveShipmentDto;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.exception.enums.ShipmentErrorCode;
import com.logistics.repository.*;
import com.logistics.request.shipper.PickedUpRequest;
import com.logistics.request.shipper.UpdateDeliveryStatusRequest;
import com.logistics.service.common.NotificationService;
import com.logistics.entity.AiRoutePlanRoute;
import com.logistics.entity.AiRoutePlanStop;
import com.logistics.enums.RouteStopStatus;
import com.logistics.repository.AiRoutePlanRouteRepository;
import com.logistics.repository.AiRoutePlanStopRepository;
import com.logistics.service.shipper.OrderShipperService;
import com.logistics.service.shipper.ShipmentDeliveryService;
import com.logistics.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ShipmentDeliveryServiceImpl implements ShipmentDeliveryService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final AiRoutePlanRouteRepository aiRoutePlanRouteRepository;
    private final AiRoutePlanStopRepository aiRoutePlanStopRepository;

    private final OrderShipperService orderShipperService;

    @Autowired
    public ShipmentDeliveryServiceImpl(
            ShipmentRepository shipmentRepository,
            ShipmentOrderRepository shipmentOrderRepository,
            OrderRepository orderRepository,
            OrderHistoryRepository orderHistoryRepository,
            EmployeeRepository employeeRepository,
            NotificationService notificationService,
            AiRoutePlanRouteRepository aiRoutePlanRouteRepository,
            AiRoutePlanStopRepository aiRoutePlanStopRepository,
            @Lazy OrderShipperService orderShipperService) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.orderRepository = orderRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.aiRoutePlanRouteRepository = aiRoutePlanRouteRepository;
        this.aiRoutePlanStopRepository = aiRoutePlanStopRepository;
        this.orderShipperService = orderShipperService;
    }

    private Employee getCurrentEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.getFirst();
    }

    @Override
    public Shipment requireActiveDeliveryShipmentForOrder(Integer orderId) {
        Employee employee = getCurrentEmployee();
        return shipmentRepository.findActiveDeliveryShipmentForOrder(employee.getId(), orderId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER));
    }

    @Override
    public Shipment requireActiveInTransitShipmentForOrder(Integer orderId) {
        Employee employee = getCurrentEmployee();
        return shipmentRepository.findActiveInTransitDeliveryShipmentForOrder(employee.getId(), orderId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER,
                        "Vui lòng bắt đầu chuyến trước khi xử lý đơn hàng."));
    }

    @Override
    public Shipment requirePendingDeliveryShipmentForOrder(Integer orderId) {
        Employee employee = getCurrentEmployee();
        return shipmentRepository.findPendingDeliveryShipmentForOrder(employee.getId(), orderId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_ORDER_NOT_IN_SHIPMENT_OF_SHIPPER,
                        "Đơn hàng không thuộc chuyến hàng PENDING của bạn"));
    }

    @Override
    public Shipment loadDeliveryShipment(Integer shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        if (shipment.getType() != ShipmentType.DELIVERY) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_DELIVERY);
        }
        return shipment;
    }

    private void saveHistory(Order order, Shipment shipment, OrderHistoryActionType action, String note) {
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setFromOffice(order.getFromOffice());
        history.setToOffice(order.getToOffice());
        history.setShipment(shipment);
        history.setAction(action);
        history.setNote(note);
        orderHistoryRepository.save(history);
    }

    // Kiểm tra xem order có được coi là đã xử lý xong stop trên route hay không.
    private boolean isStopHandledForRoute(OrderStatus status, RouteStopType stopType) {
        if (stopType == RouteStopType.PICKUP) {
            // Pickup stop hoàn thành khi:
            // - PICKED_UP: shipper đã lấy hàng
            // - AT_ORIGIN_OFFICE: shipper đã nộp bưu cục
            // - PICKUP_FAILED_FINAL: giao hàng không thành công
            // - PICKUP_RETRY: shipper đã xử lý xong lần lấy hàng này, sẽ quay lại sau
            // - CANCELLED: đơn bị hủy
            return status == OrderStatus.PICKED_UP
                    || status == OrderStatus.AT_ORIGIN_OFFICE
                    || status == OrderStatus.PICKUP_FAILED_FINAL
                    || status == OrderStatus.PICKUP_RETRY
                    || status == OrderStatus.CANCELLED;
        }
        if (stopType == RouteStopType.RETURN_TO_OFFICE) {
            return status == OrderStatus.RETURNED
                    || status == OrderStatus.RETURN_FAILED_FINAL
                    || status == OrderStatus.AT_ORIGIN_OFFICE
                    || status == OrderStatus.RETURN_AT_ORIGIN_OFFICE
                    || status == OrderStatus.CANCELLED;
        }
        // DELIVERY: completed = đã xử lý xong điểm giao (thành công hoặc thất bại tạm thời)
        // RETURNING = đang hoàn hàng về bưu cục, stop đã xử lý xong
        return status == OrderStatus.DELIVERED
                || status == OrderStatus.DELIVERY_RETRY
                || status == OrderStatus.DELIVERY_FAILED_FINAL
                || status == OrderStatus.AT_DEST_OFFICE
                || status == OrderStatus.RETURNING
                || status == OrderStatus.CANCELLED;
    }

    /**
     * Kiểm tra xem order status có được coi là completed cho delivery shipment hay không.
     * Dùng trong finishShipment() để validate trước khi cho phép kết thúc thủ công.
     */
    private boolean isCompletedForDeliveryShipment(OrderStatus status) {
        return status == OrderStatus.DELIVERED
                || status == OrderStatus.AT_DEST_OFFICE
                || status == OrderStatus.RETURNING
                || status == OrderStatus.CANCELLED;
    }

    private static String safeCodeName(com.logistics.exception.enums.BaseErrorCode code) {
        if (code == null) return null;
        try {
            if (code.getClass().isEnum()) {
                Object[] constants = code.getClass().getEnumConstants();
                for (Object c : constants) {
                    if (c == code) {
                        return ((Enum<?>) c).name();
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return code.getClass().getSimpleName();
    }

    // Kiểm tra xem shipment còn item nào cần shipper nộp về bưu cục hay không.: DELIVERY_RETRY và PICKED_UP với PICKUP_BY_COURIER
    private boolean hasItemsNeedReturnToOffice(Shipment shipment) {
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        if (shipmentOrders == null || shipmentOrders.isEmpty()) {
            return false;
        }
        for (ShipmentOrder so : shipmentOrders) {
            Order order = so.getOrder();
            if (order == null) continue;

            OrderStatus status = order.getStatus();
            RouteStopType stopType = so.getStopType();

            // DELIVERY stop: đơn cần nộp bưu cục
            if (stopType == RouteStopType.DELIVERY) {
                // DELIVERY_RETRY: giao lại (vẫn còn trên xe)
                if (status == OrderStatus.DELIVERY_RETRY) {
                    return true;
                }
                // DELIVERY_FAILED_FINAL: giao thất bại cuối cùng - cần nộp bưu cục nếu chưa nộp
                // Chỉ cần nộp khi employee != null (shipper đang giữ hàng)
                if (status == OrderStatus.DELIVERY_FAILED_FINAL && order.getEmployee() != null) {
                    return true;
                }
                // RETURN_FAILED_FINAL: giao hoàn thất bại cuối cùng - cần nộp bưu cục nếu chưa nộp
                // Chỉ cần nộp khi employee != null (shipper đang giữ hàng)
                if (status == OrderStatus.RETURN_FAILED_FINAL && order.getEmployee() != null) {
                    return true;
                }
            }

            // RETURN_TO_OFFICE stop: đơn hoàn cần nộp bưu cục
            if (stopType == RouteStopType.RETURN_TO_OFFICE) {
                // RETURN_FAILED_FINAL: hoàn thất bại cuối cùng - cần nộp bưu cục nếu chưa nộp
                if (status == OrderStatus.RETURN_FAILED_FINAL && order.getEmployee() != null) {
                    return true;
                }
            }

            // Pickup đã lấy cần nộp bưu cục
            if (stopType == RouteStopType.PICKUP
                    && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER
                    && status == OrderStatus.PICKED_UP) {
                return true;
            }
        }
        return false;
    }

    private boolean isShipmentWorkDone(Shipment shipment) {
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        if (shipmentOrders == null || shipmentOrders.isEmpty()) {
            return false;
        }
        // Tất cả stop phải được xử lý xong
        boolean allStopsHandled = shipmentOrders.stream()
                .allMatch(so -> isStopHandledForRoute(so.getOrder().getStatus(), so.getStopType()));
        if (!allStopsHandled) {
            return false;
        }
        // Không còn item cần nộp bưu cục
        return !hasItemsNeedReturnToOffice(shipment);
    }

    private void checkAndAutoFinish(Shipment shipment) {
        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            return;
        }
        if (isShipmentWorkDone(shipment)) {
            finishShipmentInternal(shipment);
        }
    }

    private void finishShipmentInternal(Shipment shipment) {
        shipment.setStatus(ShipmentStatus.COMPLETED);
        shipment.setEndTime(LocalDateTime.now());
        shipmentRepository.save(shipment);
        // Deactivate tất cả AI route đang gắn với shipment để tránh fallback vào
        // AI route cũ trong getDeliveryRoute() - nguyên nhân gốc khiến đơn đã
        // giao vẫn hiển thị trên /shipper/route.
        deactivateAiRoutesForShipment(shipment);
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        for (ShipmentOrder so : shipmentOrders) {
            saveHistory(so.getOrder(), shipment, OrderHistoryActionType.CONFIRMED,
                    "Hoàn tất chuyến DELIVERY " + shipment.getCode());
        }
    }

    /**
     * Mapping trạng thái Order -> RouteStopStatus cho {@link AiRoutePlanStop}.
     * - DELIVERED, RETURNED, AT_DEST_OFFICE, RETURN_AT_ORIGIN_OFFICE,
     *   AT_ORIGIN_OFFICE, PICKED_UP (delivery leg)               => COMPLETED
     * - DELIVERY_FAILED_FINAL, RETURN_FAILED_FINAL,
     *   PICKUP_FAILED_FINAL, CANCELLED                           => FAILED
     * - DELIVERY_RETRY, PICKUP_RETRY, RETURN_RETRY               => PENDING (giữ nguyên,
     *   vì shipper sẽ quay lại xử lý stop này)
     */
    private RouteStopStatus mapOrderStatusToStopStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DELIVERED, AT_DEST_OFFICE, RETURNED,
                 RETURN_AT_ORIGIN_OFFICE, AT_ORIGIN_OFFICE -> RouteStopStatus.COMPLETED;
            case DELIVERY_FAILED_FINAL, RETURN_FAILED_FINAL,
                 PICKUP_FAILED_FINAL, CANCELLED -> RouteStopStatus.FAILED;
            default -> null;
        };
    }

    /**
     * Đồng bộ AiRoutePlanStop.stopStatus theo Order.status mới.
     * Chỉ sync trên các stop thuộc route đang is_active=true (route còn "sống"
     * với shipper). Helper này không bao giờ throw để tránh làm fail flow chính
     * - lỗi chỉ được log warning.
     */
    private void syncAiRouteStopStatus(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        RouteStopStatus mapped = mapOrderStatusToStopStatus(order.getStatus());
        if (mapped == null) {
            return;
        }
        try {
            List<AiRoutePlanStop> stops = aiRoutePlanStopRepository.findActiveByOrderId(order.getId());
            if (stops == null || stops.isEmpty()) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            for (AiRoutePlanStop stop : stops {
                boolean needStopStatusUpdate = stop.getStopStatus() != mapped
                        || (mapped == RouteStopStatus.COMPLETED && stop.getActualCompletedAt() == null);
                if (!needStopStatusUpdate) {
                    continue;
                }
                stop.setStopStatus(mapped);
                if (mapped == RouteStopStatus.COMPLETED) {
                    stop.setActualCompletedAt(now);
                    if (stop.getActualArrivedAt() == null) {
                        stop.setActualArrivedAt(now);
                    }
                }
                aiRoutePlanStopRepository.save(stop);
                log.info("[AI_STOP_SYNC] orderId={} status={} -> stopId={} routeId={} stopStatus={} actualCompletedAt={}",
                        order.getId(), order.getStatus(),
                        stop.getId(), stop.getRoute() != null ? stop.getRoute().getId() : null,
                        mapped, mapped == RouteStopStatus.COMPLETED ? now : null);
            }
        } catch (Exception ex) {
            log.warn("[AI_STOP_SYNC_FAILED] orderId={} status={} reason={}",
                    order.getId(), order.getStatus(), ex.getMessage());
        }
    }

    /**
     * Deactivate tất cả AiRoutePlanRoute còn isActive=true và đang gắn với
     * shipment này. Chỉ set isActive=false + actualCompletedAt + reoptimizeReason
     * để giữ lịch sử re-optimize cho manager xem sau.
     */
    private void deactivateAiRoutesForShipment(Shipment shipment) {
        if (shipment == null || shipment.getId() == null) {
            return;
        }
        try {
            List<AiRoutePlanRoute> aiRoutes = aiRoutePlanRouteRepository.findActiveByShipmentId(shipment.getId());
            if (aiRoutes == null || aiRoutes.isEmpty()) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            for (AiRoutePlanRoute r : aiRoutes) {
                r.setIsActive(false);
                r.setActualCompletedAt(now);
                if (r.getReoptimizedAt() == null) {
                    r.setReoptimizedAt(now);
                }
                if (r.getReoptimizeReason() == null
                        || !r.getReoptimizeReason().contains("SHIPMENT_COMPLETED")) {
                    r.setReoptimizeReason(
                            (r.getReoptimizeReason() != null ? r.getReoptimizeReason() + " | " : "")
                                    + "SHIPMENT_COMPLETED:" + shipment.getCode());
                }
                aiRoutePlanRouteRepository.save(r);
                log.info("[AI_ROUTE_DEACTIVATED] routeId={} shipmentId={} shipmentCode={}",
                        r.getId(), shipment.getId(), shipment.getCode());
            }
        } catch (Exception ex) {
            log.warn("[AI_ROUTE_DEACTIVATION_FAILED] shipmentId={} reason={}",
                    shipment.getId(), ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void startShipment(Integer shipmentId) {
        Shipment shipment = loadDeliveryShipment(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.PENDING) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_PENDING);
        }
        if (shipment.getEmployee() == null) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_ASSIGNED);
        }

        Employee caller = getCurrentEmployee();
        boolean isAssignedShipper = Objects.equals(shipment.getEmployee().getId(), caller.getId());
        boolean isManager = SecurityUtils.hasRole("manager") || SecurityUtils.hasRole("admin");
        if (!isAssignedShipper && !isManager) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_ASSIGNED);
        }

        if (!isManager) {
            List<Shipment> activeByShipper = shipmentRepository
                    .findActiveDeliveryShipmentsByEmployee(shipment.getEmployee().getId());
            boolean hasOtherInTransit = activeByShipper.stream()
                    .anyMatch(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT
                            && !s.getId().equals(shipment.getId()));
            if (hasOtherInTransit) {
                throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER,
                        "Shipper đã có chuyến DELIVERY đang chạy. Vui lòng hoàn tất trước khi bắt đầu chuyến mới.");
            }
        }

        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        if (shipmentOrders == null || shipmentOrders.isEmpty()) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_EMPTY);
        }

        List<String> invalidOrders = new ArrayList<>();

        for (ShipmentOrder so : shipmentOrders) {
            Order order = so.getOrder();
            RouteStopType stopType = so.getStopType();

            boolean valid = false;
            if (stopType == RouteStopType.PICKUP) {
                valid = order.getStatus() == OrderStatus.PICKING_UP
                        || order.getStatus() == OrderStatus.READY_FOR_PICKUP;
            } else if (stopType == RouteStopType.RETURN_TO_OFFICE) {
                valid = true;
            } else {
                valid = order.getStatus() == OrderStatus.PICKED_UP;
            }

            if (!valid) {
                invalidOrders.add(order.getTrackingNumber() + " (" + order.getStatus().name() + ")");
            }
        }

        if (!invalidOrders.isEmpty()) {
            throw new AppException(
                    ShipmentErrorCode.SHIPMENT_ORDERS_NOT_SCANNED,
                    "Không thể bắt đầu chuyến. Vẫn còn đơn hàng chưa lên xe."
            );
        }

        for (ShipmentOrder so : shipmentOrders) {
            Order order = so.getOrder();
            if (so.getStopType() == RouteStopType.DELIVERY) {
                order.setStatus(OrderStatus.DELIVERING);
                orderRepository.save(order);
                saveHistory(order, shipment, OrderHistoryActionType.DELIVERING,
                        "Shipper bắt đầu chuyến giao hàng (chuyến " + shipment.getCode() + ")");
            } else if (so.getStopType() == RouteStopType.RETURN_TO_OFFICE) {
                saveHistory(order, shipment, OrderHistoryActionType.RETURNING,
                        "Shipper bắt đầu chuyến giao trả hàng hoàn (chuyến " + shipment.getCode() + ")");
            } else {
                saveHistory(order, shipment, OrderHistoryActionType.PICKING_UP,
                        "Shipper bắt đầu chuyến lấy hàng (chuyến " + shipment.getCode() + ")");
            }
        }

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setStartTime(LocalDateTime.now());
        shipmentRepository.save(shipment);
    }

    @Override
    @Transactional
    public void finishShipment(Integer shipmentId) {
        Shipment shipment = loadDeliveryShipment(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_STARTED);
        }
        Employee caller = getCurrentEmployee();
        boolean isAssignedShipper = shipment.getEmployee() != null
                && Objects.equals(shipment.getEmployee().getId(), caller.getId());
        boolean isManager = SecurityUtils.hasRole("manager") || SecurityUtils.hasRole("admin");
        if (!isAssignedShipper && !isManager) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_ASSIGNED);
        }

        if (!isShipmentWorkDone(shipment)) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_HAS_ACTIVE_ORDERS);
        }

        finishShipmentInternal(shipment);
    }

    @Override
    public Map<String, Object> acceptPickupRequest(Integer orderId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            return doAcceptPickupRequest(orderId, resp);
        } catch (PessimisticLockingFailureException ple) {
            log.warn("[ACCEPT_PICKUP_LOCK_TIMEOUT] orderId={} message={}",
                    orderId, ple.getMessage());
            resp.put("success", false);
            resp.put("message", "Đơn đang được xử lý, vui lòng thử lại sau");
            resp.put("orderId", orderId);
            resp.put("shipmentId", null);
            resp.put("requiresReoptimize", false);
            resp.put("errorCode", "LOCK_TIMEOUT");
            return resp;
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException oole) {
            log.warn("[ACCEPT_PICKUP_OPTIMISTIC_LOCK] orderId={} message={}",
                    orderId, oole.getMessage());
            resp.put("success", false);
            resp.put("message", "Đơn đã được nhận bởi shipper khác. Vui lòng tải lại danh sách.");
            resp.put("orderId", orderId);
            resp.put("shipmentId", null);
            resp.put("requiresReoptimize", false);
            resp.put("errorCode", "OPTIMISTIC_LOCK");
            return resp;
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            log.warn("[ACCEPT_PICKUP_DATA_INTEGRITY] orderId={} message={}",
                    orderId, dive.getMessage());
            resp.put("success", false);
            resp.put("message", "Đơn đã được nhận bởi shipper khác hoặc đã có trong chuyến khác. Vui lòng tải lại danh sách.");
            resp.put("orderId", orderId);
            resp.put("shipmentId", null);
            resp.put("requiresReoptimize", false);
            resp.put("errorCode", "DATA_INTEGRITY");
            return resp;
        } catch (AppException ae) {
            log.warn("[ACCEPT_PICKUP_APP_EXCEPTION] orderId={} code={} message={}",
                    orderId, ae.getErrorCode(), ae.getMessage());
            resp.put("success", false);
            resp.put("message", ae.getMessage());
            resp.put("orderId", orderId);
            resp.put("shipmentId", null);
            resp.put("requiresReoptimize", false);
            resp.put("errorCode", ae.getErrorCode() != null
                    ? ae.getErrorCode().getClass().getSimpleName() + ":" + safeCodeName(ae.getErrorCode())
                    : null);
            return resp;
        } catch (Exception ex) {
            log.error("[ACCEPT_PICKUP_UNEXPECTED] orderId={} exClass={} message={}",
                    orderId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            resp.put("success", false);
            resp.put("message", "Đã xảy ra lỗi khi nhận đơn pickup. Vui lòng thử lại.");
            resp.put("orderId", orderId);
            resp.put("shipmentId", null);
            resp.put("requiresReoptimize", false);
            resp.put("errorCode", "UNEXPECTED");
            resp.put("errorClass", ex.getClass().getSimpleName());
            return resp;
        }
    }

    private Map<String, Object> doAcceptPickupRequest(Integer orderId, Map<String, Object> resp) {
        Employee employee = getCurrentEmployee();

        Order order = orderShipperService.quickClaimOrderForPickup(orderId, employee);

        List<Shipment> activeShipments = shipmentRepository
                .findActiveDeliveryShipmentsByEmployee(employee.getId());

        Optional<Shipment> activeShipmentOpt = activeShipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT
                        && s.getType() == com.logistics.enums.ShipmentType.DELIVERY)
                .findFirst();

        if (activeShipmentOpt.isEmpty()) {
            activeShipmentOpt = activeShipments.stream()
                    .filter(sh -> sh.getStatus() == ShipmentStatus.PENDING
                            && sh.getType() == com.logistics.enums.ShipmentType.DELIVERY)
                    .findFirst();
        }

        if (activeShipmentOpt.isEmpty()) {
            Shipment shipment = new Shipment();
            shipment.setStatus(ShipmentStatus.PENDING);
            shipment.setType(ShipmentType.DELIVERY);
            shipment.setEmployee(employee);
            shipment.setFromOffice(order.getFromOffice());
            shipment.setToOffice(order.getToOffice());
            shipmentRepository.save(shipment);

            try {
                com.logistics.request.shipper.InsertPickupShipmentRequest body = new com.logistics.request.shipper.InsertPickupShipmentRequest();
                body.setPickupOrderId(orderId);
                Map<String, Object> insertResult = orderShipperService.insertPickupIntoShipment(
                        shipment.getId(), body);

                if (order.getStatus() != OrderStatus.PICKING_UP) {
                    order.setStatus(OrderStatus.PICKING_UP);
                    orderRepository.save(order);
                }

                try {
                    saveHistory(order, shipment, OrderHistoryActionType.PICKING_UP,
                            "Shipper nhận đơn pickup và tự động tạo chuyến mới " + shipment.getCode());
                } catch (Exception he) {
                    log.warn("Failed to save new-shipment history: {}", he.getMessage());
                }

                insertResult.put("success", true);
                insertResult.put("message",
                        "Đã tạo chuyến mới và thêm đơn pickup. Vui lòng tối ưu lại tuyến.");

                shipment.setStatus(ShipmentStatus.IN_TRANSIT);
                shipment.setStartTime(LocalDateTime.now());
                shipmentRepository.save(shipment);

                return insertResult;

            } catch (Exception ex) {
                log.warn(
                        "acceptPickupRequest: create new shipment failed for orderId={} shipmentId={} ({}): {}",
                        orderId,
                        shipment.getId(),
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        ex);
                try {
                    saveHistory(order, shipment, OrderHistoryActionType.PENDING,
                            "Shipper đăng ký nhận đơn pickup (tạo chuyến mới thất bại: "
                                    + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
                } catch (Exception he) {
                    log.warn("Failed to save new-shipment fallback history: {}", he.getMessage());
                }

                resp.put("success", true);
                resp.put("message", "Nhận yêu cầu lấy hàng thành công. Đơn đã được thêm vào chuyến lấy hàng.");
                resp.put("orderId", orderId);
                resp.put("shipmentId", shipment.getId());
                resp.put("requiresReoptimize", false);
                resp.put("reason", "new_shipment_add_failed");
                resp.put("errorClass", ex.getClass().getSimpleName());
                resp.put("errorMessage", ex.getMessage());
                return resp;
            }
        }

        Shipment activeShipment = activeShipmentOpt.get();

        try {
            com.logistics.request.shipper.InsertPickupShipmentRequest body = new com.logistics.request.shipper.InsertPickupShipmentRequest();
            body.setPickupOrderId(orderId);
            Map<String, Object> insertResult = orderShipperService.insertPickupIntoShipment(
                    activeShipment.getId(), body);

            if (order.getStatus() != OrderStatus.PICKING_UP) {
                order.setStatus(OrderStatus.PICKING_UP);
                orderRepository.save(order);
            }

            try {
                saveHistory(order, activeShipment, OrderHistoryActionType.PICKING_UP,
                        "Shipper nhận đơn pickup và tự động thêm vào chuyến đang chạy "
                                + activeShipment.getCode());
            } catch (Exception he) {
                log.warn("Failed to save auto-add history: {}", he.getMessage());
            }

            insertResult.put("success", true);
            insertResult.put("message",
                    "Đã thêm đơn pickup vào chuyến đang chạy. Vui lòng tối ưu lại tuyến.");

            return insertResult;

        } catch (Exception ex) {
            log.warn(
                    "acceptPickupRequest: auto-add into shipmentId={} orderId={} failed ({}): {}",
                    activeShipment.getId(),
                    orderId,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex);
            try {
                saveHistory(order, activeShipment, OrderHistoryActionType.PENDING,
                        "Shipper đăng ký nhận đơn pickup (chờ gom vào chuyến - auto-add failed: "
                                + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
            } catch (Exception he) {
                log.warn("Failed to save fallback history: {}", he.getMessage());
            }

            resp.put("success", true);
            resp.put("message", "Nhận yêu cầu lấy hàng thành công. Đơn đã được thêm vào chuyến lấy hàng.");
            resp.put("orderId", orderId);
            resp.put("shipmentId", null);
            resp.put("requiresReoptimize", false);
            resp.put("reason", "auto_add_failed");
            resp.put("errorClass", ex.getClass().getSimpleName());
            resp.put("errorMessage", ex.getMessage());
            return resp;
        }
    }

    @Override
    @Transactional
    public void startPickup(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getPickupType() != OrderPickupType.PICKUP_BY_COURIER) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }

        Set<OrderStatus> allowed = EnumSet.of(
                OrderStatus.CONFIRMED,
                OrderStatus.PICKUP_RETRY,
                OrderStatus.READY_FOR_PICKUP
        );
        if (!allowed.contains(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.PICKING_UP);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.PICKING_UP,
                "Shipper bắt đầu đi lấy hàng trong chuyến " + shipment.getCode());
    }

    @Override
    @Transactional
    public void markPickedUp(Integer orderId, PickedUpRequest req) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requirePendingDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        if (order.getStatus() == OrderStatus.PICKED_UP) {
            return;
        }

        RouteStopType stopType = null;
        List<ShipmentOrder> sos = shipmentOrderRepository.findByShipmentId(shipment.getId());
        if (sos != null) {
            for (ShipmentOrder so : sos) {
                if (Objects.equals(so.getOrder().getId(), orderId)) {
                    stopType = so.getStopType();
                    break;
                }
            }
        }

        OrderStatus current = order.getStatus();
        Set<OrderStatus> allowed;
        if (stopType == RouteStopType.PICKUP) {
            allowed = EnumSet.of(
                    OrderStatus.PICKING_UP,
                    OrderStatus.PICKUP_RETRY,
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.CONFIRMED,
                    OrderStatus.URGENT_PICKUP
            );
        } else {
            allowed = EnumSet.of(OrderStatus.READY_FOR_PICKUP);
        }

        if (!allowed.contains(current)) {
            if (current == OrderStatus.AT_DEST_OFFICE) {
                throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS,
                        "Đơn chưa sẵn sàng lấy hàng tại bưu cục đích. Cần xác nhận trước.");
            }
            if (current == OrderStatus.DELIVERING || current == OrderStatus.DELIVERED) {
                throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS,
                        "Đơn đang ở trạng thái " + current.name() + ", không thể xác nhận lên xe.");
            }
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.PICKED_UP);
        if (req != null && req.getPhotoUrl() != null && !req.getPhotoUrl().isBlank()) {
            order.setPickupProofImageUrl(req.getPhotoUrl());
        }
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.PICKED_UP,
                "Shipper xác nhận đã lấy hàng (chuyến " + shipment.getCode() + ")");
        // Sync AI stop CHỈ khi stop hiện tại là PICKUP leg (PICKED_UP là terminal
        // cho PICKUP). Đối với DELIVERY leg, PICKED_UP không phải terminal.
        if (stopType == RouteStopType.PICKUP) {
            syncAiRouteStopStatus(order);
        }
        checkAndAutoFinish(shipment);
    }

    @Override
    @Transactional
    public void startDelivery(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.DELIVERING);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.DELIVERING,
                "Shipper bắt đầu giao hàng (chuyến " + shipment.getCode() + ")");
    }

    @Override
    @Transactional
    public Map<String, Object> startDeliveryAll(Integer shipmentId) {
        Shipment shipment = loadDeliveryShipment(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER,
                    "Shipment không ở trạng thái IN_TRANSIT");
        }

        Employee caller = getCurrentEmployee();
        boolean isAssignedShipper = shipment.getEmployee() != null
                && Objects.equals(shipment.getEmployee().getId(), caller.getId());
        boolean isManager = SecurityUtils.hasRole("manager") || SecurityUtils.hasRole("admin");
        if (!isAssignedShipper && !isManager) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_NOT_ASSIGNED);
        }

        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipmentId);
        if (shipmentOrders == null || shipmentOrders.isEmpty()) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_EMPTY);
        }

        List<Integer> updatedOrderIds = new ArrayList<>();
        List<Integer> skippedOrderIds = new ArrayList<>();
        int updatedCount = 0;
        int skippedCount = 0;

        for (ShipmentOrder so : shipmentOrders) {
            Order order = so.getOrder();
            if (order == null) {
                continue;
            }
            if (order.getStatus() == OrderStatus.PICKED_UP) {
                RouteStopType stopType = so.getStopType();
                if (stopType == RouteStopType.PICKUP) {
                    skippedOrderIds.add(order.getId());
                    skippedCount++;
                } else {
                    order.setStatus(OrderStatus.DELIVERING);
                    orderRepository.save(order);
                    saveHistory(order, shipment, OrderHistoryActionType.DELIVERING,
                            "Shipper bắt đầu giao hàng (bulk, chuyến " + shipment.getCode() + ")");
                    updatedOrderIds.add(order.getId());
                    updatedCount++;
                }
            } else {
                skippedOrderIds.add(order.getId());
                skippedCount++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shipmentId", shipmentId);
        result.put("shipmentCode", shipment.getCode());
        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        result.put("updatedOrderIds", updatedOrderIds);
        result.put("skippedOrderIds", skippedOrderIds);
        return result;
    }

    @Override
    @Transactional
    public void markDelivered(Integer orderId, UpdateDeliveryStatusRequest req) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new AppException(OrderErrorCode.ORDER_NOT_DELIVERING);
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.DELIVERED,
                "Giao hàng thành công (chuyến " + shipment.getCode() + ")");
        syncAiRouteStopStatus(order);
        checkAndAutoFinish(shipment);
    }

    @Override
    @Transactional
    public void markDeliveryFailed(Integer orderId, UpdateDeliveryStatusRequest req) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new AppException(OrderErrorCode.ORDER_NOT_DELIVERING);
        }

        order.setStatus(OrderStatus.DELIVERY_RETRY);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.DELIVERY_RETRY,
                "Giao thất bại, sẽ thử lại (chuyến " + shipment.getCode() + ")");
        checkAndAutoFinish(shipment);
    }

    @Override
    @Transactional
    public void markDeliveryFailedFinal(Integer orderId, UpdateDeliveryStatusRequest req) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new AppException(OrderErrorCode.ORDER_NOT_DELIVERING);
        }

        order.setStatus(OrderStatus.DELIVERY_FAILED_FINAL);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.DELIVERY_FAILED_FINAL,
                "Giao thất bại cuối cùng (chuyến " + shipment.getCode() + ")");

        order.setStatus(OrderStatus.RETURNING);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.RETURNING,
                "Tự động chuyển sang hoàn hàng trong cùng chuyến " + shipment.getCode());
        // Sync stop tương ứng với trạng thái terminal (FAILED).
        syncAiRouteStopStatus(order);
    }

    @Override
    @Transactional
    public void returnFailedToDestOffice(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.DELIVERY_RETRY) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.AT_DEST_OFFICE);
        order.setCurrentOffice(shipment.getFromOffice());
        order.setEmployee(null);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.AT_DEST_OFFICE,
                "Trả hàng giao thất bại về bưu cục đích (chuyến " + shipment.getCode() + ")");
        syncAiRouteStopStatus(order);
    }

    @Override
    @Transactional
    public void returnFailedFinalToDestOffice(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.DELIVERY_FAILED_FINAL) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.RETURNING);
        order.setCurrentOffice(shipment.getFromOffice());
        order.setEmployee(null);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.RETURNING,
                "Nộp hàng giao thất bại quá số lần về bưu cục đích, chuyển luồng hoàn hàng (chuyến " + shipment.getCode() + ")");
        syncAiRouteStopStatus(order);
    }

    @Override
    @Transactional
    public void submitReturnFailedToOffice(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.RETURN_FAILED_FINAL) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        // Giữ nguyên status RETURN_FAILED_FINAL - hàng đã ở bưu cục gốc rồi
        order.setCurrentOffice(shipment.getFromOffice());
        order.setEmployee(null);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.RETURN_FAILED_FINAL,
                "Nộp hàng giao hoàn thất bại về bưu cục gốc (chuyến " + shipment.getCode() + ")");

        // Gửi notification cho người gửi
        if (order.getUser() != null) {
            Office fromOffice = shipment.getFromOffice();
            String officeName = fromOffice != null ? fromOffice.getName() : "bưu cục gốc";
            String officeAddress = fromOffice != null && fromOffice.getDetail() != null
                    ? fromOffice.getDetail() : "";
            notificationService.create(
                    "Hàng hoàn đã được lưu tại bưu cục",
                    String.format("Đơn hàng %s không thể giao tới địa chỉ của bạn. "
                                    + "Hàng hiện đang được lưu tại:\n%s\n%s\nVui lòng đến nhận hàng.",
                            order.getTrackingNumber(),
                            officeName,
                            officeAddress),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }

        // Check auto finish shipment
        checkAndAutoFinish(shipment);
    }

    @Override
    @Transactional
    public void startReturn(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (!EnumSet.of(OrderStatus.RETURN_RETRY, OrderStatus.RETURNING).contains(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        if (order.getStatus() == OrderStatus.RETURN_RETRY) {
            order.setStatus(OrderStatus.RETURNING);
            orderRepository.save(order);
            saveHistory(order, shipment, OrderHistoryActionType.RETURNING,
                    "Bắt đầu hoàn hàng (chuyến " + shipment.getCode() + ")");
        }
    }

    @Override
    @Transactional
    public void markReturnAtOrigin(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getStatus() != OrderStatus.RETURNING) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.RETURN_AT_ORIGIN_OFFICE);
        order.setCurrentOffice(shipment.getFromOffice());
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.RETURN_AT_ORIGIN_OFFICE,
                "Đã về bưu cục gốc khi hoàn hàng (chuyến " + shipment.getCode() + ")");
        syncAiRouteStopStatus(order);

        if (order.getUser() != null) {
            String title;
            String content;
            if (order.getPickupType() == OrderPickupType.AT_OFFICE) {
                title = "Đơn hàng hoàn đã về bưu cục gốc";
                content = String.format("Đơn %s đã về bưu cục gốc. Vui lòng đến bưu cục để nhận lại hàng.", order.getTrackingNumber());
            } else {
                title = "Đơn hàng hoàn đã về bưu cục gốc";
                content = String.format("Đơn %s đã về bưu cục gốc. Chúng tôi sẽ sắp xếp giao trả đến địa chỉ của bạn.", order.getTrackingNumber());
            }
            notificationService.create(title, content, "order_status", order.getUser().getId(), null, "orders/tracking", order.getTrackingNumber());
        }
    }

    @Override
    @Transactional
    public void deliverToOrigin(Integer orderId) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }
        if (order.getPickupType() != OrderPickupType.PICKUP_BY_COURIER) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.AT_ORIGIN_OFFICE);
        order.setCurrentOffice(shipment.getFromOffice());
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.AT_DEST_OFFICE,
                "Nộp hàng về bưu cục gốc (chuyến " + shipment.getCode() + ")");
        syncAiRouteStopStatus(order);
        checkAndAutoFinish(shipment);
    }

    @Override
    @Transactional
    public void finalizeReturn(Integer orderId) {
        boolean isManager = SecurityUtils.hasRole("manager") || SecurityUtils.hasRole("admin");
        if (!isManager) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_ACCESS_DENIED);
        }
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.RETURN_AT_ORIGIN_OFFICE) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        if (order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS,
                    "Đơn PICKUP_BY_COURIER phải qua luồng giao trả tận nhà, không được xác nhận hoàn trực tiếp");
        }

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setFromOffice(order.getCurrentOffice());
        history.setToOffice(order.getCurrentOffice());
        history.setAction(OrderHistoryActionType.RETURNED);
        history.setNote("Manager xac nhan hoan hang thanh cong");
        orderHistoryRepository.save(history);

        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByOrderId(orderId);
        if (shipmentOrders != null && !shipmentOrders.isEmpty()) {
            Shipment linkedShipment = shipmentOrders.get(0).getShipment();
            syncAiRouteStopStatus(order);
            checkAndAutoFinish(linkedShipment);
        }
    }

    @Override
    @Transactional
    public void markReturnDelivered(Integer orderId, String proofImageUrl) {
        Employee employee = getCurrentEmployee();
        // Yêu cầu shipment phải IN_TRANSIT, không cho thao tác khi shipment còn PENDING
        Shipment shipment = requireActiveInTransitShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getPickupType() != OrderPickupType.PICKUP_BY_COURIER) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID,
                    "Chỉ áp dụng cho đơn PICKUP_BY_COURIER");
        }

        if (order.getStatus() != OrderStatus.RETURN_AT_ORIGIN_OFFICE
                && order.getStatus() != OrderStatus.RETURNING) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS,
                    "Đơn phải có trạng thái RETURN_AT_ORIGIN_OFFICE hoặc RETURNING");
        }

        boolean hasPermission = false;

        if (order.getEmployee() != null && Objects.equals(order.getEmployee().getId(), employee.getId())) {
            hasPermission = true;
        }

        if (!hasPermission) {
            List<Shipment> activeShipments = shipmentOrderRepository.findActiveShipmentsForOrder(orderId);
            for (Shipment s : activeShipments) {
                if (s.getType() == ShipmentType.DELIVERY
                        && s.getEmployee() != null
                        && Objects.equals(s.getEmployee().getId(), employee.getId())
                        && (s.getStatus() == ShipmentStatus.PENDING || s.getStatus() == ShipmentStatus.IN_TRANSIT)) {
                    hasPermission = true;
                    break;
                }
            }
        }

        if (!hasPermission) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_PERMISSION_DENIED,
                    "Bạn không có quyền xác nhận giao trả cho đơn hàng này");
        }

        // shipment đã được lấy từ requireActiveInTransitShipmentForOrder ở trên

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(LocalDateTime.now());

        if (proofImageUrl != null && !proofImageUrl.isBlank()) {
            try {
                java.lang.reflect.Field field = order.getClass().getDeclaredField("proofImageUrl");
                field.setAccessible(true);
                field.set(order, proofImageUrl);
            } catch (NoSuchFieldException | IllegalAccessException e) {
            }
        }

        orderRepository.save(order);

        String note = shipment != null
                ? "Giao trả hàng hoàn thành công (chuyến " + shipment.getCode() + ")"
                : "Giao trả hàng hoàn thành công";
        saveHistory(order, shipment, OrderHistoryActionType.RETURNED, note);

        syncAiRouteStopStatus(order);

        if (order.getUser() != null) {
            notificationService.create(
                    "Đơn hàng đã được hoàn trả thành công",
                    String.format("Đơn %s đã được hoàn trả thành công. Cảm ơn bạn đã sử dụng dịch vụ.", order.getTrackingNumber()),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }

        if (shipment != null) {
            checkAndAutoFinish(shipment);
        }
    }

    @Override
    @Transactional
    public void markReturnFailedFinal(Integer orderId, UpdateDeliveryStatusRequest req) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.RETURN_AT_ORIGIN_OFFICE
                && order.getStatus() != OrderStatus.RETURNING) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS,
                    "Đơn phải có trạng thái RETURN_AT_ORIGIN_OFFICE hoặc RETURNING");
        }

        boolean hasPermission = false;
        if (order.getEmployee() != null && Objects.equals(order.getEmployee().getId(), employee.getId())) {
            hasPermission = true;
        }
        if (!hasPermission && shipment.getEmployee() != null
                && Objects.equals(shipment.getEmployee().getId(), employee.getId())) {
            hasPermission = true;
        }
        if (!hasPermission) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_PERMISSION_DENIED,
                    "Bạn không có quyền xác nhận giao hoàn thất bại cho đơn hàng này");
        }

        order.setStatus(OrderStatus.RETURN_FAILED_FINAL);
        // Shipper vẫn đang giữ hàng, chưa nộp về bưu cục
        // Giữ nguyên employee để submitReturnFailedToOffice() có thể verify
        // Order sẽ xuất hiện ở "Hàng cần nộp bưu cục" vì listOrders gọi với status = RETURN_FAILED_FINAL
        orderRepository.save(order);
        syncAiRouteStopStatus(order);

        String failReason = req != null && req.getFailReason() != null ? req.getFailReason() : "OTHER";
        String note = req != null && req.getNotes() != null ? req.getNotes() : "Giao hoàn thất bại";
        saveHistory(order, shipment, OrderHistoryActionType.RETURN_FAILED_FINAL,
                note + " (Lý do: " + failReason + ", chuyến " + shipment.getCode() + ")");

        if (order.getUser() != null) {
            notificationService.create(
                    "Không thể giao hoàn đơn hàng",
                    String.format("Đơn hàng %s không thể giao tới địa chỉ của bạn. "
                                    + "Hàng hiện đang được lưu tại bưu cục %s. Vui lòng đến nhận hàng.",
                            order.getTrackingNumber(),
                            shipment.getFromOffice() != null ? shipment.getFromOffice().getName() : "bưu cục gốc"),
                    "order_status",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    private Shipment loadDeliveryShipmentForOrder(Integer orderId) {
        List<Shipment> active = shipmentOrderRepository.findActiveShipmentsForOrder(orderId);
        Shipment shipment = active.stream()
                .filter(s -> s.getType() == ShipmentType.DELIVERY)
                .findFirst()
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER));
        return shipment;
    }

    @Override
    public List<ShipperActiveShipmentDto> listActiveShipmentsForCurrentShipper() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();

        List<Employee> employeesByUserId = employeeRepository.findByUserId(userId);

        Employee employee = null;
        if (employeesByUserId != null && !employeesByUserId.isEmpty()) {
            for (Employee emp : employeesByUserId) {
                if (emp.getAccountRole() != null && emp.getAccountRole().getRole() != null
                        && "Shipper".equalsIgnoreCase(emp.getAccountRole().getRole().getName())) {
                    employee = emp;
                    break;
                }
            }
            if (employee == null) {
                employee = employeesByUserId.get(0);
            }
        }

        if (employee == null) {
            return List.of();
        }

        List<Shipment> shipments = shipmentRepository.findActiveDeliveryShipmentsByEmployee(employee.getId());

        return shipments.stream()
                .map(this::toShipperActiveShipmentDto)
                .toList();
    }

    private ShipperActiveShipmentDto toShipperActiveShipmentDto(Shipment s) {
        ShipperActiveShipmentDto.ShipperActiveShipmentDtoBuilder builder = ShipperActiveShipmentDto.builder()
                .id(s.getId())
                .code(s.getCode())
                .status(s.getStatus())
                .type(s.getType())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt());

        if (s.getVehicle() != null) {
            builder.vehicle(ShipperActiveShipmentDto.VehicleInfo.builder()
                    .id(s.getVehicle().getId())
                    .licensePlate(s.getVehicle().getLicensePlate())
                    .type(s.getVehicle().getType() != null ? s.getVehicle().getType().name() : null)
                    .build());
        }

        if (s.getEmployee() != null) {
            String fullName = null;
            String phone = null;
            if (s.getEmployee().getUser() != null) {
                fullName = s.getEmployee().getUser().getFullName();
                phone = s.getEmployee().getUser().getPhoneNumber();
            }
            builder.employee(ShipperActiveShipmentDto.EmployeeInfo.builder()
                    .id(s.getEmployee().getId())
                    .code(s.getEmployee().getCode())
                    .fullName(fullName)
                    .phone(phone)
                    .build());
        }

        if (s.getFromOffice() != null) {
            builder.fromOffice(ShipperActiveShipmentDto.OfficeInfo.builder()
                    .id(s.getFromOffice().getId())
                    .name(s.getFromOffice().getName())
                    .code(s.getFromOffice().getCode())
                    .build());
        }

        if (s.getToOffice() != null) {
            builder.toOffice(ShipperActiveShipmentDto.OfficeInfo.builder()
                    .id(s.getToOffice().getId())
                    .name(s.getToOffice().getName())
                    .code(s.getToOffice().getCode())
                    .build());
        }

        List<ShipmentOrder> shipmentOrders = s.getShipmentOrders();
        int totalOrders = (shipmentOrders != null) ? shipmentOrders.size() : 0;

        if (totalOrders > 0) {
            // Chỉ tính DELIVERY stops cho logic QR scan và start validation
            long deliveryStopCount = shipmentOrders.stream()
                    .filter(so -> so.getStopType() == RouteStopType.DELIVERY)
                    .count();
            long scannedCount = shipmentOrders.stream()
                    .filter(so -> so.getStopType() == RouteStopType.DELIVERY)
                    .filter(so -> so.getOrder() != null && so.getOrder().getStatus() == OrderStatus.PICKED_UP)
                    .count();
            // isReadyToStart: nếu không có delivery stop nào thì luôn ready (pickup-only shipment)
            boolean readyToStart = deliveryStopCount == 0 || scannedCount == deliveryStopCount;

            builder.totalOrders(totalOrders);
            builder.orderCount((int) deliveryStopCount);
            builder.totalCount((int) deliveryStopCount);
            builder.scannedCount((int) scannedCount);
            builder.isReadyToStart(readyToStart);
        } else {
            builder.totalOrders(0);
            builder.orderCount(0);
            builder.totalCount(0);
            builder.scannedCount(0);
            builder.isReadyToStart(true);
        }

        return builder.build();
    }

    @Override
    public void checkAndAutoFinishForOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByOrderId(orderId);
        for (ShipmentOrder so : shipmentOrders) {
            Shipment shipment = so.getShipment();
            if (shipment != null && shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
                checkAndAutoFinish(shipment);
            }
        }
    }
}
