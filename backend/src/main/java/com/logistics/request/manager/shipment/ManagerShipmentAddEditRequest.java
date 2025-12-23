package com.logistics.request.manager.shipment;

import lombok.*;

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
