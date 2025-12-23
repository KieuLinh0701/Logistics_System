export const canProcessManagerPaymetSubmission = (value: string) => {
  return ["MISMATCHED"].includes(value);
};

export const PAYMENT_SUBMISSION_STATUSES = ['PENDING', 'IN_BATCH', 'MATCHED', 'MISMATCHED', 'ADJUSTED'] as const;
export const translatePaymentSubmissionStatus = (value: string): string => {
  const map: Record<string, string> = {
    PENDING: 'Shipper giữ tiền',
    IN_BATCH: 'Chờ đối soát',
    MATCHED: 'Đã khớp',
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
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';

    default: return value;
  }
};

// Manager: các trạng thái được phép chuyển tiếp tùy theo status hiện tại để xác nhận đối soát
export const getAllowedManagerStatuses = (currentStatus?: string): string[] => {
  if (!currentStatus) return [];

  switch (currentStatus) {
    case 'PENDING':
      return ['IN_BATCH'];

    case 'IN_BATCH':
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