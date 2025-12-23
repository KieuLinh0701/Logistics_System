import type { ShippingRate } from "./shippingRate";

export interface ServiceType {
  id: number;
  name: string;
  deliveryTime: string;
  description: string;
  status: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceTypeWithShippingRatesResponse {
  id: number;
  name: string;
  deliveryTime: string;
  rates: ShippingRate[];
}

export interface AdminServiceType {
  id: number;
  name: string;
  deliveryTime?: string;
  description?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateServiceTypePayload {
  name: string;
  description?: string;
  status?: string;
  deliveryTime?: string;
  deliveryTimeFrom?: number;
  deliveryTimeTo?: number;
  deliveryTimeUnit?: string;
}

export type UpdateServiceTypePayload = Partial<CreateServiceTypePayload>;