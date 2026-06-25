CREATE TABLE promotion_service_types
(
    promotion_id    INTEGER NOT NULL,
    service_type_id INTEGER NOT NULL,
    PRIMARY KEY (promotion_id, service_type_id),
    CONSTRAINT fk_prom_serv_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id),
    CONSTRAINT fk_prom_serv_type FOREIGN KEY (service_type_id) REFERENCES service_types (id)
) ENGINE=InnoDB;

CREATE INDEX idx_promotion_service_types_service_type_id ON promotion_service_types (service_type_id);
CREATE INDEX idx_promotion_service_types_promotion_id ON promotion_service_types (promotion_id);
