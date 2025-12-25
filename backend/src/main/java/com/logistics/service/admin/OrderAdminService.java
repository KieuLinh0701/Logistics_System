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

import com.logistics.request.admin.UpdateOrderStatusRequest;
import com.logistics.entity.Office;
import com.logistics.repository.OfficeRepository;
import com.logistics.enums.OrderPickupType;
import com.logistics.entity.Order;
import com.logistics.enums.OrderStatus;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderProductRepository;
import com.logistics.mapper.OrderMapper;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;

@Service
public class OrderAdminService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

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

            List<com.logistics.entity.OrderHistory> orderHistories = orderHistoryRepository.findByOrderIdOrderByActionTimeDesc(order.getId());
            List<com.logistics.entity.OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());

            ManagerOrderDetailDto dto = OrderMapper.toManagerOrderDetailDto(order, orderHistories, orderProducts);

            return new ApiResponse<>(true, "Lấy chi tiết đơn hàng thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateOrderStatus(Integer orderId, UpdateOrderStatusRequest request) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            // Nếu admin xác nhận đơn Lấy hàng tại nhà (PICKUP_BY_COURIER),
            // thì yêu cầu phải chọn `fromOfficeId` và gán bưu cục lấy vào đơn
            String newStatus = request.getStatus();
            if (newStatus != null && newStatus.equalsIgnoreCase("CONFIRMED")
                && order.getPickupType() != null
                && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
                if (request.getFromOfficeId() == null) {
                    return new ApiResponse<>(false, "Vui lòng chọn bưu cục lấy hàng khi xác nhận đơn Lấy hàng tại nhà", null);
                }
                Office of = officeRepository.findById(request.getFromOfficeId())
                    .orElseThrow(() -> new RuntimeException("Bưu cục không tồn tại"));
                order.setFromOffice(of);
            }

            order.setStatus(OrderStatus.valueOf(request.getStatus()));
            order = orderRepository.save(order);

            return new ApiResponse<>(true, "Cập nhật trạng thái đơn hàng thành công", mapOrder(order));
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


