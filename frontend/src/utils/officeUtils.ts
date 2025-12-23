export const OFFICE_STATUSES = ['ACTIVE', 'INACTIVE', 'MAINTENANCE'] as const;

export const translateOfficeType = (value: string): string => {
  switch (value) {
    case 'HEAD_OFFICE': return 'Trụ sở chính';       
    case 'POST_OFFICE': return 'Bưu cục';          
    default: return value;
  }
};

export const translateOfficeStatus = (value: string): string => {
  switch (value) {
    case 'ACTIVE':
      return 'Đang hoạt động';
    case 'INACTIVE':
      return 'Ngừng hoạt động';
    case 'MAINTENANCE':
      return 'Đang bảo trì';
    default:
      return value;
  }
};