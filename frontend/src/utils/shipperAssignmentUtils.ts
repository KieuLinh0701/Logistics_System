export const SHIPPER_ASSIGNMENT_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
] as const;
export const translateShipperAssignmentFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';
    default:
      return value;
  }
};