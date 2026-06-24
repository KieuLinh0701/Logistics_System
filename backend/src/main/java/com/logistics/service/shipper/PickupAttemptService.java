package com.logistics.service.shipper;

import com.logistics.entity.Order;
import com.logistics.entity.OrderProduct;
import com.logistics.entity.PickupAttempt;
import com.logistics.entity.User;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.PickupAttemptStatus;
import com.logistics.enums.PickupFailReason;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.repository.*;
import com.logistics.service.common.ConfigService;
import com.logistics.service.common.NotificationService;
import com.logistics.service.user.ProductUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PickupAttemptService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PickupAttemptRepository pickupAttemptRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderProductRepository orderProductRepository;
    private final ConfigService configService;
    private final OrderShipperService orderShipperService;
    private final NotificationService notificationService;
    private final ProductUserService productUserService;

    @Transactional
    public Map<String, Object> recordPickupAttempt(Integer orderId,
            Integer shipperId,
            PickupAttemptStatus status,
            PickupFailReason failReason,
            String note) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        Integer nextAttempt = 1;
        var recent = pickupAttemptRepository.findByOrderIdOrderByAttemptedAtDesc(orderId);
        if (recent != null && !recent.isEmpty() && recent.get(0).getAttemptNumber() != null) {
            nextAttempt = recent.get(0).getAttemptNumber() + 1;
        }

        PickupAttempt attempt = new PickupAttempt();
        attempt.setOrder(order);
        attempt.setShipper(shipper);
        attempt.setAttemptNumber(nextAttempt);
        attempt.setStatus(status);
        attempt.setFailReason(status == PickupAttemptStatus.SUCCESS ? null : failReason);
        attempt.setNote(note);
        attempt.setAttemptedAt(LocalDateTime.now());
        pickupAttemptRepository.save(attempt);

        if (status == PickupAttemptStatus.SUCCESS) {
            order.setStatus(OrderStatus.PICKED_UP);
            orderRepository.save(order);

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
        } else {
            long failedCount = pickupAttemptRepository.countByOrderIdAndStatus(orderId, PickupAttemptStatus.FAILED);
            int maxAttempts = configService.getInt("MAX_PICKUP_ATTEMPTS");

            if (failedCount >= maxAttempts) {
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    return orderShipperService.buildOrderDetail(order);
                }

                // Ghi history PICKUP_FAILED_FINAL
                var historyFailed = new com.logistics.entity.OrderHistory();
                historyFailed.setOrder(order);
                historyFailed.setAction(OrderHistoryActionType.PICKUP_FAILED_FINAL);
                historyFailed.setNote("Lấy hàng thất bại quá số lần cho phép");
                orderHistoryRepository.save(historyFailed);

                // Chuyển sang CANCELLED
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);

                List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
                productUserService.restoreStockFromOrder(orderProducts);

                // Ghi history CANCELLED
                var historyCancelled = new com.logistics.entity.OrderHistory();
                historyCancelled.setOrder(order);
                historyCancelled.setAction(OrderHistoryActionType.CANCELLED);
                historyCancelled.setNote("Đơn bị hủy do lấy hàng không thành công");
                orderHistoryRepository.save(historyCancelled);

                if (order.getUser() != null) {
                    notificationService.create(
                            "Đơn hàng đã bị hủy",
                            String.format("Đơn %s đã bị hủy do lấy hàng không thành công sau nhiều lần thử.", order.getTrackingNumber()),
                            "order_status",
                            order.getUser().getId(),
                            null,
                            "orders/tracking",
                            order.getTrackingNumber());
                }
            } else {
                order.setStatus(OrderStatus.PICKUP_RETRY);
                orderRepository.save(order);

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
        }

        return orderShipperService.buildOrderDetail(order);
    }
}
