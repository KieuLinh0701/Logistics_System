export const canProcessManagerPaymentSubmission = (value: string) => {
    return [
        "PENDING",
        "PROCESSING",
        "MISMATCHED",
    ].includes(value);
};

export const PAYMENT_SUBMISSION_STATUSES = ['PENDING', 'PROCESSING', 'MATCHED', 'MISMATCHED', 'ADJUSTED'] as const;
export const translatePaymentSubmissionStatus = (value: string): string => {
    const map: Record<string, string> = {
        PENDING: 'Chờ nộp',
        PROCESSING: 'Đang xem xét',
        MATCHED: 'Khớp tiền',
        MISMATCHED: 'Lệch tiền',
        ADJUSTED: 'Đã điều chỉnh',
    };
    return map[value] || value;
};

export const PAYMENT_SUBMISSION_FILTER_SORT = [
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
        case 'PENDING':
            return ['PROCESSING'];

        case 'PROCESSING':
            return ['MATCHED', 'MISMATCHED'];

        case 'MISMATCHED':
            return ['ADJUSTED'];

        case 'MATCHED':
        case 'ADJUSTED':
            return [];

        default:
            return [];
    }
};