package com.logistics.request.manager.shipmentOrder;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveShipmentOrdersRequest { 
    private List<Integer> removedOrderIds;
    private List<Integer> addedOrderIds;
}
