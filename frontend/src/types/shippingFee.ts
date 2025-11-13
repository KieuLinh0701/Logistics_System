export interface CalculateShippingFeeRequest {
  weight: number;
  serviceTypeId: number;
  senderCodeCity: number;
  recipientCodeCity: number;
}

export interface ShippingFeeResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}