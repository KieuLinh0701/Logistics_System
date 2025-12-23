export interface OrderHistory {
    fromOfficeName: string;
    toOfficeName: string;
    action: string;
    note: string;
    actionTime: Date;
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