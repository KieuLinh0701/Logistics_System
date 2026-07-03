package com.logistics.service.user.impl;

import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.Shipment;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.service.user.OrderHistoryUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderHistoryUserServiceImpl implements OrderHistoryUserService {

    private final OrderHistoryRepository repository;

    @Override
    @Transactional
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