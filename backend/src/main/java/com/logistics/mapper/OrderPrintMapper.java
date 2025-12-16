package com.logistics.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.logistics.dto.OrderPrintDto;
import com.logistics.entity.Order;
import com.logistics.entity.OrderProduct;
import com.logistics.utils.BarcodeUtils;
import com.logistics.utils.QrCodeUtils;

public class OrderPrintMapper {

    public static OrderPrintDto toDto(Order entity, List<OrderProduct> orderProducts) {
        if (entity == null || entity.getTrackingNumber() == null) {
            return null;
        }

        String barcodeTrackingNumber = BarcodeUtils.generateBarcode(entity.getTrackingNumber());

        String qrcodeFromOffice = null;
        if (entity.getFromOffice() != null) {
            qrcodeFromOffice = QrCodeUtils.generateQrCode(entity.getFromOffice().getPostalCode());
        }

        return new OrderPrintDto(
                entity.getTrackingNumber(),
                barcodeTrackingNumber,
                entity.getFromOffice() != null ? entity.getFromOffice().getPostalCode() : null,
                qrcodeFromOffice,
                entity.getSenderName(),
                entity.getSenderPhone(),
                entity.getSenderCityCode() != null ? String.valueOf(entity.getSenderCityCode()) : null,
                entity.getSenderWardCode() != null ? String.valueOf(entity.getSenderWardCode()) : null,
                entity.getSenderDetail(),
                entity.getRecipientAddress() != null ? AddressMapper.toDto(entity.getRecipientAddress()) : null,
                Integer.valueOf((entity.getTotalFee() != null ? entity.getTotalFee() : 0)
                        + (entity.getCod() != null ? entity.getCod() : 0)),
                entity.getWeight(),
                entity.getCreatedAt(),
                OrderPrintMapper.toDtoList(orderProducts));
    }

    public static OrderPrintDto.OrderProductDto toOrderProductDto(OrderProduct entity) {
        if (entity == null) {
            return null;
        }

        return new OrderPrintDto.OrderProductDto(
                entity.getProduct().getName(),
                entity.getQuantity());
    }

    public static List<OrderPrintDto.OrderProductDto> toDtoList(List<OrderProduct> entities) {
        if (entities == null)
            return Collections.emptyList();
        return entities.stream()
                .map(OrderPrintMapper::toOrderProductDto)
                .collect(Collectors.toList());
    }

}