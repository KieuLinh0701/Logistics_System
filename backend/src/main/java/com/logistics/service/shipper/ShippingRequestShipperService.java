package com.logistics.service.shipper;

import com.logistics.entity.Employee;
import com.logistics.entity.Order;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.User;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.enums.ShippingRequestType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.ShippingRequestErrorCode;
import com.logistics.repository.*;
import com.logistics.service.common.NotificationService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        List<ShippingRequest> all = shippingRequestRepo.findAll();
        List<ShippingRequest> filtered = all.stream()
            .filter(r -> r.getRequestType() == ShippingRequestType.PICKUP_REMINDER)
            .filter(r -> r.getStatus() == ShippingRequestStatus.PENDING)
            .filter(r -> r.getOrder() != null)
            .toList();
        return filtered;
    }

    @Transactional
    public void accept(Integer requestId) {
        Integer shipperUserId = SecurityUtils.getAuthenticatedUserId();

        ShippingRequest req = shippingRequestRepo.findByIdWithOrder(requestId)
                .orElseThrow(() -> new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_NOT_FOUND));

        if (req.getRequestType() != ShippingRequestType.PICKUP_REMINDER) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_INVALID_TYPE);
        }
        if (req.getStatus() != ShippingRequestStatus.PENDING) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_ALREADY_PROCESSED);
        }

        Order order = req.getOrder();
        if (order == null) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_ORDER_NOT_FOUND);
        }

        User shipper = userRepository.findById(shipperUserId)
                .orElseThrow(() -> new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_SHIPPER_NOT_FOUND));

        req.setHandler(shipper);
        req.setStatus(ShippingRequestStatus.PROCESSING);
        shippingRequestRepo.save(req);

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
                "recipientaddress",
                order.getTrackingNumber()
        );

        if (req.getUser() != null && req.getUser().getId() != null) {
            notificationService.create(
                "Yêu cầu đã được nhận",
                "Yêu cầu của bạn đã được shipper nhận: " + req.getId(),
                "shipping_request_accepted",
                req.getUser().getId(),
                null,
                "recipientaddress",
                order.getTrackingNumber()
            );
        }
    }
}
