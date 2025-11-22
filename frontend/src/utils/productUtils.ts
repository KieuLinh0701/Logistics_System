export const PRODUCT_TYPES = ['FRESH', 'LETTER', 'GOODS'] as const;

export const PRODUCT_STATUS = ['ACTIVE', 'INACTIVE'] as const;

export const PRODUCT_FILTER_STOCK = ['ALL', 'INSTOCK', 'OUTOFSTOCK', 'LOWSTOCK'] as const;

export const PRODUCT_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
  'BESTSELLING',
  'LEASTSELLING',
  'HIGHESTPRICE',
  'LOWESTPRICE',
  'HIGHESTSTOCK',
  'LOWESTSTOCK',
] as const;

export const translateProductType = (value: string): string => {
  switch (value) {
    case 'FRESH': return 'Tươi sống';
    case 'LETTER': return 'Thư từ';
    case 'GOODS': return 'Hàng hóa';
    default: return value;
  }
};

export const translateProductStatus = (value: string): string => {
  switch (value) {
    case 'ACTIVE': return 'Đang bán';
    case 'INACTIVE': return 'Ngừng bán';
    default: return value;
  }
};

export const translateProductFilterStock = (value: string): string => {
  switch (value) {
    case 'ALL': return 'Tất cả tồn kho';
    case 'INSTOCK': return 'Còn hàng';
    case 'OUTOFSTOCK': return 'Hết hàng';
    case 'LOWSTOCK': return 'Sắp hết hàng';
    default: return value;
  }
};

export const translateProductFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';

    case 'BESTSELLING': return 'Bán chạy nhất';
    case 'LEASTSELLING': return 'Bán ít nhất';

    case 'HIGHESTPRICE': return 'Giá cao nhất';
    case 'LOWESTPRICE': return 'Giá thấp nhất';

    case 'HIGHESTSTOCK': return 'Tồn kho nhiều nhất';
    case 'LOWESTSTOCK': return 'Tồn kho ít nhất';

    default: return value;
  }
};