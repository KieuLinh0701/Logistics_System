export interface ManagerShipment {
  id: number;
  code: string;
  vehicle: {
    licensePlate: string;
    capacity: number;
  }
  status: string;
  startTime: Date;
  endTime: Date;
  orderCount: number;
  totalWeight: number;
}