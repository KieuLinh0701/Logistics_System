package com.logistics.service.user.impl;

import com.logistics.entity.*;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.OrderPickupType;
import com.logistics.enums.RouteStopType;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.ShipmentOrderRepository;
import com.logistics.service.user.OrderHistoryUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderHistoryUserServiceImpl implements OrderHistoryUserService {

    private final OrderHistoryRepository repository;
    private final ShipmentOrderRepository shipmentOrderRepository;

    @Override
    @Transactional
    public void save(Order order, Office fromOffice,
                      Office toOffice, Shipment shipment,
                      OrderHistoryActionType action, String note) {

        OrderPickupType pickupTypeSnapshot = order.getPickupType();
        RouteStopType stopTypeSnapshot = null;
        if (shipment != null && order.getId() != null) {
            List<ShipmentOrder> shipOrders =
                    shipmentOrderRepository.findByShipmentId(shipment.getId());
            if (shipOrders != null) {
                for (ShipmentOrder so : shipOrders) {
                    if (so.getOrder() != null
                            && so.getOrder().getId().equals(order.getId())
                            && so.getStopType() != null) {
                        stopTypeSnapshot = so.getStopType();
                        break;
                    }
                }
            }
        }

        OrderHistory orderHistory = OrderHistory.builder()
                .order(order)
                .fromOffice(fromOffice)
                .toOffice(toOffice)
                .shipment(shipment)
                .action(action)
                .note(note)
                .pickupTypeSnapshot(pickupTypeSnapshot)
                .stopTypeSnapshot(stopTypeSnapshot)
                .build();

        repository.save(orderHistory);
    }
}