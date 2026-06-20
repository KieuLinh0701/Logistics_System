package com.logistics.request.manager.shipmentOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveShipmentOrdersRequest { 
    private List<Integer> removedOrderIds;
    private List<Integer> addedOrderIds;
}
