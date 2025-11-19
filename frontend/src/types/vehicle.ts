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

