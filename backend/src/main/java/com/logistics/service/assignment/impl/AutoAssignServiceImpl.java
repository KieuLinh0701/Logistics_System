package com.logistics.service.assignment.impl;

import com.logistics.entity.*;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.repository.*;
import com.logistics.service.assignment.AutoAssignService;
import com.logistics.service.common.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutoAssignServiceImpl implements AutoAssignService {

    private final OrderRepository orderRepository;
    private final ShipperAssignmentRepository shipperAssignmentRepo;
    private final ShippingRequestRepository shippingRequestRepo;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    @Value("${logistics.auto-assign.enabled:true}")
    private boolean autoAssignEnabled;

    @Override
    @Transactional
    public Optional<User> autoAssignOnArrival(Integer orderId) {
        if (!autoAssignEnabled) return Optional.empty();
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

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
                    "recipientaddress",
                    order.getTrackingNumber()
            );
            return Optional.empty();
        }

        final List<ShipperAssignment> finalCandidates = candidates;
        ShipperAssignment chosenAssignment = finalCandidates.stream()
            .min(Comparator.comparing(ShipperAssignment::getCreatedAt))
            .orElseGet(() -> finalCandidates.isEmpty() ? null : finalCandidates.get(0));

        User chosen = chosenAssignment.getShipper();

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

        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        orderRepository.save(order);

        notificationService.create(
                "Bạn được phân công giao hàng",
                "Bạn được phân công giao đơn " + order.getTrackingNumber(),
                "assignment",
                chosen.getId(),
                null,
                "recipientaddress",
                order.getTrackingNumber()
        );

        if (order.getUser() != null && order.getUser().getId() != null) {
            notificationService.create(
                "Đã phân công shipper",
                "Đơn của bạn đã được phân công shipper: " + chosen.getFullName(),
                "assignment",
                order.getUser().getId(),
                null,
                "recipientaddress",
                order.getTrackingNumber()
            );
        }

        return Optional.of(chosen);
    }

    @Override
    @Transactional
    public Optional<User> autoAssignPickupRequest(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getPickupType() != com.logistics.enums.OrderPickupType.PICKUP_BY_COURIER) return Optional.empty();
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
            return Optional.empty();
        }

        final List<ShipperAssignment> finalCandidates2 = candidates;
        ShipperAssignment chosenAssignment = finalCandidates2.stream()
            .min(Comparator.comparing(ShipperAssignment::getCreatedAt))
            .orElseGet(() -> finalCandidates2.isEmpty() ? null : finalCandidates2.get(0));

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

        orderRepository.save(order);

        notificationService.create(
                "Bạn được phân công lấy hàng",
                "Bạn được phân công lấy đơn " + order.getTrackingNumber(),
                "assignment",
                chosen.getId(),
                null,
                "recipientaddress",
                order.getTrackingNumber()
        );

        if (order.getUser() != null && order.getUser().getId() != null) {
            notificationService.create(
                    "Đã phân công shipper",
                    "Đơn của bạn đã được phân công shipper: " + chosen.getFullName(),
                    "assignment",
                    order.getUser().getId(),
                    null,
                    "recipientaddress",
                    order.getTrackingNumber()
            );
        }

        return Optional.of(chosen);
    }
}
