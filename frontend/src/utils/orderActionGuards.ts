export type ShipperOrderLike = {
  status?: string;
  pickupType?: string;
  shipmentId?: number | null;
  shipmentStatus?: string | null;
  shipmentType?: string | null;
  stopType?: string | null; 
};


export const isInActiveDeliveryShipment = (order: ShipperOrderLike): boolean => {
  if (!order.shipmentId) return false;

  // Check if it's a return order
  const isReturnOrder = order.stopType === "RETURN_TO_OFFICE"
    || order.status === "RETURN_AT_ORIGIN_OFFICE"
    || order.status === "RETURNING"
    || order.status === "RETURN_RETRY";

  if (isReturnOrder) {
    // Return orders: allow both PENDING and IN_TRANSIT
    return order.shipmentType === "DELIVERY" &&
      (order.shipmentStatus === "PENDING" || order.shipmentStatus === "IN_TRANSIT");
  }

  // Delivery orders: require IN_TRANSIT
  return order.shipmentType === "DELIVERY" &&
    order.shipmentStatus === "IN_TRANSIT";
};

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


export const isReturnOrder = (order: ShipperOrderLike): boolean =>
  order.stopType === "RETURN_TO_OFFICE"
  || ["RETURN_AT_ORIGIN_OFFICE", "RETURNING", "RETURN_RETRY"].includes(order.status || "");

export const isInActiveReturnShipment = (order: ShipperOrderLike): boolean => {
  if (!order.shipmentId) return false;

  const returnStatuses = ["RETURN_AT_ORIGIN_OFFICE", "RETURNING", "RETURN_RETRY"];
  if (!returnStatuses.includes(order.status || "")) return false;

  return order.shipmentType === "DELIVERY" &&
    (order.shipmentStatus === "PENDING" || order.shipmentStatus === "IN_TRANSIT");
};

export const canMarkReturnDelivered = (order: ShipperOrderLike): boolean => {
  if (!isInActiveReturnShipment(order)) return false;

  return ["RETURNING", "RETURN_RETRY"].includes(order.status || "");
};
