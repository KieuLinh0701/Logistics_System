export interface ShippingRate {
  id: number;
  regionType: string;
  weightFrom: number;
  weightTo: number;
  price: number;
  unit: number;
  extraPrice: number;
  createdAt: string;
  updatedAt: string;
}