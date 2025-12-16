package com.logistics.request.manager.vehicle;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerVehicleEditRequest {
    private String status;
    private String description;
    private LocalDateTime nextMaintenanceDue;
    private String gpsDeviceId; 
}
