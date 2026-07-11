ALTER TABLE vehicles
    ADD COLUMN current_office_id INT NULL,
ADD CONSTRAINT fk_vehicle_current_office
    FOREIGN KEY (current_office_id) REFERENCES offices(id);

UPDATE vehicles SET current_office_id = office_id;