package com.logistics.service.assignment;

import com.logistics.entity.Order;
import com.logistics.entity.ShipperAssignment;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.User;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ShipperAssignmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.repository.UserRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.entity.Employee;
import com.logistics.service.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AutoAssignService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShipperAssignmentRepository shipperAssignmentRepo;

    @Autowired
    private ShippingRequestRepository shippingRequestRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Optional<User> autoAssignOnArrival(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.AT_DEST_OFFICE) return Optional.empty();

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

        List<ShipperAssignment> candidates = List.of();
        if (cityCode != null && wardCode != null) {
            candidates = shipperAssignmentRepo.findActiveByCityAndWard(cityCode, wardCode, now);
        }

        if (candidates == null || candidates.isEmpty()) {
            if (cityCode != null) {
                candidates = shipperAssignmentRepo.findActiveByCity(cityCode, now);
            }
        }

        if (candidates == null || candidates.isEmpty()) {
            notificationService.create(
                    "Auto-assign failed",
                    "Không tìm thấy shipper phù hợp cho đơn " + order.getTrackingNumber(),
                    "assign_failure",
                    null,
                    null,
                    "order",
                    order.getTrackingNumber()
            );
            return Optional.empty();
        }

        // Chọn đơn giản: lấy shipper có ít phân công nhất (theo trường createdAt của ShipperAssignment)
        ShipperAssignment chosenAssignment = candidates.stream()
                .min(Comparator.comparing(ShipperAssignment::getCreatedAt))
                .orElse(candidates.get(0));

        User chosen = chosenAssignment.getShipper();

        // Thử ánh xạ shipper được chọn (User) sang một Employee và đặt orders.employee_id
        List<Employee> empList = employeeRepository.findByUserId(chosen.getId());
        if (empList != null && !empList.isEmpty()) {
            Employee selected = null;
            if (order.getToOffice() != null) {
                Integer toOfficeId = order.getToOffice().getId();
                selected = empList.stream()
                        .filter(e -> e.getOffice() != null && Objects.equals(e.getOffice().getId(), toOfficeId))
                        .findFirst()
                        .orElse(null);
            }
            if (selected == null) selected = empList.get(0);
            order.setEmployee(selected);
        }

        // Tái sử dụng hoặc tạo mới một ShippingRequest loại DELIVERY_REMINDER
        Optional<ShippingRequest> existing = shippingRequestRepo.findDeliveryReminderByOrderId(order.getId());
        ShippingRequest sr = existing.orElseGet(() -> {
            ShippingRequest s = new ShippingRequest();
            s.setOrder(order);
            s.setRequestType(com.logistics.enums.ShippingRequestType.DELIVERY_REMINDER);
            s.setRequestContent("Auto-assign for delivery");
            return s;
        });

        sr.setHandler(chosen);
        sr.setStatus(ShippingRequestStatus.PROCESSING);
        shippingRequestRepo.save(sr);

        // Đánh dấu đơn là READY_FOR_PICKUP để nó xuất hiện trong danh sách 'đơn cần giao' của shipper
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        orderRepository.save(order);

        notificationService.create(
                "Bạn được phân công giao hàng",
                "Bạn được phân công giao đơn " + order.getTrackingNumber(),
                "assignment",
                chosen.getId(),
                null,
                "order",
                order.getTrackingNumber()
        );

        // Thông báo cho chủ đơn nếu có
        if (order.getUser() != null && order.getUser().getId() != null) {
            notificationService.create(
                "Đã phân công shipper",
                "Đơn của bạn đã được phân công shipper: " + chosen.getFullName(),
                "assignment",
                order.getUser().getId(),
                null,
                "order",
                order.getTrackingNumber()
            );
        }

        return Optional.of(chosen);
    }

    @Transactional
    public Optional<User> autoAssignPickupRequest(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getPickupType() != com.logistics.enums.OrderPickupType.PICKUP_BY_COURIER) return Optional.empty();
        // Cho phép khi admin vừa xác nhận (CONFIRMED), đang ở trạng thái PENDING, hoặc đã được đánh dấu READY_FOR_PICKUP
        if (order.getStatus() != com.logistics.enums.OrderStatus.CONFIRMED
            && order.getStatus() != com.logistics.enums.OrderStatus.PENDING
            && order.getStatus() != com.logistics.enums.OrderStatus.READY_FOR_PICKUP) return Optional.empty();

        Integer cityCode = order.getSenderCityCode();
        Integer wardCode = order.getSenderWardCode();
        if ((cityCode == null || wardCode == null) && order.getFromOffice() != null) {
            cityCode = order.getFromOffice().getCityCode();
            wardCode = order.getFromOffice().getWardCode();
        }

        List<ShipperAssignment> candidates = List.of();
        if (cityCode != null && wardCode != null) {
            candidates = shipperAssignmentRepo.findActiveByCityAndWard(cityCode, wardCode, LocalDateTime.now());
        }

        if (candidates == null || candidates.isEmpty()) {
            if (cityCode != null) {
                candidates = shipperAssignmentRepo.findActiveByCity(cityCode, LocalDateTime.now());
            }
        }

        if (candidates == null || candidates.isEmpty()) {
            // không có shipper gần đó -> để nguyên chưa gán để shipper trong bưu cục có thể nhận
            return Optional.empty();
        }

        ShipperAssignment chosenAssignment = candidates.stream()
                .min(Comparator.comparing(ShipperAssignment::getCreatedAt))
                .orElse(candidates.get(0));

        User chosen = chosenAssignment.getShipper();

        List<Employee> empList = employeeRepository.findByUserId(chosen.getId());
        if (empList != null && !empList.isEmpty()) {
            Employee selected = null;
            if (order.getFromOffice() != null) {
                Integer fromOfficeId = order.getFromOffice().getId();
                selected = empList.stream()
                        .filter(e -> e.getOffice() != null && Objects.equals(e.getOffice().getId(), fromOfficeId))
                        .findFirst()
                        .orElse(null);
            }
            if (selected == null) selected = empList.get(0);
            order.setEmployee(selected);
        }

        Optional<ShippingRequest> existing = shippingRequestRepo.findDeliveryReminderByOrderId(order.getId());
        ShippingRequest sr = existing.orElseGet(() -> {
            ShippingRequest s = new ShippingRequest();
            s.setOrder(order);
            s.setRequestType(com.logistics.enums.ShippingRequestType.PICKUP_REMINDER);
            s.setRequestContent("Auto-assign for pickup");
            return s;
        });

        sr.setHandler(chosen);
        sr.setStatus(com.logistics.enums.ShippingRequestStatus.PROCESSING);
        shippingRequestRepo.save(sr);

        // Lưu employee/handler nhưng KHÔNG thay đổi trạng thái đơn ở đây (admin giữ CONFIRMED)
        orderRepository.save(order);

        notificationService.create(
                "Bạn được phân công lấy hàng",
                "Bạn được phân công lấy đơn " + order.getTrackingNumber(),
                "assignment",
                chosen.getId(),
                null,
                "order",
                order.getTrackingNumber()
        );

        if (order.getUser() != null && order.getUser().getId() != null) {
            notificationService.create(
                    "Đã phân công shipper",
                    "Đơn của bạn đã được phân công shipper: " + chosen.getFullName(),
                    "assignment",
                    order.getUser().getId(),
                    null,
                    "order",
                    order.getTrackingNumber()
            );
        }

        return Optional.of(chosen);
    }
}
