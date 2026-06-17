export const SHIPPER_ROUTE_REFRESH_EVENT = "shipper-route-refresh";

export const dispatchShipperRouteRefresh = () => {
  window.dispatchEvent(new CustomEvent(SHIPPER_ROUTE_REFRESH_EVENT));
};
