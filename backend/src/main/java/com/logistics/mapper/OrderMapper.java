package com.logistics.mapper;

import com.logistics.dto.OrderDto;
import com.logistics.entity.Order;

public class OrderMapper {

    public static OrderDto toDto(Order entity) {
        if (entity == null) {
            return null;
        }

        return new OrderDto( 
                entity.getId(),
                entity.getTrackingNumber(),
                entity.getStatus().name(),
                entity.getCreatedByType().name(),
                entity.getSenderName(),
                entity.getSenderPhone(),
                AddressMapper.toDto(entity.getSenderAddress()),
                entity.getRecipientName(),
                entity.getRecipientPhone(),
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
                entity.getCreatedAt()
                );
    }
}