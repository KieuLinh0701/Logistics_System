export const translateRoleName = (value: string): string => {
  switch (value) {
    case 'Driver': return 'Tài xế';
    case 'Shipper': return 'Nhân viên giao hàng';
    case 'Admin': return 'Quản trị viên';
    case 'Manager': return 'Quản lý';
    case 'User': return 'Cửa hàng';
    default: return value;
  }
};

export const OFFICE_MANAGER_ADDABLE_ROLES = ['Driver', 'Shipper'] as const;

