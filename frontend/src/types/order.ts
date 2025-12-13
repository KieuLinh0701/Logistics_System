import type { Address } from "./address";
import type { Office } from "./office";
import type { OrderHistory } from "./orderHistory";
import type { OrderProduct, OrderProductPrint, OrderProductRequest } from "./orderProduct";
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
  senderCityCode: number;
  senderWardCode: number;
  senderDetail: string;
  senderAddress: Address;
  recipientAddress: Address;
  pickupType: string;
  weight: number;
  serviceTypeName: string;
  serviceType: ServiceType;
  discountAmount: number;
  cod: number;
  totalFee: number;
  orderValue: number;
  payer: string;
  paymentStatus: string;
  notes: string;
  promotionId: number | undefined;
  shippingFee: number;
  paidAt: Date;
  deliveredAt: Date;
  refundedAt: Date;
  createdAt: Date;
  fromOffice: Office;
  toOffice: Office;
  orderProducts: OrderProduct[];
  orderHistories: OrderHistory[];
  employeeCode: string;
  userCode: string;
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

export interface UserOrderRequest {
  id?: number;
  code?: number;
  status: string;
  senderAddressId: number;
  recipientName: string;
  recipientPhone: string;
  recipientCityCode: number;
  recipientWardCode: number;
  recipientDetail: string;
  pickupType: string;
  weight: number;
  serviceTypeId: number;
  cod: number;
  orderValue: number;
  payer: string;
  notes: string;
  fromOfficeId: number;
  orderProducts: OrderProductRequest[];
  promotionId: number;
}

export interface ManagerOrderSearchRequest {
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

export interface ManagerOrderRequest {
  id?: number;
  code?: string;
  senderName: string;
  senderDetail: string;
  senderWardCode: number;
  senderCityCode: number;
  senderPhone: string;
  recipientName: string;
  recipientPhone: string;
  recipientCityCode: number;
  recipientWardCode: number;
  recipientDetail: string;
  weight: number;
  serviceTypeId: number;
  cod: number;
  orderValue: number;
  payer: string;
  notes: string;
}

export interface CreateOrderSuccess {
  trackingNumber: string;
  orderId: number;
}

export interface OrderPrint {
  trackingNumber: string;
  barcodeTrackingNumber: string;
  fromOfficeCode: string;
  qrFromOfficeCode: string;
  senderName: string;
  senderPhone: string;
  senderCityCode: number;
  senderWardCode: number;
  senderDetail: string;
  recipientAddress: Address;
  codAmount: number;
  weight: number;
  createdAt: Date;
  orderProducts: OrderProductPrint[];
}