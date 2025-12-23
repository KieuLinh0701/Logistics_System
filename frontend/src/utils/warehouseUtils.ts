export const WAREHOUSE_FILTER_SORT = ['NEWEST', 'OLDEST', 'WEIGHT_HIGH', 'WEIGHT_LOW'] as const;

export const translateWarehouseFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';

    case 'WEIGHT_HIGH': return 'Khối lượng cao nhất';
    case 'WEIGHT_LOW': return 'Khối lượng thấp nhất';

    default: return value;
  }
};