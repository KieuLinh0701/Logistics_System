package com.logistics.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.entity.Order;
import com.logistics.enums.OrderStatus;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.PickupAttemptRepository;
import com.logistics.mapper.OrderMapper;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.OrderProduct;

@Service
public class OrderAdminService {

    @Autowired
    private OrderRepository orderRepository;


    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private PickupAttemptRepository pickupAttemptRepository;

    public ApiResponse<Map<String, Object>> listOrders(int page, int limit, String search, String status) {
        try {
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

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ManagerOrderDetailDto> getOrderById(Integer orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            List<OrderHistory> orderHistories = orderHistoryRepository.findByOrderIdOrderByActionTimeDesc(order.getId());
            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
            var pickupAttempts = pickupAttemptRepository.findByOrderIdOrderByAttemptedAtDesc(order.getId());

            ManagerOrderDetailDto dto = OrderMapper.toManagerOrderDetailDto(order, orderHistories, orderProducts, pickupAttempts);

            return new ApiResponse<>(true, "Lấy chi tiết đơn hàng thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteOrder(Integer orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            orderRepository.delete(order);
            return new ApiResponse<>(true, "Xóa đơn hàng thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
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


