export const translatePromotionStatus = (value: string): string => {
  switch (value) {
    case 'ACTIVE': return 'Đang hoạt động';
    case 'INACTIVE': return 'Không hoạt động';
    case 'EXPIRED': return 'Đã hết hạn';
    default: return value;
  }
};

export const translatePromotionDiscountType = (value: string): string => {
  switch (value) {
    case 'PERCENTAGE': return 'Giảm theo %';
    case 'FIXED': return 'Giảm theo số tiền';
    default: return value;
  }
};

export  const getDiscountText = (type: string, value: number): string => {
    if (type === "PERCENTAGE") {
      return `Giảm ${value}%`;
    } else {
      return `Giảm ${value.toLocaleString()} VNĐ`;
    }
  };