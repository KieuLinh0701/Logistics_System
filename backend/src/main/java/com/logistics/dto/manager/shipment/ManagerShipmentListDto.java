package com.logistics.dto.manager.shipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipmentListDto {
    private Integer id;
    private String code;
    private VehicleShipment vehicle;
    private EmployeeShipment employee;
    private EmployeeShipment createdBy;
    private OfficeShipment fromOffice;
    private OfficeShipment toOffice;
    private String status;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleShipment {
        private String licensePlate;
        private BigDecimal capacity;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OfficeShipment {
        private String name;
        private String postalCode;
        private Integer cityCode;
        private Integer wardCode;
        private String detail;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmployeeShipment {
        private String name;
        private String code;
        private String phoneNumber;
        private String email;
    }
}
