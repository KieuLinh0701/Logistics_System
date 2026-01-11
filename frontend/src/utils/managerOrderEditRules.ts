import type { OrderCreatorType, OrderStatus } from "./orderUtils";

interface EditableRule {
  editableStatuses?: readonly OrderStatus[];
  nonEditableStatuses?: readonly OrderStatus[];
}

export const MANAGER_FINAL_STATUSES = ['DELIVERED', 'RETURNED', 'CANCELLED', 'FAILED_DELIVERY'] as const;

// Điều kiện sửa order của manager cho đơn do user tạo
// Chỉ cho phép sửa weight khi gian lận
export const MANAGER_EDIT_USER_ORDER_FIELD_RULES: Record<string, EditableRule> = {
  // Người gửi (KHÓA)
  senderName: { editableStatuses: [] },
  senderPhoneNumber: { editableStatuses: [] },
  senderCityCode: { editableStatuses: [] },
  senderWardCode: { editableStatuses: [] },
  senderDetailAddress: { editableStatuses: [] },

  // Người nhận (KHÓA)
  recipientName: { editableStatuses: [] },
  recipientPhoneNumber: { editableStatuses: [] },
  recipientCityCode: { editableStatuses: [] },
  recipientWardCode: { editableStatuses: [] },
  recipientDetailAddress: { editableStatuses: [] },

  // Thông tin đơn hàng
  weight: { editableStatuses: ['PICKED_UP', 'AT_ORIGIN_OFFICE'] },
  serviceType: { editableStatuses: [] },
  cod: { editableStatuses: [] },
  orderValue: { editableStatuses: [] },
  products: { editableStatuses: [] },

  // Lấy hàng (KHÓA)
  pickupType: { editableStatuses: [] },
  fromOffice: { editableStatuses: [] },

  // Người trả phí
  payer: { editableStatuses: [] },

  // Ghi chú
  notes: { editableStatuses: [] },

  promotion: { editableStatuses: [] },
};

// Điều kiện để manager sửa các đơn hàng tạo tại bưu cục
export const MANAGER_EDIT_OFFICE_ORDER_FIELD_RULES: Record<string, EditableRule> = {
  // Người gửi
  senderName: {
    editableStatuses: ['AT_ORIGIN_OFFICE'],
  },
  senderPhoneNumber: {
    editableStatuses: ['AT_ORIGIN_OFFICE'],
  },
  senderCityCode: {
    editableStatuses: [],
  },
  senderWardCode: {
    editableStatuses: ['AT_ORIGIN_OFFICE'],
  },
  senderDetailAddress: {
    editableStatuses: ['AT_ORIGIN_OFFICE'],
  },
  // Người nhận 
  recipientName: {
    nonEditableStatuses: MANAGER_FINAL_STATUSES,
  },
  recipientPhoneNumber: {
    nonEditableStatuses: MANAGER_FINAL_STATUSES,
  },
  recipientCityCode: {
    editableStatuses: [],
  },
  recipientWardCode: {
    editableStatuses: ['PENDING', 'CONFIRMED', 'READY_FOR_PICKUP', 'PICKING_UP', 'PICKED_UP', 'AT_ORIGIN_OFFICE'],
  },
  recipientDetailAddress: {
    nonEditableStatuses: MANAGER_FINAL_STATUSES,
  },
  // Thông tin đơn hàng
  weight: {
    editableStatuses: [],
  },
  serviceType: {
    editableStatuses: [],
  },
  cod: {
    editableStatuses: [],
  },
  orderValue: {
    editableStatuses: [],
  },
  products: {
    editableStatuses: [],
  },
  // Hình thức lấy hàng
  pickupType: {
    editableStatuses: [],
  },
  fromOffice: {
    editableStatuses: [],
  },
  // Người trả phí
  payer: {
    editableStatuses: [],
  },
  // Ghi chú
  notes: {
    nonEditableStatuses: ['DELIVERED', 'CANCELLED', 'RETURNED'],
  },
  // Khuyến mãi
  promotion: {
    editableStatuses: [],
  }
};

function canEditByRules(
  rules: Record<string, EditableRule>,
  field: string,
  status: OrderStatus
): boolean {
  const rule = rules[field];
  if (!rule) return false;

  if (rule.nonEditableStatuses) {
    return !rule.nonEditableStatuses.includes(status);
  }
  if (rule.editableStatuses) {
    return rule.editableStatuses.includes(status);
  }

  return false;
}

export const canManagerEditOrderField = (
  field:
    | keyof typeof MANAGER_EDIT_USER_ORDER_FIELD_RULES
    | keyof typeof MANAGER_EDIT_OFFICE_ORDER_FIELD_RULES,
  status: OrderStatus,
  creatorType: OrderCreatorType
): boolean => {
  if (creatorType === 'USER') {
    return canEditByRules(
      MANAGER_EDIT_USER_ORDER_FIELD_RULES,
      field,
      status
    );
  }

  return canEditByRules(
    MANAGER_EDIT_OFFICE_ORDER_FIELD_RULES,
    field,
    status
  );
};
