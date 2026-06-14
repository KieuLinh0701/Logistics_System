import type {ShippingRequestAttachment} from "./shippingRequestAttachment";

export interface ShippingRequest {
    id: number;
    code: string;
    userCode: string;
    orderTrackingNumber: string;
    contactName: string;
    contactPhoneNumber: string;
    contactEmail: string;
    contactCityCode: number;
    contactCityName: number;
    contactWardCode: number;
    contactWardName: string;
    contactDetail: string;
    contactFullAddress: string;
    handlerName: string;
    handlerPhoneNumber: string;
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

export interface PublicShippingRequestCreate {
    contactName: string;
    contactEmail: string;
    contactPhoneNumber: string;
    requestType: string;
    requestContent: string;
}