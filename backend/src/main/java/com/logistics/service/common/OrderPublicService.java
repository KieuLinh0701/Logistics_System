package com.logistics.service.common;

import com.logistics.dto.OrderHistoryDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.mapper.OrderHistoryMapper;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPublicService {

    private final OrderRepository repository;
    private final OrderHistoryRepository historyRepository;

    public List<OrderHistoryDto> getOrderHistoriesByTrackingNumber(
            @PathVariable String trackingNumber) {
        Order order = repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        List<OrderHistory> orderHistories = historyRepository
                .findByOrderIdOrderByActionTimeDesc(order.getId());

        return OrderHistoryMapper.toDtoList(orderHistories);
    }
}