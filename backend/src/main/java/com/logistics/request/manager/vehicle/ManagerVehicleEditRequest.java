package com.logistics.request.manager.vehicle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
