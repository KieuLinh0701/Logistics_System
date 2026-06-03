export type ShipperVehicleType = "MOTORBIKE" | "ELECTRIC_BIKE";
export type ShipperVehicleStatus = "ACTIVE" | "INACTIVE" | "MAINTENANCE";

export interface ShipperVehicleSetting {
  id: number;
  shipperId: number;
  vehicleType: ShipperVehicleType;
  maxOrders: number;
  maxWeightKg: number;
  currentOrders?: number | null;
  currentWeightKg?: number | null;
  batteryLevel?: number | null;
  status: ShipperVehicleStatus;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ShipperVehicleSettingRequest {
  vehicleType: ShipperVehicleType;
  maxOrders: number;
  maxWeightKg: number;
  batteryLevel?: number | null;
  notes?: string | null;
}

export interface ShipperVehicleStatusUpdateRequest {
  status: ShipperVehicleStatus;
}
