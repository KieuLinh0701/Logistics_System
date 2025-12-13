export interface ManagerEmployeePerformanceShipment {
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

export interface ManagerShipment {
  id: number;
  code: string;

  vehicle: {
    licensePlate: string;
    capacity: number; 
  };

  employee: {
    name: string;
    code: string;
    phoneNumber: string;
    email: string;
  };

  createdBy: {
    name: string;
    code: string;
    phoneNumber: string;
    email: string;
  };

  fromOffice: {
    name: string;
    postalCode: string;
    cityCode: number;
    wardCode: number;
    detail: string;
    latitude: number;
    longitude: number;
  };

  toOffice: {
    name: string;
    postalCode: string;
    cityCode: number;
    wardCode: number;
    detail: string;
    latitude: number;
    longitude: number;
  };

  status: string;
  type: string;

  startTime: string; 
  endTime: string; 
}

export interface ManagerShipmentSearchRequest {
  page: number;
  limit: number;
  search?: string;
  sort?: string;
  status?: string;
  type?: string;
  startDate?: string;
  endDate?: string;
}

export interface ManagerOrderShipment {
  id: number;
  trackingNumber: string;
  status: string;
  weight: number;
  cod: number;
  totalFee: number;
  paymentStatus: string;
  payer: string;
  recipient: {
    name: string;
    phone: string;
    cityCode: number;
    wardCode: number;
    detail: string;
  }
  toOffice: {
    name: string;
    postalCode: string;
    cityCode: number;
    wardCode: number;
    detail: string;
    latitude: number;
    longitude: number;
  };
}

export interface ManagerOrderShipmentSearchRequest {
  page: number;
  limit: number;
  search?: string; 
}