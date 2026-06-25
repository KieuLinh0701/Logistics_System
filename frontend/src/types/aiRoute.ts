export type RouteStopType = "DELIVERY" | "PICKUP" | "RETURN_TO_OFFICE";
export type RouteStopStatus = "PENDING" | "ARRIVED" | "COMPLETED" | "SKIPPED" | "FAILED";
export type RouteMode = "CLOSED_LOOP" | "OPEN_ROUTE";
export type RouteOptimizationScope = "MANAGER_GLOBAL" | "SHIPPER_LOCAL" | "PICKUP_INSERTION";

export interface AiOptimizeRequest {
  startTime?: string;
  returnToOffice?: boolean;
  includePickupStops?: boolean;
  routeMode?: RouteMode;
  maxOrdersPerShipper?: number;
}

export interface AiRouteStop {
  stopId?: number;
  orderId?: number | null;
  stopType?: RouteStopType;
  stopSequence: number;
  trackingNumber?: string | null;
  recipientName?: string;
  recipientPhone?: string;
  recipientAddress?: string;
  latitude?: number;
  longitude?: number;
  codAmount?: number;
  priority?: string;
  etaTime?: string;
  etaMinutesFromStart?: number;
  legDistanceKm?: number;
  legDurationMinutes?: number;
  serviceTimeMinutes?: number;
  stopStatus?: RouteStopStatus;
  isInserted?: boolean;
  insertedReason?: string;
  originalSequence?: number;
  actualArrivedAt?: string;
  actualCompletedAt?: string;
}

export interface AiShipperRoute {
  routeId?: number;
  shipperUserId: number;
  shipperEmployeeId: number;
  shipperName: string;
  routeSequence: number;
  estimatedDistanceKm?: number;
  estimatedDurationMinutes?: number;
  fuelCost?: number;
  totalCod?: number;
  encodedPolyline?: string | null;
  startTime?: string;
  stopCount?: number;
  vehicleType?: "MOTORBIKE" | "ELECTRIC_BIKE" | string;
  totalWeightKg?: number;
  maxWeightKg?: number;
  batteryLevel?: number | null;
  routeMode?: RouteMode;
  returnToOffice?: boolean;
  routeVersion?: number;
  parentRouteId?: number;
  isActive?: boolean;
  currentLatitude?: number;
  currentLongitude?: number;
  actualStartedAt?: string;
  actualCompletedAt?: string;
  reoptimizedAt?: string;
  reoptimizeReason?: string;
  stops: AiRouteStop[];
  returnToOfficeStop?: AiRouteStop | null;
  shipmentId?: number | null;
  shipmentCode?: string | null;
  shipmentStatus?: string | null;
}

export interface AiUnassignedOrder {
  orderId: number;
  trackingNumber?: string;
  reason: string;
}

export interface AiRoutePlanDetail {
  id: number;
  planCode: string;
  status: "DRAFT" | "CONFIRMED" | "RUNNING" | "COMPLETED" | "CANCELLED";
  officeId: number;
  officeName: string;
  totalDistanceKm?: number;
  totalDurationMinutes?: number;
  totalFuelCost?: number;
  totalCod?: number;
  unassignedCount?: number;
  optimizationNote?: string;
  createdAt?: string;
  confirmedAt?: string;
  routeMode?: RouteMode;
  returnToOffice?: boolean;
  optimizationScope?: RouteOptimizationScope;
  versionNumber?: number;
  active?: boolean;
  startedAt?: string;
  completedAt?: string;
  routes: AiShipperRoute[];
  unassignedOrders: AiUnassignedOrder[];
}

export interface AiRoutePlanSummary {
  id: number;
  planCode: string;
  status: "DRAFT" | "CONFIRMED" | "RUNNING" | "COMPLETED" | "CANCELLED";
  totalDistanceKm?: number;
  totalDurationMinutes?: number;
  totalFuelCost?: number;
  totalCod?: number;
  unassignedCount?: number;
  routeCount?: number;
  routeMode?: RouteMode;
  returnToOffice?: boolean;
  createdAt?: string;
  confirmedAt?: string;
}

// Shipper re-optimize
export interface ShipperReOptimizeRequest {
  routeId?: number;
  currentLatitude: number;
  currentLongitude: number;
  currentAddress?: string;
  includeRemainingStopsOnly?: boolean;
  returnToOffice?: boolean;
  reason?: "MANUAL" | "PICKUP_INSERTION" | "GPS_DEVIATION" | "TRAFFIC";
}

// Pickup insertion
export interface PickupInsertionRequest {
  pickupOrderId: number;
  targetShipperEmployeeId?: number;
  targetRouteId?: number;
  autoAssign?: boolean;
  reOptimizeAfterInsert?: boolean;
}

// Shipper route response (from getDeliveryRoute)
export interface ShipperRouteResponse {
  routeInfo: {
    id: number;
    planId?: number;
    planCode?: string;
    name: string;
    startLocation?: string;
    totalStops: number;
    completedStops: number;
    totalDistance: number;
    estimatedDuration: number;
    fuelCost?: number;
    totalCOD: number;
    encodedPolyline?: string | null;
    startTime?: string;
    status: string;
    source?: string;
    routeMode?: RouteMode;
    returnToOffice?: boolean;
    routeVersion?: number;
    isActive?: boolean;
    parentRouteId?: number;
    currentLatitude?: number;
    currentLongitude?: number;
    actualStartedAt?: string;
    actualCompletedAt?: string;
    reoptimizedAt?: string;
    reoptimizeReason?: string;
  };
  deliveryStops: AiRouteStop[];
  office?: {
    id?: number;
    name: string;
    latitude?: number;
    longitude?: number;
  };
}
