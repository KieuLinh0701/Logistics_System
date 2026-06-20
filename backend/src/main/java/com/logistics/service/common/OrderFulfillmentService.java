package com.logistics.service.common;

import com.logistics.dto.order.OrderFulfillmentSummaryDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderProduct;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;

    public OrderFulfillmentSummaryDto getFulfillmentSummary(Long orderId) {
        Order order = orderRepository.findById(orderId.intValue())
                .orElse(null);

        if (order == null) {
            throw new AppException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        List<OrderProduct> products = orderProductRepository.findByOrderId(order.getId());

        int totalItems = 0;
        int deliveredItems = 0;
        int returnedItems = 0;
        long expectedCod = 0L;
        long collectedCod = 0L;
        long returnedValue = 0L;

        for (OrderProduct product : products) {

            int quantity = product.getQuantity() != null ? product.getQuantity() : 0;
            int deliveredQuantity = product.getDeliveredQuantity() != null ? product.getDeliveredQuantity() : 0;
            int returnedQuantity = product.getReturnedQuantity() != null ? product.getReturnedQuantity() : 0;
            int price = product.getPrice() != null ? product.getPrice() : 0;

            totalItems += quantity;
            deliveredItems += deliveredQuantity;
            returnedItems += returnedQuantity;

            expectedCod += (long) quantity * price;
            collectedCod += (long) deliveredQuantity * price;
            returnedValue += (long) returnedQuantity * price;
        }

        return new OrderFulfillmentSummaryDto(
                order.getId().longValue(),
                order.getStatus().name(),
                totalItems,
                deliveredItems,
                returnedItems,
                expectedCod,
                collectedCod,
                returnedValue
        );
    }
}