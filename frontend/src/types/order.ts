import type {Address} from "./address";
import type {Office} from "./office";
import type {OrderHistory} from "./orderHistory";
import type {PaymentSubmission} from "./paymentSubmission";
import type {OrderProduct, OrderProductPrint, OrderProductRequest} from "./orderProduct";
import type {Promotion} from "./promotion";
import type {ServiceType} from "./serviceType";

export interface AdminOrder {
    id: number;
    trackingNumber: string;
    senderName: string;
    senderPhone?: string;
    senderCityCode?: number;
    senderWardCode?: number;
    senderDetail?: string;
    senderAddress?: Address;
    fromOffice?: Office;
    toOffice?: Office;
    pickupType?: string;
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
    recipientDetail: string;
    recipientName: string;
    recipientPhone: string;
    recipientCityCode: number;
    recipientCityName: string;
    recipientWardCode: number;
    recipientWardName: string;
    recipientLatitude: number;
    recipientLongitude: number;
    recipientFullAddress: string;
    senderName: string;
    senderPhone: string;
    senderCityCode: number;
    senderCityName: string;
    senderWardCode: number;
    senderWardName: string;
    senderDetail: string;
    senderLatitude: number;
    senderLongitude: number;
    senderFullAddress: string;
    senderAddress: Address;
    recipientAddress: Address;
    pickupType: string;
    weight: number;
    originalWeight: number;
    height: number;
    length: number;
    width: number;
    adjustedWeight: number;
    adjustedOriginalWeight: number;
    adjustedHeight: number;
    adjustedWidth: number;
    adjustedLength: number
    serviceTypeName: string;
    serviceType: ServiceType;
    discountAmount: number;
    cod: number;
    actualCollected?: number;
    returnedAmount?: number;
    totalFee: number;
    orderValue: number;
    payer: string;
    paymentStatus: string;
    notes: string;
    promotion: Promotion | undefined;
    shippingFee: number;
    paidAt: string;
    deliveredAt: string;
    refundedAt: string;
    createdAt: string;
    fromOffice: Office;
    toOffice: Office;
    orderProducts: OrderProduct[];
    orderHistories: OrderHistory[];
    employeeCode: string;
    userCode: string;
    codStatus: string;
    paymentSubmissions?: PaymentSubmission[];
}

export interface StatusCount {
    status: string;
    count: number;
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
    recipientName?: string;
    recipientPhone?: string;
    recipientCityCode?: number;
    recipientCityName?: string;
    recipientWardCode?: number;
    recipientDetail?: string;
    recipientWardName?: string;
    recipientLatitude?: number;
    recipientLongitude?: number;
    recipientAddressId?: number;
    saveRecipient: boolean;
    pickupType: string;
    weight: number;
    length: number;
    width: number;
    height: number;
    originalWeight: number;
    serviceTypeId: number;
    cod: number;
    orderValue: number;
    payer: string;
    notes: string;
    fromOfficeId: number;
    orderProducts: OrderProductRequest[];
    promotionId: number;

    discountAmount: number;
    totalFee: number;
    shippingFee: number;
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
    senderPhone: string;
    senderDetail: string;
    senderWardCode: number;
    senderWardName: string;
    senderCityCode: number;
    senderCityName: string;
    senderLatitude: number;
    senderLongitude: number;
    recipientName: string;
    recipientPhone: string;
    recipientCityCode: number;
    recipientCityName: string;
    recipientWardCode: number;
    recipientWardName: string;
    recipientDetail: string;
    recipientLatitude: number;
    recipientLongitude:number;
    weight: number;
    originalWeight: number;
    length: number;
    width: number;
    height: number;
    serviceTypeId: number;
    cod: number;
    orderValue: number;
    payer: string;
    notes: string;
    pickupType?: string;
    fromOfficeId?: number;
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
    senderAddress: Address;
    recipientAddress: Address;
    codAmount: number;
    weight: number;
    createdAt: Date;
    orderProducts: OrderProductPrint[];
}