package com.logistics.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.dto.manager.order.ManagerOrderListDto;
import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.user.order.UserOrderDetailDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.OrderProduct;

public class OrderMapper {

    public static ManagerShipmentDetailDto toManagerShipmentDetailDto(Order entity) {
        if (entity == null)
            return null;

        ManagerShipmentDetailDto.Recipient recipient = null;
        if (entity.getRecipientAddress() != null) {
            recipient = new ManagerShipmentDetailDto.Recipient(
                    entity.getRecipientAddress().getName(),
                    entity.getRecipientAddress().getPhoneNumber(),
                    entity.getRecipientAddress().getCityCode(),
                    entity.getRecipientAddress().getWardCode(),
                    entity.getRecipientAddress().getDetail());
        }

        ManagerShipmentDetailDto.Office toOffice = null;
        if (entity.getToOffice() != null) {
            toOffice = new ManagerShipmentDetailDto.Office(
                    entity.getToOffice().getId(),
                    entity.getToOffice().getName(),
                    entity.getToOffice().getPostalCode(),
                    entity.getToOffice().getCityCode(),
                    entity.getToOffice().getWardCode(),
                    entity.getToOffice().getDetail(),
                    entity.getToOffice().getLatitude(),
                    entity.getToOffice().getLongitude());
        }

        return new ManagerShipmentDetailDto(
                entity.getId(),
                entity.getTrackingNumber(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                recipient,
                toOffice,
                entity.getWeight() != null ? entity.getWeight() : BigDecimal.ZERO,
                entity.getCod() != null ? entity.getCod() : 0,
                entity.getTotalFee() != null ? entity.getTotalFee() : 0,
                entity.getPayer() != null ? entity.getPayer().name() : null,
                entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : null);
    }

    public static ManagerOrderDetailDto toManagerOrderDetailDto(Order entity,
            List<OrderHistory> orderHistories,
            List<OrderProduct> orderProducts) {
        if (entity == null) {
            return null;
        }

        return new ManagerOrderDetailDto(
                entity.getId(),
                entity.getTrackingNumber(),
                entity.getStatus().name(),
                entity.getCreatedByType().name(),
                entity.getSenderName(),
                entity.getSenderPhone(),
                entity.getSenderCityCode(),
                entity.getSenderWardCode(),
                entity.getSenderDetail(),
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
                OfficeMapper.toDto(entity.getToOffice()),
                OrderProductMapper.toDtoList(orderProducts),
                OrderHistoryMapper.toDtoList(orderHistories),
                entity.getEmployee() != null ? entity.getEmployee().getCode() : null,
                entity.getUser() != null ? entity.getUser().getCode() : null);
    }

    public static ManagerOrderListDto toManagerOrderListDto(Order entity) {
        if (entity == null) {
            return null;
        }

        return new ManagerOrderListDto(
                entity.getId(),
                entity.getTrackingNumber(),
                entity.getStatus().name(),
                entity.getSenderName(),
                entity.getSenderPhone(),
                entity.getSenderCityCode(),
                entity.getSenderWardCode(),
                entity.getSenderDetail(),
                AddressMapper.toDto(entity.getRecipientAddress()),
                entity.getPickupType().name(),
                entity.getWeight(),
                entity.getServiceType() != null ? entity.getServiceType().getName() : null,
                entity.getCod(),
                entity.getOrderValue(),
                entity.getTotalFee(),
                entity.getPayer().name(),
                entity.getPaymentStatus().name(),
                entity.getCreatedByType().name(),
                entity.getEmployee() != null ? entity.getEmployee().getCode() : null,
                entity.getUser() != null ? entity.getUser().getCode() : null,
                entity.getCreatedAt(),
                entity.getDeliveredAt(),
                entity.getPaidAt());
    }

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
                entity.getPromotion() != null ? entity.getPromotion().getId() : null,
                entity.getCodStatus().name());
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
                entity.getPaymentStatus().name(),
                entity.getCreatedAt(),
                entity.getDeliveredAt(),
                entity.getPaidAt(),
                entity.getCodStatus().name());
    }

}