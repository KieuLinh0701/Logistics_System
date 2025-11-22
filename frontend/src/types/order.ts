import type { Address } from "./address";
import type { ServiceType } from "./serviceType";

export interface AdminOrder {
  id: number;
  trackingNumber: string;
  senderName: string;
  recipientName: string;
  status: string;
  totalFee: number;
  createdAt: string;
}

export interface Order {
  id: number;
  trackingNumber: string;
  status: string;
  createdByType: string;
  senderName: string;
  senderPhone: string;
  senderAddress: Address;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: Address;
  pickupType: string;
  weight: number;
  serviceType: ServiceType;
  discountAmount: number;
  cod: number;
  totalFee: number;
  orderValue: number;
  payer: string;
  paymentStatus: string;
  notes: string;
  paidAt: Date;
  deliveredAt: Date;
  refundedAt: Date;
  createdAt: Date;
}

export interface UserOrderSearchRequest {
  page: number;
  limit: number;
  search?: string;
  payer?: string;
  status?: string;
  pickupType?: string;
  serviceTypeId?: number;
  paymentStatus?: string;
  cod?: string;
  sort?: string;
  startDate?: string;
  endDate?: string;
}
