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
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        LocalDateTime now = LocalDateTime.now();
        List<ShipperAssignment> assigns = shipperAssignmentRepo.findActiveByShipperId(userId, now);

        if (assigns == null || assigns.isEmpty()) return List.of();

        return shippingRequestRepo.findAll().stream()
            .filter(r -> r.getRequestType() == ShippingRequestType.PICKUP_REMINDER)
            .filter(r -> r.getStatus() == ShippingRequestStatus.PENDING)
            .filter(r -> {
                Order o = r.getOrder();
                if (o == null) return false;

                Integer oCity = null;
                Integer oWard = null;
                if (o.getRecipientAddress() != null) {
                    oCity = o.getRecipientAddress().getCityCode();
                    oWard = o.getRecipientAddress().getWardCode();
                } else if (o.getToOffice() != null) {
                    oCity = o.getToOffice().getCityCode();
                    oWard = o.getToOffice().getWardCode();
                }

                if (oCity == null || oWard == null) return false;

                final Integer finalCity = oCity;
                final Integer finalWard = oWard;

                return assigns.stream().anyMatch(a ->
                    a.getCityCode().equals(finalCity) && a.getWardCode().equals(finalWard)
                );
            })
            .toList();
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

        // Kiểm tra phân công shipper
        LocalDateTime now = LocalDateTime.now();
        Integer cityCode = null;
        Integer wardCode = null;
        if (order.getRecipientAddress() != null) {
            cityCode = order.getRecipientAddress().getCityCode();
            wardCode = order.getRecipientAddress().getWardCode();
        } else if (order.getToOffice() != null) {
            cityCode = order.getToOffice().getCityCode();
            wardCode = order.getToOffice().getWardCode();
        }

        if (cityCode == null || wardCode == null) return new ApiResponse<>(false, "Không có thông tin khu vực của đơn", false);

        List<ShipperAssignment> candidates = shipperAssignmentRepo.findActiveByCityAndWard(cityCode, wardCode, now);
        boolean allowed = candidates.stream().anyMatch(a -> a.getShipper().getId().equals(shipperUserId));
        if (!allowed) return new ApiResponse<>(false, "Bạn không có phân công cho khu vực này", false);

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
