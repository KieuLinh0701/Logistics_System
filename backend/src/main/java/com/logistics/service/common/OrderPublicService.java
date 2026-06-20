package com.logistics.service.common;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.logistics.dto.OrderHistoryDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.mapper.OrderHistoryMapper;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderPublicService {

    private final OrderRepository repository;
    private final OrderHistoryRepository historyRepository;

    public List<OrderHistoryDto> getOrderHistoriesByTrackingNumber(
            @PathVariable String trackingNumber) {
        Order order = repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        List<OrderHistory> orderHistories = historyRepository
                .findByOrderIdOrderByActionTimeDesc(order.getId());

        return OrderHistoryMapper.toDtoList(orderHistories);
    }
}