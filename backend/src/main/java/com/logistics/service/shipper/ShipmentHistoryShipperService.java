package com.logistics.service.shipper;

import com.logistics.request.shipper.ShipperOrdersShipmentSearchRequest;
import com.logistics.request.shipper.ShipperShipmentSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.dto.shipper.shipment.ShipperShipmentDetailDto;
import com.logistics.dto.shipper.shipment.ShipperShipmentListDto;

public interface ShipmentHistoryShipperService {

    ListResponse<ShipperShipmentListDto> list(int userId, ShipperShipmentSearchRequest request);

    ListResponse<ShipperShipmentDetailDto> getOrdersByShipmentId(int userId, int shipmentId, ShipperOrdersShipmentSearchRequest request);

    byte[] export(int userId, ShipperShipmentSearchRequest request);

    byte[] exportOrdersByShipmentId(int userId, int shipmentId, ShipperOrdersShipmentSearchRequest request);
}
