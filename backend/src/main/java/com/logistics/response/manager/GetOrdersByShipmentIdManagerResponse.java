package com.logistics.response.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;
import com.logistics.response.ListResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetOrdersByShipmentIdManagerResponse {
    ListResponse<ManagerShipmentDetailDto> orders;
    ShipmentStatus status;
    ShipmentType type;
}