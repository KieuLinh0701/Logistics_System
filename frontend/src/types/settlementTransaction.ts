import type { Order } from "./orderHistory";

export interface SettlementTransaction {
    id: number;
    code: string;
    amount: number;
    type: string;
    status: string;
    paidAt: string;

    bankName: string;
    accountNumber: string;
    accountName: string;
    referenceCode: string;
    order: Order[];
    transaction: SettlementTransaction[];
}