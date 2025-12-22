import type { OrderStatus } from "./orderUtils";

// Điều kiện sửa order của user 
interface EditableRule {
  editableStatuses?: readonly OrderStatus[];
  nonEditableStatuses?: readonly OrderStatus[];
}

export const USER_FINAL_STATUSES = ['DELIVERING', 'DELIVERED', 'RETURNED', 'CANCELLED', 'FAILED_DELIVERY'] as const;

export const USER_ORDER_FIELD_EDIT_RULES: Record<string, EditableRule> = {
  // Người gửi
  senderAddress: {
    editableStatuses: ['DRAFT'],
  },
  // Người nhận 
  recipientName: {
    nonEditableStatuses: USER_FINAL_STATUSES,
  },
  recipientPhoneNumber: {
    nonEditableStatuses: USER_FINAL_STATUSES,
  },
  recipientCityCode: {
    editableStatuses: ['DRAFT'],
  },
  recipientWardCode: {
    editableStatuses: ['DRAFT', 'PENDING', 'CONFIRMED', 'READY_FOR_PICKUP', 'PICKING_UP', 'PICKED_UP', 'AT_ORIGIN_OFFICE'],
  },
  recipientDetailAddress: {
    nonEditableStatuses: USER_FINAL_STATUSES,
  },
  // Thông tin đơn hàng
  weight: {
    editableStatuses: ['DRAFT'],
  },
  serviceType: {
    editableStatuses: ['DRAFT'],
  },
  cod: {
    editableStatuses: ['DRAFT'],
  },
  orderValue: {
    editableStatuses: ['DRAFT'],
  },
  products: {
    editableStatuses: ['DRAFT'],
  },
  // Hình thức lấy hàng
  pickupType: {
    editableStatuses: ['DRAFT', 'PENDING'],
  },
  fromOffice: {
    editableStatuses: ['DRAFT', 'PENDING'],
  },
  // Người trả phí
  payer: {
    editableStatuses: ['DRAFT'],
  },
  // Ghi chú
  notes: {
    nonEditableStatuses: ['DELIVERED', 'CANCELLED', 'RETURNED'],
  },
  // Khuyến mãi
  promotion: {
    editableStatuses: ['DRAFT'],
  }
};

export const canEditUserOrderField = (
  field: keyof typeof USER_ORDER_FIELD_EDIT_RULES,
  status: OrderStatus
): boolean => {
  const rule = USER_ORDER_FIELD_EDIT_RULES[field];
  if (!rule) return false;
  if (rule.nonEditableStatuses) {
    return !rule.nonEditableStatuses.includes(status);
  }
  if (rule.editableStatuses) {
    return rule.editableStatuses.includes(status);
  }
  return false;
};