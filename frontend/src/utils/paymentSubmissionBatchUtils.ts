export const canProcessManagerPaymetSubmissionBatch = (value: string) => {
  return ["PENDING", 'CHECKING', 'PARTIAL'].includes(value);
};

export const PAYMENT_SUBMISSION_BATCH_STATUSES = [
  'PENDING',
  'CHECKING',
  'COMPLETED',
  'PARTIAL',
  'CANCELLED'] as const;
export const translatePaymentSubmissionBatchStatus = (value: string): string => {
  switch (value) {
    case 'PENDING': return 'Đã nộp tiền';
    case 'CHECKING': return 'Đang đối soát';
    case 'COMPLETED': return 'Đã đối soát';
    case 'PARTIAL': return 'Lệch tiền';
    case 'CANCELLED': return 'Đã huỷ';
    default: return value;
  }
};

export const PAYMENT_SUBMISSION_BATCH_FILTER_SORT = [
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
      return ['CHECKING', 'CANCELLED'];

    case 'CHECKING':
      return ['COMPLETED', 'PARTIAL'];

    case 'PARTIAL':
      return ['COMPLETED'];

    case 'COMPLETED':
    case 'CANCELLED':
      return [];

    default:
      return [];
  }
};