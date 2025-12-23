export const SETTLEMENT_TRANSACTION_TYPES = [
  'SHOP_TO_SYSTEM',
  'SYSTEM_TO_SHOP'] as const;
export const translateSettlementTransactionType = (value: string): string => {
  switch (value) {
    case 'SHOP_TO_SYSTEM': return 'Shop chuyển';
    case 'SYSTEM_TO_SHOP': return 'Hệ thống chuyển';
    default: return value;
  }
};


export const SETTLEMENT_TRANSACTION_STATUSES = [
  'PENDING',
  'SUCCESS',
  'FAILED'] as const;
export const translateSettlementTransactionStatus = (value: string): string => {
  switch (value) {
    case 'PENDING': return 'Đang xử lý';
    case 'SUCCESS': return 'Thành công';
    case 'FAILED': return 'Thất bại';
    default: return value;
  }
};