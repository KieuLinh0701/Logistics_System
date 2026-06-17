export interface AiOptimizeRequest {
  startTime?: string;
}

export interface AiRouteStop {
  stopId?: number;
  orderId: number;
  stopSequence: number;
  trackingNumber?: string;
  recipientName?: string;
  recipientPhone?: string;
  recipientAddress?: string;
  latitude?: number;
  longitude?: number;
  codAmount?: number;
  priority?: string;
  etaTime?: string;
  etaMinutesFromStart?: number;
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
  encodedPolyline?: string;
  startTime?: string;
  stopCount?: number;
  vehicleType?: "MOTORBIKE" | "ELECTRIC_BIKE" | string;
  totalWeightKg?: number;
  maxWeightKg?: number;
  batteryLevel?: number | null;
  stops: AiRouteStop[];
}

export interface AiUnassignedOrder {
  orderId: number;
  trackingNumber?: string;
  reason: string;
}

export interface AiRoutePlanDetail {
  id: number;
  planCode: string;
  status: "DRAFT" | "CONFIRMED" | "CANCELLED";
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
  routes: AiShipperRoute[];
  unassignedOrders: AiUnassignedOrder[];
}

export interface AiRoutePlanSummary {
  id: number;
  planCode: string;
  status: "DRAFT" | "CONFIRMED" | "CANCELLED";
  totalDistanceKm?: number;
  totalDurationMinutes?: number;
  totalFuelCost?: number;
  totalCod?: number;
  unassignedCount?: number;
  routeCount?: number;
  createdAt?: string;
  confirmedAt?: string;
}
