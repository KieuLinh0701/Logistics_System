package com.logistics.service.common.impl;

import com.logistics.dto.OrderHistoryDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.mapper.OrderHistoryMapper;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ShipmentOrderRepository;
import com.logistics.service.common.OrderPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderPublicServiceImpl implements OrderPublicService {

    private final OrderRepository repository;
    private final OrderHistoryRepository historyRepository;
    private final ShipmentOrderRepository shipmentOrderRepository;

    @Override
    public List<OrderHistoryDto> getOrderHistoriesByTrackingNumber(
            @PathVariable String trackingNumber) {
        Order order = repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        List<OrderHistory> orderHistories = historyRepository
                .findByOrderIdOrderByActionTimeDesc(order.getId());

        // history cũ (chưa có stop_type_snapshot).
        return OrderHistoryMapper.toDtoList(orderHistories, shipmentOrderRepository);
    }
}