export interface Address {
  id?: number;
  wardCode: number;
  cityCode: number;
  detail: string;
  isDefault: boolean;
  name: string;
  phoneNumber: string;
}

export interface AddressRequest {
  id?: number;
  wardCode: number;
  cityCode: number;
  detail: string;
  name: string;
  phoneNumber: string;
  isDefault: boolean;
}