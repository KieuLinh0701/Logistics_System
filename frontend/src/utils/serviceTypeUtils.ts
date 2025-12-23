export const translateServiceTypeStatus = (value: string): string => {
  switch (value) {
    case 'ACTIVE': return 'Đang hoạt động';       
    case 'INACTIVE': return 'Ngừng hoạt động';          
    default: return value;
  }
};