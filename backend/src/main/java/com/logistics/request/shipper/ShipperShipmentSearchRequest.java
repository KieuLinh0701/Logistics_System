package com.logistics.request.shipper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShipperShipmentSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String sort;
    private String status;
    private String startDate;
    private String endDate;
}
