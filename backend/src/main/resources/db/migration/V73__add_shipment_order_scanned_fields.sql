
ALTER TABLE shipment_orders
    ADD COLUMN scanned_at DATETIME NULL,
    ADD COLUMN scanned_by_employee_id INT NULL;

ALTER TABLE shipment_orders
    ADD CONSTRAINT fk_shipment_order_scanned_by
        FOREIGN KEY (scanned_by_employee_id) REFERENCES employees(id);

CREATE INDEX idx_shipment_orders_shipment_scanned_at
    ON shipment_orders (shipment_id, scanned_at);