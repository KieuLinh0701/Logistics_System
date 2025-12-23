export interface AdminVehicle {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
  description?: string;
  officeId?: number;
  office?: {
    id: number;
    name: string;
  } | null;
  createdAt: string;
  updatedAt?: string;
}

export interface Vehicle {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
  description: string;
  lastMaintenanceAt: string | null;
  nextMaintenanceDue: string | null;
  latitude: number;
  longitude: number;
  gpsDeviceId: string;
}

export interface ManagerVehicleSearchRequest {
    page: number;
    limit: number;
    search?: string;
    type?: string;
    status?: string;
    sort?: string;
    startDate?: string;
    endDate?: string;
}

export interface ManagerVehicleEditRequest {
    status: string;
    description: string;
    nextMaintenanceDue: string | null;
    gpsDeviceId: string;
}
