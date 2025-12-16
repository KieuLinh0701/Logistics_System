import type { ShippingRequestAttachment } from "./shippingRequestAttachment";

export interface ShippingRequest {
  id: number;
  code: string;
  userCode: string;
  orderTrackingNumber: string;
  contactName: string;
  contactPhoneNumber: string;
  contactEmail: string;
  contactCityCode: number;
  contactWardCode: number;
  contactDetail: string;
  handlerName: string;
  handlerPhone: string;
  handlerEmail: string;
  requestType: string;
  requestContent: string;
  status: string;
  response: string;
  createdAt: Date;
  responseAt: Date;
  requestAttachments: ShippingRequestAttachment[];
  responseAttachments: ShippingRequestAttachment[];
}

export interface UserShippingRequestSearchRequest {
  page: number;
  limit: number;
  search?: string;
  type?: string;
  status?: string;
  sort?: string;
  startDate?: string;
  endDate?: string;
}

export interface ManagerShippingRequestSearchRequest {
  page: number;
  limit: number;
  search?: string;
  type?: string;
  status?: string;
  sort?: string;
  startDate?: string;
  endDate?: string;
}

