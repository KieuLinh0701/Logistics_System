package com.logistics.service.shipper;

import com.logistics.dto.shipper.ShipperActiveShipmentDto;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.exception.enums.ShipmentErrorCode;
import com.logistics.repository.*;
import com.logistics.request.shipper.InsertPickupShipmentRequest;
import com.logistics.request.shipper.PickedUpRequest;
import com.logistics.request.shipper.UpdateDeliveryStatusRequest;
import com.logistics.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ShipmentDeliveryService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;
    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Lazy inject để tránh circular bean giữa OrderShipperService ↔ ShipmentDeliveryService.
     * Dùng để auto-add pickup vào shipment IN_TRANSIT đang chạy khi shipper accept.
     */
    @Autowired
    @Lazy
    private OrderShipperService orderShipperService;

    // ==================== Helpers ====================

    private Employee getCurrentEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        List<Employee> employees = employeeRepository.findByUserId(userId);
        if (employees == null || employees.isEmpty()) {
            throw new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employees.getFirst();
    }

    public Shipment requireActiveDeliveryShipmentForOrder(Integer orderId) {
        Employee employee = getCurrentEmployee();
        return shipmentRepository.findActiveDeliveryShipmentForOrder(employee.getId(), orderId)
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER));
    }

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

    private boolean isTerminalOrderStatus(OrderStatus status) {
        // DELIVERY_FAILED_FINAL được set tạm trong markDeliveryFailedFinal() rồi ngay lập tức
        // chuyển sang RETURNING trong cùng transaction. Vì vậy nó chỉ là transient state,
        // không tính là terminal. RETURNING mới là trạng thái thực sự cần xử lý tiếp.
        // Tương tự RETURN_FAILED_FINAL cũng là transient state (chuyển ngay sang RETURNED).
        return EnumSet.of(
                OrderStatus.DELIVERED,
                OrderStatus.AT_ORIGIN_OFFICE,
                OrderStatus.RETURN_AT_ORIGIN_OFFICE,
                OrderStatus.RETURNED,
                OrderStatus.CANCELLED
        ).contains(status);
    }

    /**
     * Lấy tên code (vd: SHIPMENT_NOT_ACTIVE_FOR_ORDER) từ BaseErrorCode.
     * Vì BaseErrorCode là interface (không phải enum), không gọi .name() trực tiếp được.
     * Enum implement interface thì vẫn có .name() ở concrete class.
     */
    private static String safeCodeName(com.logistics.exception.enums.BaseErrorCode code) {
        if (code == null) return null;
        try {
            // Enum constants thực thi Enum#name() qua reflection
            if (code.getClass().isEnum()) {
                Object[] constants = code.getClass().getEnumConstants();
                for (Object c : constants) {
                    if (c == code) {
                        return ((Enum<?>) c).name();
                    }
                }
            }
        } catch (Throwable ignore) {
            // fall through
        }
        return code.getClass().getSimpleName();
    }

    /**
     * Tự động finish shipment nếu tất cả orders đều ở trạng thái terminal.
     */
    private void checkAndAutoFinish(Shipment shipment) {
        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            return;
        }
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        if (shipmentOrders == null || shipmentOrders.isEmpty()) {
            return;
        }
        boolean allTerminal = shipmentOrders.stream()
                .map(ShipmentOrder::getOrder)
                .allMatch(o -> isTerminalOrderStatus(o.getStatus()));
        if (allTerminal) {
            finishShipmentInternal(shipment);
        }
    }

    private void finishShipmentInternal(Shipment shipment) {
        shipment.setStatus(ShipmentStatus.COMPLETED);
        shipment.setEndTime(LocalDateTime.now());
        shipmentRepository.save(shipment);
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        for (ShipmentOrder so : shipmentOrders) {
            saveHistory(so.getOrder(), shipment, OrderHistoryActionType.CONFIRMED,
                    "Hoàn tất chuyến DELIVERY " + shipment.getCode());
        }
    }

    // ==================== Shipment start/finish ====================

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

        // Ràng buộc: một shipper chỉ được có TỐI ĐA 1 shipment DELIVERY IN_TRANSIT tại một thời điểm.
        // Tránh shipper chạy 2 chuyến cùng lúc dẫn đến xung đột trạng thái đơn.
        // Manager vẫn được phép (override cho việc test/force start).
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

        // Auto transition orders theo rule mới (đã chốt với user)
        // DELIVERY orders: AT_DEST_OFFICE/READY_FOR_PICKUP GIỮ NGUYÊN - chờ shipper quét QR -> PICKED_UP
        // PICKUP orders: CONFIRMED/PICKUP_RETRY -> PICKING_UP (shipper đi lấy ngay)
        // RETURN orders: RETURN_RETRY -> RETURNING
        for (ShipmentOrder so : shipmentOrders) {
            Order order = so.getOrder();
            OrderStatus current = order.getStatus();
            OrderStatus next = current;
            OrderHistoryActionType action = null;
            String note = null;

            if (current == OrderStatus.AT_DEST_OFFICE) {
                // AT_DEST_OFFICE -> chuyển sang READY_FOR_PICKUP khi manager bắt đầu chuyến
                next = OrderStatus.READY_FOR_PICKUP;
                action = OrderHistoryActionType.READY_FOR_PICKUP;
                note = "Chuyến DELIVERY " + shipment.getCode() + " khởi hành, đơn chuyển sang sẵn sàng lấy hàng";
            } else if (current == OrderStatus.READY_FOR_PICKUP) {
                // GIỮ NGUYÊN - shipper sẽ quét QR từng đơn để chuyển PICKED_UP
                note = "Chuyến DELIVERY " + shipment.getCode() + " khởi hành, đơn chờ shipper quét QR";
            } else if (current == OrderStatus.CONFIRMED || current == OrderStatus.PICKUP_RETRY) {
                next = OrderStatus.PICKING_UP;
                action = OrderHistoryActionType.PICKING_UP;
                note = "Chuyến DELIVERY " + shipment.getCode() + " khởi hành, đơn pickup bắt đầu đi lấy";
            } else if (current == OrderStatus.RETURN_RETRY) {
                next = OrderStatus.RETURNING;
                action = OrderHistoryActionType.RETURNING;
                note = "Chuyến DELIVERY " + shipment.getCode() + " khởi hành, đơn chuyển sang đang hoàn";
            } else if (current == OrderStatus.RETURNING || current == OrderStatus.RETURN_AT_ORIGIN_OFFICE) {
                // giữ nguyên
            } else {
                log.warn("Shipment {} starts but order {} has unexpected status {}",
                        shipment.getCode(), order.getId(), current);
            }

            if (next != current) {
                order.setStatus(next);
            }
            // Set employee = shipment.employee nếu chưa có
            if (order.getEmployee() == null) {
                order.setEmployee(shipment.getEmployee());
            }
            orderRepository.save(order);
            if (action != null) {
                saveHistory(order, shipment, action, note);
            } else if (note != null) {
                saveHistory(order, shipment, OrderHistoryActionType.CONFIRMED, note);
            }
        }

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setStartTime(LocalDateTime.now());
        shipmentRepository.save(shipment);
    }

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

        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findByShipmentId(shipment.getId());
        if (shipmentOrders == null || shipmentOrders.isEmpty()) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_EMPTY);
        }

        boolean hasActive = shipmentOrders.stream()
                .map(ShipmentOrder::getOrder)
                .anyMatch(o -> !isTerminalOrderStatus(o.getStatus()));
        if (hasActive) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_HAS_ACTIVE_ORDERS);
        }

        finishShipmentInternal(shipment);
    }

    // ==================== Order actions (proxy sang OrderShipperService sẽ gọi qua đây) ====================

    /**
     * Shipper accept yêu cầu pickup:
     *  - Gán employee cho order.
     *  - Nếu shipper đang có shipment DELIVERY IN_TRANSIT → AUTO-ADD đơn vào shipment
     *    (tạo ShipmentOrder, set PICKING_UP nếu hợp lệ, gợi ý re-optimize).
     *  - Nếu chưa có shipment IN_TRANSIT → chỉ gán employee, chờ manager gom.
     *
     * Idempotent: nếu order đã thuộc shipment active thì trả message "Đã thuộc chuyến này"
     * và requiresReoptimize = false.
     */
    /**
     * ==================================================================
     * [LOCK-FREE accept-pickup flow]
     * Endpoint này KHÔNG dùng pessimistic lock (findByIdForUpdate) để tránh
     * lock wait timeout khi frontend gọi refresh ngay sau accept.
     *
     * Chiến lược:
     *   1. Đọc order bằng findById thường (không lock).
     *   2. Check status (CONFIRMED/READY_FOR_PICKUP/URGENT_PICKUP/PICKUP_RETRY).
     *   3. Update order: set employee + status=PICKING_UP + save.
     *      - Nếu có concurrent update (2 shipper cùng nhận) → 1 thắng, 1 nhận
     *        ObjectOptimisticLockingFailureException → trả success=false với
     *        message "Đơn đã được nhận bởi shipper khác".
     *   4. Các bước tiếp theo (findActiveShipments, insertPickupIntoShipment,
     *      saveHistory, notification) chạy NGOÀI lock vì order đã được update xong.
     *
     * Lưu ý:
     *   - KHÔNG @Transactional ở method này để các sub-tx (insertPickupIntoShipment
     *     REQUIRES_NEW, saveHistory, notification) tự commit độc lập.
     *   - Catch PessimisticLockingFailureException ở outer method để trả message
     *     thân thiện (phòng trường hợp lock từ code khác vẫn còn sót).
     * ==================================================================
     */
    public Map<String, Object> acceptPickupRequest(Integer orderId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            return doAcceptPickupRequest(orderId, resp);
        } catch (PessimisticLockingFailureException ple) {
            // Lock từ flow khác (vd: markPickedUp, finishShipment) - trả 200 với success=false
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
            // Race condition: 2 shipper cùng nhận 1 đơn
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
            // Trùng unique key, FK constraint, ...
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
            // Lỗi nghiệp vụ (vd: order đã bị claim, status không hợp lệ, ...)
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
            // Lỗi không mong đợi (vd: DB exception, validation phụ, AI route mirror, ...)
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

    /**
     * Thân thật của accept-pickup. Luôn được gọi từ {@link #acceptPickupRequest(Integer)}
     * nơi đã có try/catch bao ngoài để chuyển mọi exception thành response thân thiện.
     *
     * Không dùng pessimistic lock. Mọi sub-step (insertPickupIntoShipment,
     * saveHistory, notification) tự commit độc lập để không giữ lock lâu.
     */
    private Map<String, Object> doAcceptPickupRequest(Integer orderId, Map<String, Object> resp) {

        Employee employee = getCurrentEmployee();

        // [PHA 1 - LOCK-FREE] Gọi cross-bean OrderShipperService.quickClaimOrderForPickup
        // để proxy REQUIRES_NEW kích hoạt. Method này chạy trong transaction riêng, ngắn,
        // commit và release ngay -> không giữ lock qua các bước nặng phía dưới.
        Order order = orderShipperService.quickClaimOrderForPickup(orderId, employee);

        // Tìm shipment active của shipper - ưu tiên IN_TRANSIT trước, sau đó PENDING
        List<Shipment> activeShipments = shipmentRepository
                .findActiveDeliveryShipmentsByEmployee(employee.getId());

        Optional<Shipment> activeShipmentOpt = activeShipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT
                        && s.getType() == com.logistics.enums.ShipmentType.DELIVERY)
                .findFirst();

        // Nếu không có IN_TRANSIT, thử tìm PENDING (shipper đã có chuyến nhưng chưa bắt đầu)
        if (activeShipmentOpt.isEmpty()) {
            activeShipmentOpt = activeShipments.stream()
                    .filter(sh -> sh.getStatus() == ShipmentStatus.PENDING
                            && sh.getType() == com.logistics.enums.ShipmentType.DELIVERY)
                    .findFirst();
        }

        if (activeShipmentOpt.isEmpty()) {
            // Không có shipment IN_TRANSIT/PENDING → tạo chuyến mới cho shipper rồi thêm đơn pickup vào.
            Shipment shipment = new Shipment();
            shipment.setStatus(ShipmentStatus.PENDING);
            shipment.setType(ShipmentType.DELIVERY);
            shipment.setEmployee(employee);
            shipment.setFromOffice(order.getFromOffice());
            shipment.setToOffice(order.getToOffice());
            shipmentRepository.save(shipment);

            try {
                InsertPickupShipmentRequest body = new InsertPickupShipmentRequest();
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
                resp.put("message", "Đã đăng ký nhận đơn pickup (chờ gom vào chuyến)");
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

        // Có shipment IN_TRANSIT → auto-add pickup vào shipment đó
        try {
            InsertPickupShipmentRequest body = new InsertPickupShipmentRequest();
            body.setPickupOrderId(orderId);
            Map<String, Object> insertResult = orderShipperService.insertPickupIntoShipment(
                    activeShipment.getId(), body);

            // Sau khi insert, ensure Order.status = PICKING_UP (insertPickupIntoShipment đã làm,
            // nhưng set lại để chắc chắn idempotent với mọi luồng).
            if (order.getStatus() != OrderStatus.PICKING_UP) {
                order.setStatus(OrderStatus.PICKING_UP);
                orderRepository.save(order);
            }

            // Ghi history gắn với shipment đang chạy
            try {
                saveHistory(order, activeShipment, OrderHistoryActionType.PICKING_UP,
                        "Shipper nhận đơn pickup và tự động thêm vào chuyến đang chạy "
                                + activeShipment.getCode());
            } catch (Exception he) {
                log.warn("Failed to save auto-add history: {}", he.getMessage());
            }

            // Override message cho ngữ cảnh accept-pickup, giữ nguyên requiresReoptimize từ insertResult
            insertResult.put("success", true);
            insertResult.put("message",
                    "Đã thêm đơn pickup vào chuyến đang chạy. Vui lòng tối ưu lại tuyến.");

            return insertResult;

        } catch (Exception ex) {
            // Auto-add fail (vd: validation, conflict shipment) — vẫn giữ order ở PICKING_UP vì shipper đã cam kết đi lấy.
            // KHÔNG rethrow — tránh 500 cho endpoint accept pickup.
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
            resp.put("message", "Đã đăng ký nhận đơn pickup (chờ gom vào chuyến)");
            resp.put("orderId", orderId);
            resp.put("shipmentId", null);
            resp.put("requiresReoptimize", false);
            resp.put("reason", "auto_add_failed");
            resp.put("errorClass", ex.getClass().getSimpleName());
            resp.put("errorMessage", ex.getMessage());
            return resp;
        }
    }

    /**
     * Shipper start pickup: set PICKING_UP. Yêu cầu đơn thuộc shipment IN_TRANSIT.
     */
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
        // KHÔNG cộng vehicle load ở đây — chỉ cộng khi shipper lấy hàng thật (PICKED_UP).
    }

    /**
     * Shipper markPickedUp.
     * Theo flow mới:
     *  - Delivery order: READY_FOR_PICKUP -> PICKED_UP (sau khi quét QR tại bưu cục đích)
     *  - Pickup order: PICKING_UP / PICKUP_RETRY -> PICKED_UP (sau khi lấy tại nhà người gửi)
     * Yêu cầu: đơn thuộc shipment IN_TRANSIT.
     */
    @Transactional
    public void markPickedUp(Integer orderId, PickedUpRequest req) {
        Employee employee = getCurrentEmployee();
        Shipment shipment = requireActiveDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getEmployee() == null || !Objects.equals(order.getEmployee().getId(), employee.getId())) {
            throw new AppException(OrderErrorCode.ORDER_NOT_ASSIGNED);
        }

        // Idempotent: nếu order đã PICKED_UP → trả về ngay, KHÔNG cộng vehicle lần nữa.
        // Workload đã được áp dụng ở lần gọi trước.
        if (order.getStatus() == OrderStatus.PICKED_UP) {
            return;
        }

        // Xác định stopType của order trong shipment hiện tại để biết đây là PICKUP hay DELIVERY stop.
        // Dùng stopType (entity RouteStopType) thay vì order.pickupType vì:
        //   - pickupType chỉ nói cách lấy hàng GỐC của đơn, không nói stop hiện tại trong shipment là gì.
        //   - stopType mới là nguồn chân lý: đơn có thể vừa là pickup stop (lấy) vừa là delivery stop (giao).
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
            // PICKUP stop: cho phép nhiều status vì order có thể được thêm vào shipment
            // ở các trạng thái khác nhau (READY_FOR_PICKUP, CONFIRMED, URGENT_PICKUP, PICKING_UP, PICKUP_RETRY).
            allowed = EnumSet.of(
                    OrderStatus.PICKING_UP,
                    OrderStatus.PICKUP_RETRY,
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.CONFIRMED,
                    OrderStatus.URGENT_PICKUP
            );
        } else {
            // DELIVERY stop: chỉ từ READY_FOR_PICKUP (sau QR scan tại bưu cục đích)
            allowed = EnumSet.of(OrderStatus.READY_FOR_PICKUP);
        }

        if (!allowed.contains(current)) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.PICKED_UP,
                "Shipper xác nhận đã lấy hàng (chuyến " + shipment.getCode() + ")");
    }

    /**
     * Shipper bắt đầu giao: PICKED_UP -> DELIVERING.
     * Theo flow mới: phải quét QR (PICKED_UP) trước khi bắt đầu giao.
     * Yêu cầu đơn thuộc shipment IN_TRANSIT, status PICKED_UP.
     */
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
        // Theo rule: chỉ PICKED_UP -> DELIVERING (sau khi quét QR)
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.DELIVERING);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.DELIVERING,
                "Shipper bắt đầu giao hàng (chuyến " + shipment.getCode() + ")");
    }

    /**
     * Bulk: chuyển toàn bộ order PICKED_UP trong shipment DELIVERY IN_TRANSIT sang DELIVERING.
     * Dùng khi shipper đã quét QR tất cả đơn tại bưu cục đích và muốn bắt đầu giao hàng loạt.
     *
     * Rules:
     * - Caller phải là shipper được gán cho shipment (hoặc manager/admin override).
     * - shipment.type == DELIVERY
     * - shipment.status == IN_TRANSIT
     * - Chỉ orders PICKED_UP được chuyển; orders khác giữ nguyên.
     */
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
                order.setStatus(OrderStatus.DELIVERING);
                orderRepository.save(order);
                saveHistory(order, shipment, OrderHistoryActionType.DELIVERING,
                        "Shipper bắt đầu giao hàng (bulk, chuyến " + shipment.getCode() + ")");
                updatedOrderIds.add(order.getId());
                updatedCount++;
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

    /**
     * Giao thành công.
     */
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
        checkAndAutoFinish(shipment);
    }

    /**
     * Giao thất bại (sẽ retry). Set DELIVERY_RETRY, giữ employee, giữ trong shipment.
     */
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
    }

    /**
     * Giao thất bại cuối: DELIVERY_FAILED_FINAL -> ngay RETURNING, giữ trong cùng shipment.
     */
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

        // Theo rule đã chốt: giữ cùng shipment, set RETURNING
        order.setStatus(OrderStatus.RETURNING);
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.RETURNING,
                "Tự động chuyển sang hoàn hàng trong cùng chuyến " + shipment.getCode());
    }

    /**
     * Trả giao thất bại về bưu cục đích (giữa chừng retry). Set AT_DEST_OFFICE, giữ shipment.
     */
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
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.AT_DEST_OFFICE,
                "Trả hàng giao thất bại về bưu cục đích (chuyến " + shipment.getCode() + ")");
    }

    /**
     * Bắt đầu return: RETURN_RETRY -> RETURNING. Giữ nguyên nếu đã RETURNING.
     */
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

    /**
     * Xác nhận đã về bưu cục gốc khi hoàn.
     */
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
    }

    /**
     * Nộp hàng về bưu cục gốc (sau pickup tại nhà).
     */
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
        // OrderHistoryActionType không có AT_ORIGIN_OFFICE -> dùng AT_DEST_OFFICE làm action chung cho cả 2
        saveHistory(order, shipment, OrderHistoryActionType.AT_DEST_OFFICE,
                "Nộp hàng về bưu cục gốc (chuyến " + shipment.getCode() + ")");
        checkAndAutoFinish(shipment);
    }

    /**
     * Manager xác nhận đơn RETURN_AT_ORIGIN_OFFICE đã trả cho người gửi.
     */
    @Transactional
    public void finalizeReturn(Integer orderId) {
        boolean isManager = SecurityUtils.hasRole("manager") || SecurityUtils.hasRole("admin");
        if (!isManager) {
            throw new AppException(ShipmentErrorCode.SHIPMENT_ACCESS_DENIED);
        }
        Shipment shipment = loadDeliveryShipmentForOrder(orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.RETURN_AT_ORIGIN_OFFICE) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(LocalDateTime.now());
        orderRepository.save(order);
        saveHistory(order, shipment, OrderHistoryActionType.RETURNED,
                "Hoàn hàng thành công cho người gửi (chuyến " + shipment.getCode() + ")");
        checkAndAutoFinish(shipment);
    }

    private Shipment loadDeliveryShipmentForOrder(Integer orderId) {
        List<Shipment> active = shipmentOrderRepository.findActiveShipmentsForOrder(orderId);
        Shipment shipment = active.stream()
                .filter(s -> s.getType() == ShipmentType.DELIVERY)
                .findFirst()
                .orElseThrow(() -> new AppException(ShipmentErrorCode.SHIPMENT_NOT_ACTIVE_FOR_ORDER));
        return shipment;
    }

    // ==================== Queries ====================

    public List<ShipperActiveShipmentDto> listActiveShipmentsForCurrentShipper() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();

        // Tìm Employee bằng nhiều cách để đảm bảo tìm đúng
        List<Employee> employeesByUserId = employeeRepository.findByUserId(userId);

        Employee employee = null;
        if (employeesByUserId != null && !employeesByUserId.isEmpty()) {
            // Lọc lấy Employee có role Shipper
            for (Employee emp : employeesByUserId) {
                if (emp.getAccountRole() != null && emp.getAccountRole().getRole() != null
                        && "Shipper".equalsIgnoreCase(emp.getAccountRole().getRole().getName())) {
                    employee = emp;
                    break;
                }
            }
            // Nếu không tìm được Shipper, lấy Employee đầu tiên
            if (employee == null) {
                employee = employeesByUserId.get(0);
            }
        }

        if (employee == null) {
            return List.of();
        }

        List<Shipment> shipments = shipmentRepository.findActiveDeliveryShipmentsByEmployee(employee.getId());

        // Map sang DTO để tránh circular reference
        return shipments.stream()
                .map(this::toShipperActiveShipmentDto)
                .toList();
    }

    /**
     * Convert Shipment entity sang DTO gọn, tránh circular reference
     */
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

        // Vehicle info
        if (s.getVehicle() != null) {
            builder.vehicle(ShipperActiveShipmentDto.VehicleInfo.builder()
                    .id(s.getVehicle().getId())
                    .licensePlate(s.getVehicle().getLicensePlate())
                    .type(s.getVehicle().getType() != null ? s.getVehicle().getType().name() : null)
                    .build());
        }

        // Employee info - chỉ lấy basic info, không lấy user/account
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

        // From office info
        if (s.getFromOffice() != null) {
            builder.fromOffice(ShipperActiveShipmentDto.OfficeInfo.builder()
                    .id(s.getFromOffice().getId())
                    .name(s.getFromOffice().getName())
                    .code(s.getFromOffice().getCode())
                    .build());
        }

        // To office info
        if (s.getToOffice() != null) {
            builder.toOffice(ShipperActiveShipmentDto.OfficeInfo.builder()
                    .id(s.getToOffice().getId())
                    .name(s.getToOffice().getName())
                    .code(s.getToOffice().getCode())
                    .build());
        }

        // Order count
        if (s.getShipmentOrders() != null) {
            builder.orderCount(s.getShipmentOrders().size());
        }

        return builder.build();
    }
}
