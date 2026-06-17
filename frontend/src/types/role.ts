export interface Role {
    id?: number;
    name?: string;
    description?: string;
    createdAt?: string;
    updatedAt?: string;
    permissionGroupIds?: number[];
}

export interface UserRoleSearchRequest {
    page: number;
    limit: number;
    search?: string;
    startDate?: string;
    endDate?: string;
}