package com.logistics.request.manager.shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipmentAddEditRequest {
    private String type;
    private Integer vehicleId;
    private Integer toOfficeId;
    private Integer employeeId;
}
