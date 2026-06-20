package com.logistics.service.shipper;

import com.logistics.entity.Order;
import com.logistics.entity.PickupAttempt;
import com.logistics.entity.User;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.PickupAttemptStatus;
import com.logistics.enums.PickupFailReason;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PickupAttemptRepository;
import com.logistics.repository.UserRepository;
import com.logistics.service.common.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PickupAttemptService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PickupAttemptRepository pickupAttemptRepository;
    private final ConfigService configService;
    private final OrderShipperService orderShipperService;

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
            order.setStatus(OrderStatus.PICKUP_SUCCESS);
        } else {
            long failedCount = pickupAttemptRepository.countByOrderIdAndStatus(orderId, PickupAttemptStatus.FAILED);
            int maxAttempts = configService.getInt("MAX_PICKUP_ATTEMPTS");
            if (failedCount >= maxAttempts) {
                order.setStatus(OrderStatus.PICKUP_FAILED_FINAL);
            } else {
                order.setStatus(OrderStatus.PICKUP_RETRY);
            }
        }

        orderRepository.save(order);

        return orderShipperService.buildOrderDetail(order);
    }
}
