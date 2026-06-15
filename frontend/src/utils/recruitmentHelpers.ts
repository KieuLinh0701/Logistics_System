export const shiftLabel = (shift?: string) => {
  if (!shift) return 'Đang cập nhật';
  switch (shift) {
    case 'MORNING':
      return 'Ca sáng';
    case 'AFTERNOON':
      return 'Ca chiều';
    case 'EVENING':
      return 'Ca tối';
    case 'FULL_DAY':
      return 'Cả ngày';
    default:
      return 'Đang cập nhật';
  }
};

export const shiftOptions = [
  { label: 'Ca sáng', value: 'MORNING' },
  { label: 'Ca chiều', value: 'AFTERNOON' },
  { label: 'Ca tối', value: 'EVENING' },
  { label: 'Cả ngày', value: 'FULL_DAY' },
];
