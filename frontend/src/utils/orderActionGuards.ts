// Helpers for order action guards based on shipment membership (Phase 8)
// Một shipper chỉ được thao tác với đơn nếu đơn thuộc chuyến DELIVERY đang IN_TRANSIT
// (trừ acceptPickup - chỉ gán employee, không cần shipment IN_TRANSIT)

export type ShipperOrderLike = {
  status?: string;
  pickupType?: string;
  shipmentId?: number | null;
  shipmentStatus?: string | null;
  shipmentType?: string | null;
};

export const isInActiveDeliveryShipment = (order: ShipperOrderLike): boolean =>
  !!order.shipmentId &&
  order.shipmentType === "DELIVERY" &&
  order.shipmentStatus === "IN_TRANSIT";

export const canAcceptPickup = (order: ShipperOrderLike): boolean =>
  ["CONFIRMED", "READY_FOR_PICKUP", "URGENT_PICKUP", "PICKUP_RETRY"].includes(
    order.status || ""
  );

export const canStartPickup = (order: ShipperOrderLike): boolean =>
  order.pickupType === "PICKUP_BY_COURIER" &&
  isInActiveDeliveryShipment(order) &&
  ["CONFIRMED", "PICKUP_RETRY", "READY_FOR_PICKUP"].includes(order.status || "");

export const canMarkPickedUp = (order: ShipperOrderLike): boolean =>
  isInActiveDeliveryShipment(order) &&
  ["PICKING_UP", "READY_FOR_PICKUP", "PICKUP_RETRY"].includes(order.status || "");

export const canStartDelivery = (order: ShipperOrderLike): boolean =>
  isInActiveDeliveryShipment(order) && order.status === "PICKED_UP";

export const canMarkDelivered = (order: ShipperOrderLike): boolean =>
  isInActiveDeliveryShipment(order) && order.status === "DELIVERING";

export const canDeliverToOrigin = (order: ShipperOrderLike): boolean =>
  isInActiveDeliveryShipment(order) &&
  order.pickupType === "PICKUP_BY_COURIER" &&
  order.status === "PICKED_UP";

export const canReturnFailed = (order: ShipperOrderLike): boolean =>
  isInActiveDeliveryShipment(order) && order.status === "DELIVERY_RETRY";
