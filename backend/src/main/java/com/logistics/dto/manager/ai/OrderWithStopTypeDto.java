package com.logistics.dto.manager.ai;

import com.logistics.entity.Order;
import com.logistics.enums.RouteStopType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderWithStopTypeDto {
    private final Order order;
    private final RouteStopType stopType;
}
