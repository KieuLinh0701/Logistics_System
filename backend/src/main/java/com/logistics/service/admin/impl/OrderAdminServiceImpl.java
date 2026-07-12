package com.logistics.service.admin.impl;

import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.OrderProduct;
import com.logistics.enums.OrderStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.mapper.OrderMapper;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PickupAttemptRepository;
import com.logistics.repository.DeliveryAttemptRepository;
import com.logistics.repository.ShipmentOrderRepository;
import com.logistics.response.Pagination;
import com.logistics.service.admin.OrderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderProductRepository orderProductRepository;
    private final PickupAttemptRepository pickupAttemptRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;

    @Override
    public Map<String, Object> listOrders(int page, int limit, String search, String status) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Order> orderPage;

        if (status != null && !status.trim().isEmpty()) {
            orderPage = orderRepository.findByStatus(OrderStatus.valueOf(status), pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        List<Map<String, Object>> orders = orderPage.getContent().stream()
                .map(this::mapOrder)
                .collect(Collectors.toList());

        Pagination pagination = new Pagination(
                (int) orderPage.getTotalElements(),
                page,
                limit,
                orderPage.getTotalPages());

        Map<String, Object> result = new HashMap<>();
        result.put("data", orders);
        result.put("pagination", pagination);

        return result;
    }

    @Override
    public ManagerOrderDetailDto getOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        List<OrderHistory> orderHistories = orderHistoryRepository.findByOrderIdOrderByActionTimeDesc(order.getId());
        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        var pickupAttempts = pickupAttemptRepository.findByOrderIdOrderByAttemptedAtDesc(order.getId());
        var deliveryAttempts = deliveryAttemptRepository.findByOrderIdOrderByAttemptNumberDesc(order.getId());

        return OrderMapper.toManagerOrderDetailDto(order, orderHistories, orderProducts, pickupAttempts, deliveryAttempts,
                shipmentOrderRepository);
    }

    @Override
    @Transactional
    public void deleteOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        orderRepository.delete(order);
    }

    private Map<String, Object> mapOrder(Order order) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("id", order.getId());
        orderMap.put("trackingNumber", order.getTrackingNumber());
        orderMap.put("senderName", order.getSenderName());
        orderMap.put("recipientName", order.getRecipientName());
        orderMap.put("status", order.getStatus().name());
        orderMap.put("totalFee", order.getTotalFee());
        orderMap.put("createdAt", order.getCreatedAt());
        return orderMap;
    }
}
