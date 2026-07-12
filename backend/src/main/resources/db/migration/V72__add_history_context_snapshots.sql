
ALTER TABLE order_histories
    ADD COLUMN pickup_type_snapshot VARCHAR(30) NULL,
    ADD COLUMN stop_type_snapshot VARCHAR(30) NULL;