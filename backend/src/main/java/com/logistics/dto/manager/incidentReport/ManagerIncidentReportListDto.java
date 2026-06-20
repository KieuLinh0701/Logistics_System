package com.logistics.dto.manager.incidentReport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerIncidentReportListDto {
    private Integer id;
    private String code;
    private Order order;
    private String title;
    private String incidentType;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime handledAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Order {
        private String trackingNumber;
    }
}
