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
    private com.logistics.service.common.OfficePublicService officePublicService;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private com.logistics.service.assignment.AutoAssignService autoAssignService;

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

            // Khi admin chuyển trạng thái sang CONFIRMED -> tự động gán fromOffice và toOffice
            String newStatus = request.getStatus();
            if (newStatus != null && newStatus.equalsIgnoreCase("CONFIRMED")) {
                // gán fromOffice dựa trên địa chỉ người gửi (ưu tiên ward, fallback city)
                try {
                    Integer senderCity = order.getSenderCityCode();
                    Integer senderWard = order.getSenderWardCode();
                    if (senderCity != null) {
                        com.logistics.request.common.office.PublicOfficeSearchRequest psr =
                                new com.logistics.request.common.office.PublicOfficeSearchRequest(senderCity, senderWard, null, null, null);
                        com.logistics.response.ApiResponse<java.util.List<com.logistics.dto.common.PublicOfficeInformationDto>> offRes =
                                officePublicService.listLocalOffices(psr);
                        if (offRes != null && offRes.isSuccess() && offRes.getData() != null && !offRes.getData().isEmpty()) {
                            Integer ofId = offRes.getData().get(0).getId();
                            officeRepository.findById(ofId).ifPresent(order::setFromOffice);
                        }
                    }
                } catch (Exception ignored) {}

                // gán toOffice dựa trên địa chỉ người nhận (ưu tiên ward, fallback city)
                try {
                    Integer recipCity = null;
                    Integer recipWard = null;
                    if (order.getRecipientAddress() != null) {
                        recipCity = order.getRecipientAddress().getCityCode();
                        recipWard = order.getRecipientAddress().getWardCode();
                    }

                    if (recipCity != null) {
                        com.logistics.request.common.office.PublicOfficeSearchRequest psrTo =
                                new com.logistics.request.common.office.PublicOfficeSearchRequest(recipCity, recipWard, null, null, null);
                        com.logistics.response.ApiResponse<java.util.List<com.logistics.dto.common.PublicOfficeInformationDto>> offResTo =
                                officePublicService.listLocalOffices(psrTo);
                        if (offResTo != null && offResTo.isSuccess() && offResTo.getData() != null && !offResTo.getData().isEmpty()) {
                            Integer toId = offResTo.getData().get(0).getId();
                            officeRepository.findById(toId).ifPresent(order::setToOffice);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Giữ trạng thái CONFIRMED do admin xác nhận
            order.setStatus(OrderStatus.valueOf(request.getStatus()));
            order = orderRepository.save(order);

            // Gán shipper cho yêu cầu lấy hàng; nếu không có thì để nguyên không gán
            if (newStatus != null && newStatus.equalsIgnoreCase("CONFIRMED")
                    && order.getPickupType() != null
                    && order.getPickupType() == OrderPickupType.PICKUP_BY_COURIER) {
                try {
                    autoAssignService.autoAssignPickupRequest(order.getId());
                } catch (Exception ignored) {}
            }

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


