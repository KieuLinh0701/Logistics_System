import type {Address} from "./address.ts";

export interface RecipientSuggestionResponse {
    address: Address;
    type: RecipientAddressType;
    recipientStats: RecipientStats;
}

export interface RecipientAddress {
    address: Address;
    recipientStats: RecipientStats;
}

export interface RecipientAddressSuggestionRequest {
    phone: number;
}

export interface RecipientAddressRequest {
    id?: number;
    wardCode: number;
    wardName: string;
    cityCode: number;
    cityName: string;
    detail: string;
    name: string;
    phoneNumber: string;
    latitude?: number;
    longitude?: number;
}

export interface RecipientStats {
    totalSystemOrders: number;
    successRate: number;
    returnedRate: number;
    latestOrderDate: string;
}

export type RecipientAddressType = 'NONE' | 'HISTORY' | 'SAVED';