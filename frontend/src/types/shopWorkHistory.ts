export interface UserShopWorkHistorySearchUserRequest {
    page: number;
    limit: number;
    search?: string;
    isCurrent?: boolean;
    sort?: string;
    startDate?: string;
    endDate?: string;
}

export interface ShopWorkHistory {
    id: number;
    roleName: string;
    isCurrent: boolean;
    joinedAt: string;
    leftAt: string;
    note: string;
}