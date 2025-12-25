package com.logistics.service.shipper;

import com.logistics.entity.Order;
import com.logistics.entity.ShipperAssignment;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.User;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.enums.ShippingRequestType;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ShipperAssignmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.repository.UserRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.entity.Employee;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.NotificationService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShippingRequestShipperService {

    @Autowired
    private ShippingRequestRepository shippingRequestRepo;

    @Autowired
    private ShipperAssignmentRepository shipperAssignmentRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    public List<ShippingRequest> listForCurrentShipper() {
        // Trả về tất cả yêu cầu nhắc lấy hàng đang chờ để shipper có thể tự nhận
        List<ShippingRequest> all = shippingRequestRepo.findAll();
        if (all != null) {
            for (ShippingRequest r : all) {
                String rt = r.getRequestType() == null ? "null" : r.getRequestType().name();
                String st = r.getStatus() == null ? "null" : r.getStatus().name();
                Integer oid = r.getOrder() == null ? null : r.getOrder().getId();
                String ostatus = "null";
                try { if (r.getOrder() != null && r.getOrder().getStatus() != null) ostatus = r.getOrder().getStatus().name(); } catch (Exception ignored) {}
            }
        }

        List<ShippingRequest> filtered = all.stream()
            .filter(r -> r.getRequestType() == ShippingRequestType.PICKUP_REMINDER)
            .filter(r -> r.getStatus() == ShippingRequestStatus.PENDING)
            .filter(r -> r.getOrder() != null)
            .toList();
        return filtered;
    }

    @Transactional
    public ApiResponse<Boolean> accept(Integer requestId) {
        Integer shipperUserId = SecurityUtils.getAuthenticatedUserId();

        ShippingRequest req = shippingRequestRepo.findByIdWithOrder(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu không tồn tại"));

        if (req.getRequestType() != ShippingRequestType.PICKUP_REMINDER) return new ApiResponse<>(false, "Không phải yêu cầu lấy hàng", false);
        if (req.getStatus() != ShippingRequestStatus.PENDING) return new ApiResponse<>(false, "Yêu cầu đã được xử lý", false);

        Order order = req.getOrder();
        if (order == null) return new ApiResponse<>(false, "Order liên quan không tồn tại", false);

        // Cho phép shipper nhận yêu cầu nhắc lấy mà không cần kiểm tra phân công shipper trước đó

        User shipper = userRepository.findById(shipperUserId).orElseThrow(() -> new RuntimeException("Shipper not found"));

        req.setHandler(shipper);
        req.setStatus(ShippingRequestStatus.PROCESSING);
        shippingRequestRepo.save(req);

        // Ánh xạ shipper (User) sang Employee (ưu tiên Employee cùng bưu cục)
        List<Employee> empList = employeeRepository.findByUserId(shipperUserId);
        if (empList != null && !empList.isEmpty()) {
            Employee selected = null;
            if (order.getToOffice() != null) {
                Integer toOfficeId = order.getToOffice().getId();
                selected = empList.stream()
                        .filter(e -> e.getOffice() != null && e.getOffice().getId().equals(toOfficeId))
                        .findFirst().orElse(null);
            }
            if (selected == null) selected = empList.get(0);
            order.setEmployee(selected);
        }

        order.setStatus(com.logistics.enums.OrderStatus.PICKING_UP);
        orderRepository.save(order);

        notificationService.create(
                "Đã nhận yêu cầu",
                "Bạn đã nhận yêu cầu " + req.getId(),
                "shipping_request_accepted",
                shipperUserId,
                null,
                "order",
                order.getTrackingNumber()
        );

        // Thông báo cho người yêu cầu (user) nếu có
        if (req.getUser() != null && req.getUser().getId() != null) {
            notificationService.create(
                "Yêu cầu đã được nhận",
                "Yêu cầu của bạn đã được shipper nhận: " + req.getId(),
                "shipping_request_accepted",
                req.getUser().getId(),
                null,
                "order",
                order.getTrackingNumber()
            );
        }

        return new ApiResponse<>(true, "Đã nhận yêu cầu", true);
    }
}
