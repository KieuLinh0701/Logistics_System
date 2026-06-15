export interface CalculateShippingFeeRequest {
  weight: number;
  serviceTypeId: number;
  senderCodeCity: number;
  recipientCodeCity: number;
}

export interface CalculateTotalFeeUserRequest {
  weight: number;
  serviceTypeId: number;
  senderCodeCity: number;
  recipientCodeCity: number;
  cod: number;
  orderValue: number;
}

export interface CalculateWeightRequest {
  height: number;
  width: number;
  length: number;
  originalWeight: number;
}