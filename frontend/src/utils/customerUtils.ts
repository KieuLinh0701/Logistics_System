export const CUSTOMER_FILTER_SORT = [
    'NEWEST',
    'OLDEST',

    'TOTAL_ORDERS_HIGH',
    'TOTAL_ORDERS_LOW',

    'RETURN_RATE_HIGH',
    'RETURN_RATE_LOW',

    'SUCCESS_RATE_HIGH',
    'SUCCESS_RATE_LOW',
] as const;

export const translateCustomerFilterSort = (value: string): string => {
    switch (value) {
        case 'NEWEST':
            return 'Mới nhất';
        case 'OLDEST':
            return 'Cũ nhất';

        case 'TOTAL_ORDERS_HIGH':
            return 'Số đơn nhiều nhất';
        case 'TOTAL_ORDERS_LOW':
            return 'Số đơn ít nhất';

        case 'RETURN_RATE_HIGH':
            return 'Tỉ lệ hoàn hàng cao nhất';
        case 'RETURN_RATE_LOW':
            return 'Tỉ lệ hoàn hàng thấp nhất';

        case 'SUCCESS_RATE_HIGH':
            return 'Tỉ lệ hoàn thành cao nhất';
        case 'SUCCESS_RATE_LOW':
            return 'Tỉ lệ hoàn thành thấp nhất';

        default:
            return value;
    }
};