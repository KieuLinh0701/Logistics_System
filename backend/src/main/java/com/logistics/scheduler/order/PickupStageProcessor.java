package com.logistics.scheduler.order;

import com.logistics.entity.Order;
import com.logistics.entity.User;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.PickupNotificationStage;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.UserRepository;
import com.logistics.service.common.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PickupStageProcessor {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processStage1(LocalDateTime now, int delayMinutes) {
        List<Order> orders = orderRepository.findReadyOrdersForNotification(
                OrderStatus.READY_FOR_PICKUP,
                PickupNotificationStage.NONE,
                now.minusMinutes(delayMinutes)
        );

        for (Order order : orders) {
            try {
                List<User> wardShippers = userRepository.findActiveShippersByWard(
                        order.getSenderCityCode(), order.getSenderWardCode(), now);

                List<User> cityShippers = userRepository.findActiveShippersByCity(
                        order.getSenderCityCode(), now);

                LinkedHashSet<User> merged = new LinkedHashSet<>(wardShippers);
                merged.addAll(cityShippers);
                List<User> shippers = new ArrayList<>(merged);

                if (!shippers.isEmpty()) {
                    pushPickupNotification(order, shippers, 1);
                    log.info("[Pickup Stage 1] Order #{} → pinged {} shippers ({} ward-match, city {})",
                            order.getId(), shippers.size(), wardShippers.size(), order.getSenderCityCode());
                }

                order.setPickupNotificationStage(PickupNotificationStage.STAGE_1);
                orderRepository.save(order);

            } catch (Exception e) {
                log.error("[Pickup Stage 1] Error processing order #{}: {}", order.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processStage2(LocalDateTime now, int delayMinutes) {
        List<Order> orders = orderRepository.findReadyOrdersForNotification(
                OrderStatus.READY_FOR_PICKUP,
                PickupNotificationStage.STAGE_1,
                now.minusMinutes(delayMinutes)
        );

        for (Order order : orders) {
            try {
                List<User> shippers = userRepository.findActiveShippersByCity(
                        order.getSenderCityCode(), now);

                if (!shippers.isEmpty()) {
                    pushPickupNotification(order, shippers, 2);
                    log.info("[Pickup Stage 2] Order #{} → pinged {} shippers city-wide (city {})",
                            order.getId(), shippers.size(), order.getSenderCityCode());
                }

                order.setPickupNotificationStage(PickupNotificationStage.STAGE_2);
                orderRepository.save(order);

            } catch (Exception e) {
                log.error("[Pickup Stage 2] Error processing order #{}: {}", order.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processUrgent(LocalDateTime now, int delayMinutes) {
        List<Order> orders = orderRepository.findReadyOrdersForNotification(
                OrderStatus.READY_FOR_PICKUP,
                PickupNotificationStage.STAGE_2,
                now.minusMinutes(delayMinutes)
        );

        for (Order order : orders) {
            try {
                order.setStatus(OrderStatus.URGENT_PICKUP);
                order.setPickupNotificationStage(PickupNotificationStage.URGENT);
                orderRepository.save(order);

                log.warn("[Pickup URGENT] Order #{} → moved to URGENT_PICKUP after {}min unassigned",
                        order.getId(), delayMinutes);

            } catch (Exception e) {
                log.error("[Pickup URGENT] Error processing order #{}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private void pushPickupNotification(Order order, List<User> shippers, int stage) {
        String title = stage == 1
                ? "Có đơn hàng cần lấy gần bạn"
                : "Đơn hàng cần lấy trong khu vực của bạn";

        String message = String.format(
                "Đơn #%s cần lấy tại %s, %s",
                order.getTrackingNumber(),
                order.getSenderWardName(),
                order.getSenderCityName()
        );

        for (User shipper : shippers) {
            try {
                notificationService.create(
                        title,
                        message,
                        "PICKUP_REQUEST",
                        shipper.getId(),
                        null,
                        "Order",
                        order.getId().toString()
                );
            } catch (Exception e) {
                log.warn("[Pickup Notify] Failed to notify shipper #{}: {}", shipper.getId(), e.getMessage());
            }
        }
    }
}