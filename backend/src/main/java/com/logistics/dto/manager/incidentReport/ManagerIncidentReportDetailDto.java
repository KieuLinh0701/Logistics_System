package com.logistics.dto.manager.incidentReport;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerIncidentReportDetailDto {
    private Integer id;
    private String code;
    private Order order;
    private User shipper;
    private User handler;
    private String title;
    private String incidentType;
    private String description;
    private Address address;
    private List<String> images;
    private String status;
    private String priority;
    private String resolution;
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

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private String fullName;
        private String phoneNumber;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {
        private String detail;
        private Integer cityCode;
        private Integer wardCode;
    }
}
