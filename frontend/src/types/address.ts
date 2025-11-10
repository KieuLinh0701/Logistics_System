export interface Address {
  id: number;
  wardCode: number;
  cityCode: number;
  detail: string;
  createdAt: Date;
}

export interface AddressResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}