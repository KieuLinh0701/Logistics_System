package com.logistics.dto.manager.shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipmentPerformanceDto {
    private Integer id;
    private String code;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long orderCount;
    private long totalWeight;
}
