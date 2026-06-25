package com.logistics.dto.shipper.shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShipperShipmentListDto {
    private Integer id;
    private String code;
    private EmployeeShipment createdBy;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmployeeShipment {
        private Integer id;
        private String lastName; 
        private String firstName;
        private String code;
        private String phoneNumber;
        private String email;
    }
}
