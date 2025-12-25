import type { User } from "./user";

export interface SettlementBatch {
    id: number;
    code: string;
    balanceAmount: number;
    remainAmount: number;
    status: string;
    createdAt: string;
    updatedAt: string;
    shop: User;
}

export interface SettlementBatchSelectedAllResponse {
    ids: number[] | [];
    totalAmount: number;
}