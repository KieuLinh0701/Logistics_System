export type AttemptCategory = "PICKUP" | "DELIVERY" | "RETURN_DELIVERY";
export type AttemptStatus = "SUCCESS" | "FAILED";

export interface AttemptHistoryItem {
    id: number;
    attemptCategory: AttemptCategory;
    attemptType: "DELIVERY" | "RETURN_DELIVERY" | null;
    orderId: number | null;
    trackingNumber: string | null;
    attemptNumber: number | null;
    pickupStatus: AttemptStatus | null;
    deliveryStatus: AttemptStatus | null;
    failReason: string | null;
    note: string | null;
    proofImageUrl: string | null;
    attemptedAt: string | null;
    shipperId: number | null;
    shipperName: string | null;
    shipperPhone: string | null;
}

export interface AttemptSearchParams {
    page?: number;
    limit?: number;
    search?: string;
    category?: string;
    status?: string;
    sort?: string;
    startDate?: string;
    endDate?: string;
}
