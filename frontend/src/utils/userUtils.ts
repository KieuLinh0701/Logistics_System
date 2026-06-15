export const USER_FILTER_SORT = [
    'NEWEST',
    'OLDEST',
] as const;

export const translateUserFilterSort = (value: string): string => {
    switch (value) {
        case 'NEWEST':
            return 'Mới nhất';
        case 'OLDEST':
            return 'Cũ nhất';
        default:
            return value;
    }
};

export const USER_ACTIVE = [
    'ACTIVE',
    'ACTIVE',
] as const;

export const translateUserActive = (value: string): string => {
    switch (value) {
        case 'ALL':
            return 'Tất cả trạng thái';
        case 'ACTIVE':
            return 'Đang hoạt động';
        case 'INACTIVE':
            return 'Ngừng hoạt động';
        default:
            return value;
    }
};

export const getActiveValue = (status: string): boolean | undefined => {
    switch (status) {
        case "ACTIVE": return true;
        case "INACTIVE": return false;
        case "ALL":
        default: return undefined;
    }
};