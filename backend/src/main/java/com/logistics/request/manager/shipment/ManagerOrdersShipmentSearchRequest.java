package com.logistics.request.manager.shipment;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOrdersShipmentSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
}
