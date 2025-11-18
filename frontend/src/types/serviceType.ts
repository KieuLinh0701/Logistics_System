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