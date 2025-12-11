package com.logistics.mapper;

import java.util.List;

import com.logistics.dto.user.order.UserOrderDetailDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.OrderProduct;

public class OrderMapper {

    public static UserOrderDetailDto toUserOrderDetailDto(Order entity,
            List<OrderHistory> orderHistories,
            List<OrderProduct> orderProducts) {
        if (entity == null) {
            return null;
        }

        return new UserOrderDetailDto(
                entity.getId(),
                entity.getTrackingNumber(),
                entity.getStatus().name(),
                entity.getCreatedByType().name(),
                entity.getSenderName(),
                entity.getSenderPhone(),
                entity.getSenderCityCode(),
                entity.getSenderWardCode(),
                entity.getSenderDetail(),
                AddressMapper.toDto(entity.getSenderAddress()),
                AddressMapper.toDto(entity.getRecipientAddress()),
                entity.getPickupType().name(),
                entity.getWeight(),
                ServiceTypeMapper.toDto(entity.getServiceType()),
                entity.getDiscountAmount(),
                entity.getCod(),
                entity.getOrderValue(),
                entity.getTotalFee(),
                entity.getPayer().name(),
                entity.getPaymentStatus().name(),
                entity.getNotes(),
                entity.getDeliveredAt(),
                entity.getPaidAt(),
                entity.getRefundedAt(),
                entity.getCreatedAt(),
                OfficeMapper.toDto(entity.getFromOffice()),
                OrderProductMapper.toDtoList(orderProducts),
                OrderHistoryMapper.toDtoList(orderHistories),
                entity.getPromotion() != null ? entity.getPromotion().getId() : null);
    }

    public static UserOrderListDto toUserOrderListDto(Order entity) {
        if (entity == null) {
            return null;
        }

        return new UserOrderListDto(
                entity.getId(),
                entity.getTrackingNumber(),
                entity.getStatus().name(),
                AddressMapper.toDto(entity.getRecipientAddress()),
                entity.getPickupType().name(),
                entity.getWeight(),
                entity.getServiceType() != null ? entity.getServiceType().getName() : null,
                entity.getCod(),
                entity.getOrderValue(),
                entity.getTotalFee(),
                entity.getPayer().name(),
                entity.getPaymentStatus().name());
    }

}