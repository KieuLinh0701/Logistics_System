export interface OrderHistory {
    fromOfficeName: string;
    toOfficeName: string;
    action: string;
    actionTime: Date;
    pickupType?: string;
    stopType?: string;
    order: Order;
}

export interface Order {
    id: number;
    trackingNumber: string;
    status: string;
    weight: string;
    serviceTypeName: string;
    notes: string;
    
}