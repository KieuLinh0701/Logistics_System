export interface CalculateShippingFeeRequest {
  weight: number;
  serviceTypeId: number;
  senderCodeCity: number;
  recipientCodeCity: number;
}