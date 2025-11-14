export interface Office {
  id: number;
  code: string;
  postalCode: string;
  name: string;
  cityCode: number;
  wardCode: number;
  detail: string;
  latitude: number;
  longitude: number;
  email: string;
  phoneNumber: string;
  openingTime: string;
  closingTime: string;
  type: string;
  //manager: Employee;
  capacity: number;
  notes: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface OfficeSearchRequest {
  page?: number; 
  limit?: number; 
  search?: string; 
  city?: boolean;
  ward?: boolean;
}