import type { SettlementBatch } from "../types/settlementBatch";

export const canPayUserSettlementBatch = (
  batch: SettlementBatch
): boolean => {
  return (
    ['PENDING', 'PARTIAL', 'FAILED'].includes(batch.status) &&
    batch.remainAmount > 0 && batch.balanceAmount < 0
  );
};

export const SETTLEMENT_BATCH_STATUSES = [
  'PENDING',
  'PARTIAL',
  'COMPLETED',
  'FAILED'] as const;
export const translateSettlementBatchStatus = (value: string): string => {
  switch (value) {
    case 'PENDING':
      return 'Chờ chuyển tiền';
    case 'PARTIAL':
      return 'Chuyển tiền một phần';
    case 'COMPLETED':
      return 'Đã chuyển tiền';
    case 'FAILED':
      return 'Chuyển tiền thất bại';
    default:
      return value;
  }
};

export const SETTLEMENT_BATCH_TYPES = [
  'BALANCED',
  'SYSTEM_PAYS',
  'SHOP_PAYS',
] as const;
export const translateSettlementBatchType = (value: string): string => {
  switch (value) {
    case 'BALANCED':
      return 'Hòa (không nợ)';
    case 'SYSTEM_PAYS':
      return 'Hệ thống trả shop';
    case 'SHOP_PAYS':
      return 'Shop trả hệ thống';
    default:
      return value;
  }
};

export const SETTLEMENT_BATCH_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
  'BALANCE_HIGH',
  'BALANCE_LOW'
] as const;
export const translateSettlementFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';
    case 'BALANCE_HIGH': return 'Số tiền cao nhất';
    case 'BALANCE_LOW': return 'Số tiền thấp nhất';
    default: return value;
  }
};

export const SETTLEMENT_BATCH_ORDER_STATUS = [
  'DELIVERED',
  'RETURNED',
] as const;

export const SETTLEMENT_BATCH_ORDER_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
  'COD_HIGH',
  'COD_LOW',
  'FEE_HIGH',
  'FEE_LOW',
] as const;