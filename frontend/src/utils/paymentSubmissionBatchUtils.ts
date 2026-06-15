export const canProcessManagerPaymetSubmissionBatch = (value: string) => {
    return ["PENDING", 'CHECKING', 'PARTIAL'].includes(value);
};

export const PAYMENT_SUBMISSION_BATCH_STATUSES = [
    'OPEN',
    'PROCESSING',
    'COMPLETED'] as const;
export const translatePaymentSubmissionBatchStatus = (value: string): string => {
    switch (value) {
        case 'OPEN':
            return 'Đang mở';
        case 'PROCESSING':
            return 'Đang đối soát';
        case 'COMPLETED':
            return 'Đã đối soát';
        default:
            return value;
    }
};

export const PAYMENT_SUBMISSION_BATCH_FILTER_SORT = [
    'NEWEST',
    'OLDEST',
] as const;
export const translatePaymentSubmissionFilterSort = (value: string): string => {
    switch (value) {
        case 'NEWEST':
            return 'Mới nhất';
        case 'OLDEST':
            return 'Cũ nhất';

        default:
            return value;
    }
};

// Manager: các trạng thái được phép chuyển tiếp tùy theo status hiện tại để xác nhận đối soát
export const getAllowedManagerStatuses = (currentStatus?: string): string[] => {
    if (!currentStatus) return [];

    switch (currentStatus) {
        case 'PROCESSING':
            return ['COMPLETED'];

        case 'OPEN':
        case 'COMPLETED':
            return [];

        default:
            return [];
    }
};