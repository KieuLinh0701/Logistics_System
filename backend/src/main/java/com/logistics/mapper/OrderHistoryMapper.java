package com.logistics.mapper;

import com.logistics.dto.OrderHistoryDto;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.ShipmentOrder;
import com.logistics.repository.ShipmentOrderRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class OrderHistoryMapper {

    private OrderHistoryMapper() {}

    public static OrderHistoryDto toDto(OrderHistory entity) {
        return toDto(entity, null);
    }

    public static OrderHistoryDto toDto(OrderHistory entity, String stopTypeFallback) {
        if (entity == null) {
            return null;
        }
        String pickupType = entity.getPickupTypeSnapshot() != null
                ? entity.getPickupTypeSnapshot().name()
                : null;
        String stopType = entity.getStopTypeSnapshot() != null
                ? entity.getStopTypeSnapshot().name()
                : stopTypeFallback;
        return new OrderHistoryDto(
                entity.getFromOffice() != null ? entity.getFromOffice().getName() : null,
                entity.getToOffice() != null ? entity.getToOffice().getName() : null,
                entity.getAction().name(),
                entity.getActionTime(),
                pickupType,
                stopType);
    }

    public static List<OrderHistoryDto> toDtoList(List<OrderHistory> entities) {
        return toDtoList(entities, null);
    }

    /**
     * Map danh sách OrderHistory sang DTO. Nếu truyền shipmentOrderRepository,
     * sẽ batch lookup stopType cho các history có shipment mà snapshot còn null
     * (dữ liệu cũ). Nếu repository = null thì bỏ qua bước này.
     */
    public static List<OrderHistoryDto> toDtoList(
            List<OrderHistory> entities,
            ShipmentOrderRepository shipmentOrderRepository) {

        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        // Tập hợp các shipmentId cần lookup (chỉ những history có shipment
        // và stopTypeSnapshot còn null).
        Map<Integer, Integer> needLookup = new HashMap<>();
        for (OrderHistory h : entities) {
            if (h.getStopTypeSnapshot() != null) continue;
            if (h.getShipment() == null || h.getOrder() == null) continue;
            Integer shipmentId = h.getShipment().getId();
            Integer orderId = h.getOrder().getId();
            if (shipmentId != null && orderId != null) {
                needLookup.put(shipmentId, orderId);
            }
        }

        // Batch lookup
        Map<String, String> stopTypeByShipmentOrder = new HashMap<>();
        if (shipmentOrderRepository != null && !needLookup.isEmpty()) {
            List<Integer> shipmentIds = needLookup.keySet().stream()
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            List<ShipmentOrder> shipOrders =
                    shipmentOrderRepository.findByShipmentIdIn(shipmentIds);
            for (ShipmentOrder so : shipOrders) {
                if (so.getShipment() == null || so.getOrder() == null) continue;
                Integer shId = so.getShipment().getId();
                Integer ordId = so.getOrder().getId();
                if (shId == null || ordId == null || so.getStopType() == null) continue;
                stopTypeByShipmentOrder.put(shId + ":" + ordId, so.getStopType().name());
            }
        }

        return entities.stream()
                .map(h -> {
                    String fallback = null;
                    if (h.getShipment() != null && h.getOrder() != null) {
                        fallback = stopTypeByShipmentOrder.get(
                                h.getShipment().getId() + ":" + h.getOrder().getId());
                    }
                    return toDto(h, fallback);
                })
                .collect(Collectors.toList());
    }
}