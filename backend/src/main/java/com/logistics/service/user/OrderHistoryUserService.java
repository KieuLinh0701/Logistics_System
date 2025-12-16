package com.logistics.service.user;

import org.springframework.stereotype.Service;

import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.Shipment;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.repository.OrderHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderHistoryUserService {

    private final OrderHistoryRepository repository;

    public void save(Order order, Office fromOffice, 
                      Office toOffice, Shipment shipment, 
                      OrderHistoryActionType action, String note) {

        OrderHistory orderHistory = OrderHistory.builder()
                .order(order)
                .fromOffice(fromOffice)
                .toOffice(toOffice)
                .shipment(shipment)
                .action(action)
                .note(note)
                .build();
                
        if (orderHistory == null) return;

        repository.save(orderHistory);
    }
}