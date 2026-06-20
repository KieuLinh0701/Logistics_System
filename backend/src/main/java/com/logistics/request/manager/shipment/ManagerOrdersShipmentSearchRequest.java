package com.logistics.request.manager.shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOrdersShipmentSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
}
