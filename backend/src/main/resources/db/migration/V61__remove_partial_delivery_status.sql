
UPDATE orders SET status = 'DELIVERED' WHERE status = 'PARTIAL_DELIVERY';

UPDATE orders SET status = 'RETURNED' WHERE status = 'PARTIAL_RETURN';

UPDATE order_histories SET action = 'DELIVERED' WHERE action = 'PARTIAL_DELIVERY';

UPDATE order_histories SET action = 'RETURNED' WHERE action = 'PARTIAL_RETURN';
