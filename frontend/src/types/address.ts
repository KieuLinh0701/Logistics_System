export interface Address {
    id?: number;
    wardCode: number;
    wardName: string;
    cityCode: number;
    cityName: string;
    detail: string;
    longitude: number;
    latitude: number;
    isDefault: boolean;
    name: string;
    phoneNumber: string;
    fullAddress: string;
}

export interface AddressRequest {
    id?: number;
    wardCode: number;
    wardName: string;
    cityCode: number;
    cityName: string;
    detail: string;
    name: string;
    phoneNumber: string;
    isDefault: boolean;
    latitude?: number;
    longitude?: number;
}

export interface GeoCoords {
    lat: number;
    lng: number;
}