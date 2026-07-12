package com.logistics.service.shipper;

import com.logistics.dto.shipper.ShipperActiveShipmentDto;
import com.logistics.entity.Shipment;
import com.logistics.request.shipper.PickedUpRequest;
import com.logistics.request.shipper.UpdateDeliveryStatusRequest;

import java.util.List;
import java.util.Map;

public interface ShipmentDeliveryService {

    Shipment requireActiveDeliveryShipmentForOrder(Integer orderId);

    Shipment requireActiveInTransitShipmentForOrder(Integer orderId);

    Shipment requirePendingDeliveryShipmentForOrder(Integer orderId);

    Shipment loadDeliveryShipment(Integer shipmentId);

    void startShipment(Integer shipmentId);

    void finishShipment(Integer shipmentId);

    Map<String, Object> acceptPickupRequest(Integer orderId);

    void startPickup(Integer orderId);

    void markPickedUp(Integer orderId, PickedUpRequest req);

    void startDelivery(Integer orderId);

    Map<String, Object> startDeliveryAll(Integer shipmentId);

    void markDelivered(Integer orderId, UpdateDeliveryStatusRequest req);

    void markDeliveryFailed(Integer orderId, UpdateDeliveryStatusRequest req);

    void markDeliveryFailedFinal(Integer orderId, UpdateDeliveryStatusRequest req);

    void returnFailedToDestOffice(Integer orderId);

    void returnFailedFinalToDestOffice(Integer orderId);

    void submitReturnFailedToOffice(Integer orderId);

    void startReturn(Integer orderId);

    Map<String, Object> scanDeliveryShipmentOrder(Integer shipmentId, Integer orderId, String trackingNumber);

    void markReturnAtOrigin(Integer orderId);

    void deliverToOrigin(Integer orderId);

    void finalizeReturn(Integer orderId);

    void markReturnDelivered(Integer orderId, String proofImageUrl);

    void markReturnFailedFinal(Integer orderId, UpdateDeliveryStatusRequest req);

    List<ShipperActiveShipmentDto> listActiveShipmentsForCurrentShipper();

    void checkAndAutoFinishForOrder(Integer orderId);
}
