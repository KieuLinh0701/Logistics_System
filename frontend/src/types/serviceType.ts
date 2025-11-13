export interface ServiceType {
  id: number;
  name: string;
  deliveryTime: string;
  description: string;
  status: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceTypeResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}