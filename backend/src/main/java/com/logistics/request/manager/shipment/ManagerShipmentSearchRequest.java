package com.logistics.request.manager.shipment;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipmentSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String sort;
    private String status;
    private String type;
    private String startDate;
    private String endDate;
}
