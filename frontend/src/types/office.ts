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

export interface AdminOffice {
  id: number;
  code: string;
  postalCode?: string;
  name: string;
  email?: string;
  phoneNumber?: string;
  type?: string;
  status?: string;
  latitude?: number;
  longitude?: number;
  openingTime?: string;
  closingTime?: string;
  capacity?: number;
  notes?: string;
  cityCode?: number;
  wardCode?: number;
  detail?: string;
  createdAt: string;
}

export interface CreateOfficePayload {
  code: string;
  postalCode?: string;
  name: string;
  latitude: number;
  longitude: number;
  email?: string;
  phoneNumber?: string;
  openingTime?: string;
  closingTime?: string;
  type?: string;
  status?: string;
  capacity?: number;
  notes?: string;
  wardCode: number;
  cityCode: number;
  detailAddress: string;
}

export type UpdateOfficePayload = Partial<CreateOfficePayload>;